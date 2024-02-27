#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# =============================================================================
# osm2db.py
#
# Author: Daniel Krajzewicz
# Date:   01.04.2016
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2016-2024 Institute of Transport Research,
#                         German Aerospace Center
# All rights reserved.
# =============================================================================
"""Imports an OSM-file into the database.

Call with
   osm2db <HOST>,<DB>,<SCHEMA>,<PREFIX>,<USER>,<PASSWD> <FILE>"""
# =============================================================================

# --- imported modules --------------------------------------------------------
import os, string, sys
import datetime
from xml.sax import saxutils, make_parser, handler
import psycopg2
import osm


# --- meta --------------------------------------------------------------------
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright (c) 2016-2024 Institute of Transport Research, German Aerospace Center"
__credits__    = [ "Daniel Krajzewicz" ]
__license__    = "EPL2.0"
__version__    = "0.8"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Development"


# --- class definitions -------------------------------------------------------
# --- OSMReader
class OSMReader(handler.ContentHandler):
    """ @class OSMReader
    @brief A reader that parses an OSM XML file and writes it to a database
    """

    def __init__(self, schema, prefix, conn, cursor):
        """ @brief Initialises the reader
        @param self The class instance
        @param schema The database schema to write the data to
        @param prefix The database prefix to write the data to
        @param conn The connection to the database
        @param cursor The cursor used to write to the database
        """
        self.schema = schema
        self.prefix = prefix
        self.fname = schema+"."+prefix
        self.conn = conn
        self.cursor = cursor
        self.last = None
        self.elements = []
        self.nodes = []
        self.ways = []
        self.relations = []
        self.stats = {"nodes":0, "ways":0, "node_attrs":0, "way_attrs":0, "rels":0, "relMembers":0, "rel_attrs":0}
  
  
    def startElement(self, name, attrs):
        """ @brief Called when an element begins
        @param self The class instance
        @param name The name of the element
        @param attrs The attributes of the element
        """
        self.elements.append(name)
        if name=="osm" or name=="bounds":
            pass
        elif name=="node":
            id = int(attrs["id"])
            n = osm.OSMNode(id, [float(attrs["lon"]), float(attrs["lat"])])
            self.nodes.append(n)
            self.last = n
        elif name=="way":
            id = int(attrs["id"])
            e = osm.OSMWay(id)
            self.ways.append(e)
            self.last = e
        elif name=="nd":
            n = int(attrs["ref"])
            self.last.addNodeID(n)
        elif name=="relation":
            id = int(attrs["id"])
            r = osm.OSMRelation(id)
            self.relations.append(r)
            self.last = r
        elif name=="member":
            n = int(attrs["ref"])
            self.last.addMember(n, attrs["type"], attrs["role"])
        elif name=='tag' and self.last!=None:
            k = attrs['k']
            v = attrs['v']
            self.last.addTag(k, v)
      
      
    def endElement(self, name):
        """ @brief Called when an element ends
        @param self The class instance
        @param name The name of the element
        """
        l = self.elements[-1]
        if l=="node" or l=="way" or l=="relation":
            self.last = None
            self.checkCommit(False)
        self.elements = self.elements[:-1]


    def checkCommit(self, force):
        """ @brief Commits stored entries if a sufficient number has been reached
        @param self The class instance
        @param force When set, all stored items are committed in any case
        """
        if not force and (len(self.nodes)+len(self.ways)+len(self.relations))<10000:
            return
        ntagsNum = 0
        wtagsNum = 0
        rtagsNum = 0
        relMembers = 0
        # nodes
        nodeEntries = []
        nodeTagEntries = []
        for n in self.nodes:
            nodeEntries.append((n.id, n.pos[0], n.pos[1]))
            for k in n.tags:
                nodeTagEntries.append((n.id, k, n.tags[k]))
                ntagsNum = ntagsNum + 1
        if len(nodeEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s, ST_GeomFromText('POINT(%s %s)', 4326))", i).decode('utf-8') for i in nodeEntries)
            self.cursor.execute("INSERT INTO %s_node(id, pos) VALUES " % (self.fname) + (args))
        if len(nodeTagEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in nodeTagEntries)
            self.cursor.execute("INSERT INTO %s_ntag(id, k, v) VALUES " % (self.fname) + (args))
        # ways
        wayEntries = []
        wayTagEntries = []
        for w in self.ways:
            wayEntries.append((w.id, w.refs))
            for k in w.tags:
                wayTagEntries.append((w.id, k, w.tags[k]))
                wtagsNum = wtagsNum + 1
        if len(wayEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s, %s)", i).decode('utf-8') for i in wayEntries)
            self.cursor.execute("INSERT INTO %s_way(id, refs) VALUES " % (self.fname) + (args))
        if len(wayTagEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in wayTagEntries)
            self.cursor.execute("INSERT INTO %s_wtag(id, k, v) VALUES " % (self.fname) + (args))
        # relations
        relEntries = []
        relTagEntries = []
        relMemberEntries = []
        for r in self.relations:
            relEntries.append((r.id,))
            for k in r.tags:
                relTagEntries.append((r.id, k, r.tags[k]))
                rtagsNum = rtagsNum + 1
            for idx,m in enumerate(r.members):
                relMemberEntries.append((r.id, m[0], m[1], m[2], idx))
                relMembers = relMembers + 1
        if len(relEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s)", i).decode('utf-8') for i in relEntries)
            self.cursor.execute("INSERT INTO %s_rel(id) VALUES " % (self.fname) + (args))
        if len(relTagEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in relTagEntries)
            self.cursor.execute("INSERT INTO %s_rtag(id, k, v) VALUES " % (self.fname) + (args))
        if len(relMemberEntries)>0:
            args = ','.join(self.cursor.mogrify("(%s, %s, %s, %s, %s)", i).decode('utf-8') for i in relMemberEntries)
            self.cursor.execute("INSERT INTO %s_member(rid, elemID, type, role, idx) VALUES " % (self.fname) + (args))
        #
        print (" %s nodes (%s keys), %s ways (%s keys), and %s relations (%s keys, %s members)" % (len(self.nodes), ntagsNum, len(self.ways), wtagsNum, len(self.relations), rtagsNum, relMembers))
        self.stats["nodes"] = self.stats["nodes"] + len(self.nodes)
        self.stats["ways"] = self.stats["ways"] + len(self.ways)
        self.stats["node_attrs"] = self.stats["node_attrs"] + ntagsNum
        self.stats["way_attrs"] = self.stats["way_attrs"] + wtagsNum
        self.stats["rels"] = self.stats["rels"] + len(self.relations)
        self.stats["relMembers"] = self.stats["relMembers"] + relMembers
        self.stats["rel_attrs"] = self.stats["rel_attrs"] + rtagsNum
        if len(self.nodes)+len(self.ways)+len(self.relations)>0:
            self.conn.commit() 
        self.nodes = []
        self.ways = []
        self.relations = []
    

