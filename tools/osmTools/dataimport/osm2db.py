#!/usr/bin/env python

#import os, string, sys
#import datetime

#from optparse import OptionParser
from xml.sax import saxutils, make_parser, handler
from osmDb import osm_db

import psycopg2



class OSMNode:
    def __init__(self, osm_id, lat, lon):
        self.id = osm_id
        self.refNum = 0
        self.lat = lat
        self.lon = lon
        self.tags = {}
    def addTag(self, k, v):
        self.tags[k] = v
    
class OSMWay:
    def __init__(self, osm_id):
        self.id = osm_id
        self.tags = {}
        self.refs = []
    def addNodeID(self, nID):
        self.refs.append(nID)
    def addTag(self, k, v):
        self.tags[k] = v
    
class OSMRel:
    def __init__(self, osm_id):
        self.id = osm_id
        self.tags = {}
        self.members = []
    def addMember(self, mID, mType, mRole):
        self.members.append([mID, mType, mRole])
    def addTag(self, k, v):
        self.tags[k] = v
    
    


class OSMReader(handler.ContentHandler):
    def __init__(self, prefix, conn, cursor):
        self.prefix = prefix
        self.conn = conn
        self.cursor = cursor
        self.last = None
        self.elements = []
        self.nodes = {}
        self.ways = {}
        self.relations = {}
        self.stats = {"nodes":0, "ways":0, "node_attrs":0, "way_attrs":0, "rels":0, "relMembers":0, "rel_attrs":0}
    
    def startElement(self, name, attrs):
        self.elements.append(name)
        if name=="osm" or name=="bounds":
            pass
        elif name=="node":
            osm_id = long(attrs["id"])
            n = OSMNode(osm_id, float(attrs["lat"]), float(attrs["lon"]))
            self.nodes[osm_id] = n
            self.last = n
        elif name=="way":
            osm_id = long(attrs["id"])
            e = OSMWay(osm_id)
            self.ways[osm_id] = e
            self.last = e
        elif name=="nd":
            n = long(attrs["ref"])
            self.last.addNodeID(n)
        elif name=="relation":
            osm_id = long(attrs["id"])
            r = OSMRel(osm_id)
            self.relations[osm_id] = r
            self.last = r
        elif name=="member":
            n = long(attrs["ref"])
            self.last.addMember(n, attrs["type"], attrs["role"])
        elif name=='tag' and self.last!=None:
            k = attrs['k']
            v = attrs['v']
            self.last.addTag(k, v)
      
    def endElement(self, name, verbose=0):
        l = self.elements[-1]
        if l=="node" or l=="way" or l=="relation":
            self.last = None
            self.checkCommit(False)
        self.elements = self.elements[:-1]

    def checkCommit(self, force, verbose=0):
        if not force and (len(self.nodes)+len(self.ways)+len(self.relations))<1000:
            return
        ntagsNum = 0
        wtagsNum = 0
        rtagsNum = 0
        relMembers = 0
        for n in self.nodes:
            node = self.nodes[n]
            self.cursor.execute("INSERT INTO osm."+self.prefix+"_node(id, pos) VALUES (%s, ST_GeomFromText('POINT(%s %s)', 4326))", (node.id, node.lon, node.lat))
            for k in node.tags:
                self.cursor.execute("INSERT INTO osm."+self.prefix+"_ntag(id, k, v) VALUES (%s, %s, %s)", (node.id, k, node.tags[k]))
                ntagsNum = ntagsNum + 1
        for w in self.ways:
            way = self.ways[w]
            self.cursor.execute("INSERT INTO osm."+self.prefix+"_way(id, refs) VALUES (%s, %s);", (way.id, way.refs))
            for k in way.tags:
                self.cursor.execute("INSERT INTO osm."+self.prefix+"_wtag(id, k, v) VALUES (%s, %s, %s);", (way.id, k, way.tags[k]))
                wtagsNum = wtagsNum + 1
        for r in self.relations:
            rel = self.relations[r]
            self.cursor.execute("INSERT INTO osm."+self.prefix+"_rel(id) VALUES (%s);", (rel.id,))
            for k in rel.tags:
                self.cursor.execute("INSERT INTO osm."+self.prefix+"_rtag(id, k, v) VALUES (%s, %s, %s);", (rel.id, k, rel.tags[k]))
                rtagsNum = rtagsNum + 1
            for idx,m in enumerate(rel.members):
                self.cursor.execute("INSERT INTO osm."+self.prefix+"_member(rid, elemID, type, role, idx) VALUES (%s, %s, %s, %s, %s);", (rel.id, m[0], m[1], m[2], idx))
                relMembers = relMembers + 1
        if verbose>0:
            print " %s nodes (%s keys), %s ways (%s keys), and %s relations (%s keys, %s members)" % (len(self.nodes), ntagsNum, len(self.ways), wtagsNum, len(self.relations), rtagsNum, relMembers)
        self.stats["nodes"] = self.stats["nodes"] + len(self.nodes)
        self.stats["ways"] = self.stats["ways"] + len(self.ways)
        self.stats["node_attrs"] = self.stats["node_attrs"] + ntagsNum
        self.stats["way_attrs"] = self.stats["way_attrs"] + wtagsNum
        self.stats["rels"] = self.stats["rels"] + len(self.relations)
        self.stats["relMembers"] = self.stats["relMembers"] + relMembers
        self.stats["rel_attrs"] = self.stats["rel_attrs"] + rtagsNum
        if len(self.nodes)+len(self.ways)+len(self.relations)>0:
            self.conn.commit() 
        self.nodes.clear() 
        self.ways.clear()
        self.relations.clear()
    