# --- function definitions ----------------------------------------------------
# --- main 
def main(argv):
    """ @brief Main method
    @param argv The program argument
    """
    # connect to the database
    (host, db, schema, prefix, user, password) = sys.argv[1].split(",")
    t1 = datetime.datetime.now()
    print ("Connecting to the db...")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    print ("Building tables for %s..." % prefix)
    
    # check whether the tables already exist
    # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
    cursor.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s_node');" % (schema, prefix))
    conn.commit()   
    ret = cursor.fetchall()
    if ret[0][0]:
        # delete if already existing
        # @TODO: ask user whether really to delete
        cursor.execute("DROP TABLE %s.%s_member;" % (schema, prefix))
        cursor.execute("DROP TABLE %s.%s_rtag;" % (schema, prefix))
        cursor.execute("DROP TABLE %s.%s_wtag;" % (schema, prefix))
        cursor.execute("DROP TABLE %s.%s_ntag;" % (schema, prefix))
        cursor.execute("DROP TABLE %s.%s_rel;" % (schema, prefix))
        cursor.execute("DROP TABLE %s.%s_way;" % (schema, prefix))
        cursor.execute("DROP TABLE %s.%s_node;" % (schema, prefix))
        conn.commit()
    
    # build the tables
    cursor.execute("CREATE TABLE %s.%s_node (id bigint PRIMARY KEY);" % (schema, prefix))
    cursor.execute("CREATE TABLE %s.%s_way (id bigint PRIMARY KEY, refs bigint[]);" % (schema, prefix))
    cursor.execute("CREATE TABLE %s.%s_rel (id bigint PRIMARY KEY);" % (schema, prefix))
    cursor.execute("SELECT AddGeometryColumn('%s', '%s_node', 'pos', 4326, 'POINT', 2, true);" % (schema, prefix))
    # --- tags
    cursor.execute("CREATE TABLE %s.%s_ntag ( id bigint REFERENCES %s.%s_node (id), k text, v text );" % (schema, prefix, schema, prefix))
    cursor.execute("CREATE INDEX ON %s.%s_ntag (id);" % (schema, prefix))
    cursor.execute("CREATE TABLE %s.%s_wtag ( id bigint REFERENCES %s.%s_way (id), k text, v text );" % (schema, prefix, schema, prefix))
    cursor.execute("CREATE INDEX ON %s.%s_wtag (id);" % (schema, prefix))
    cursor.execute("CREATE TABLE %s.%s_rtag ( id bigint REFERENCES %s.%s_rel (id), k text, v text );" % (schema, prefix, schema, prefix))
    cursor.execute("CREATE INDEX ON %s.%s_rtag (id);" % (schema, prefix))
    cursor.execute("CREATE TABLE %s.%s_member ( rid bigint REFERENCES %s.%s_rel (id), elemID bigint, type text, role text, idx integer );" % (schema, prefix, schema, prefix))
    cursor.execute("CREATE INDEX ON %s.%s_member (rid);" % (schema, prefix))
    cursor.execute("CREATE INDEX ON %s.%s_member (elemID);" % (schema, prefix))
    conn.commit()

    # parsing the document and adding contents to the db
    print ("Parsing '%s'..." % sys.argv[2])
    parser = make_parser()
    r = OSMReader(schema, prefix, conn, cursor)
    parser.setContentHandler(r)
    parser.parse(sys.argv[2])
    r.checkCommit(True)
    cursor.close()
    conn.close()

    # report
    t2 = datetime.datetime.now()
    print ("Finished.")
    print ("Summary:")
    print (" %s nodes with %s attributes" % (r.stats["nodes"], r.stats["node_attrs"]))
    print (" %s ways with %s attributes" % (r.stats["ways"], r.stats["way_attrs"]))
    print (" %s relations with %s members and %s attributes" % (r.stats["rels"], r.stats["relMembers"], r.stats["rel_attrs"]))
    dt = t2-t1
    print ("In %s" % dt)


# -- main check
if __name__ == '__main__':
    main(sys.argv)