def import_osm_raw(db_name, user, host, psw, osm_file, location, verbose=0):
    #t1 = datetime.datetime.now()
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'"%(db_name, user, host, psw))
    cursor = conn.cursor()
    db = osm_db.OSMDB(location, conn, cursor)

    if verbose>0:
        print "Building tables for %s..." % location
        # base
    #if db.tableExists("osm", "%s_node" % location):
        # TODO: ask user whether really to delete
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_member;""" % location)
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_rtag;""" % location)
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_wtag;""" % location)
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_ntag;""" % location)
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_rel""" % location)
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_way""" % location)
    cursor.execute("""DROP TABLE IF EXISTS osm.%s_node;""" % location)
    conn.commit()
  
  
    cursor.execute("""CREATE TABLE osm.%s_node (
        id bigint PRIMARY KEY
        );""" % location)
    cursor.execute("""SELECT AddGeometryColumn('osm', '%s_node', 'pos', 4326, 'POINT', 2);""" % location)
    cursor.execute("""CREATE TABLE osm.%s_way (
        id bigint PRIMARY KEY, 
        refs bigint[] 
        );""" % location)
    cursor.execute("""CREATE TABLE osm.%s_rel (
        id bigint PRIMARY KEY
        );""" % location)

    # tags
    cursor.execute("""CREATE TABLE osm.%s_ntag (
        id bigint REFERENCES osm.%s_node (id),
        k text,        
        v text        
        );""" % (location, location))
    cursor.execute("""CREATE TABLE osm.%s_wtag (
        id bigint REFERENCES osm.%s_way (id),
        k text,        
        v text);""" % (location, location))
    cursor.execute("""CREATE TABLE osm.%s_rtag (
        id bigint REFERENCES osm.%s_rel (id),
        k text,        
        v text        
        );""" % (location, location))

    # relation members
    cursor.execute("""CREATE TABLE osm.%s_member (
        rid bigint REFERENCES osm.%s_rel (id),
        elemID bigint,
        type text,        
        role text,        
        idx integer            
        );""" % (location, location))
    conn.commit()

    # parsing the document and adding contents to the db
    if verbose>0:
        print "Parsing '%s'..." % osm_file
    parser = make_parser()
    r = OSMReader(location, conn, cursor)
    parser.setContentHandler(r)
    parser.parse(osm_file)
    r.checkCommit(True, verbose)
    cursor.execute("CREATE INDEX ON osm.%s_ntag(id)" % location)
    cursor.execute("CREATE INDEX ON osm.%s_wtag(id)" % location)
    cursor.execute("CREATE INDEX ON osm.%s_rtag(id)" % location)
    cursor.execute("CREATE INDEX ON osm.%s_way(id)" % location)
    cursor.execute("CREATE INDEX ON osm.%s_member(rid)" % location)
    conn.commit()
    cursor.close()
    conn.close()

    # report
    #t2 = datetime.datetime.now()

    print "    Summary:"
    print "    %s nodes with %s attributes" % (r.stats["nodes"], r.stats["node_attrs"])
    print "    %s ways with %s attributes" % (r.stats["ways"], r.stats["way_attrs"])
    print "    %s relations with %s members and %s attributes\n" % (r.stats["rels"], r.stats["relMembers"], r.stats["rel_attrs"])
    #dt = t2-t1
    #print "In %s" % dt



