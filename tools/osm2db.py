#!/usr/bin/env python
# =========================================================
# osm2db.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Imports an OSM-file into the database
# Call with
#  osm2db <HOST>;<DB>;<SCHEMA>.<PREFIX>;<USER>;<PASSWD> <FILE>
# =========================================================
import os, string, sys
import datetime
from xml.sax import saxutils, make_parser, handler
import psycopg2



class OSMNode:
  def __init__(self, id, lat, lon):
    self.id = id
    self.refNum = 0
    self.lat = lat
    self.lon = lon
    self.tags = {}
  def addTag(self, k, v):
    self.tags[k] = v
    
class OSMWay:
  def __init__(self, id):
    self.id = id
    self.tags = {}
    self.refs = []
  def addNodeID(self, nID):
    self.refs.append(nID)
  def addTag(self, k, v):
    self.tags[k] = v
    
class OSMRel:
  def __init__(self, id):
    self.id = id
    self.tags = {}
    self.members = []
  def addMember(self, mID, mType, mRole):
    self.members.append([mID, mType, mRole])
  def addTag(self, k, v):
    self.tags[k] = v
    
    


class OSMReader(handler.ContentHandler):
  def __init__(self, schema, prefix, conn, cursor):
    self.schema = schema
    self.prefix = prefix
    self.fname = schema+"."+prefix
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
      id = long(attrs["id"])
      n = OSMNode(id, float(attrs["lat"]), float(attrs["lon"]))
      self.nodes[id] = n
      self.last = n
    elif name=="way":
      id = long(attrs["id"])
      e = OSMWay(id)
      self.ways[id] = e
      self.last = e
    elif name=="nd":
      n = long(attrs["ref"])
      self.last.addNodeID(n)
    elif name=="relation":
      id = long(attrs["id"])
      r = OSMRel(id)
      self.relations[id] = r
      self.last = r
    elif name=="member":
      n = long(attrs["ref"])
      self.last.addMember(n, attrs["type"], attrs["role"])
    elif name=='tag' and self.last!=None:
      k = attrs['k']
      v = attrs['v']
      self.last.addTag(k, v)
      
  def endElement(self, name):
    l = self.elements[-1]
    if l=="node" or l=="way" or l=="relation":
      self.last = None
      self.checkCommit(False)
    self.elements = self.elements[:-1]

  def checkCommit(self, force):
    if not force and (len(self.nodes)+len(self.ways)+len(self.relations))<10000:
      return
    ntagsNum = 0
    wtagsNum = 0
    rtagsNum = 0
    relMembers = 0
    for n in self.nodes:
      node = self.nodes[n]
      self.cursor.execute("INSERT INTO "+self.fname+"_node(id, pos) VALUES (%s, ST_GeomFromText('POINT(%s %s)', 4326))", (node.id, node.lon, node.lat))
      for k in node.tags:
        self.cursor.execute("INSERT INTO "+self.fname+"_ntag(id, k, v) VALUES (%s, %s, %s)", (node.id, k, node.tags[k]))
        ntagsNum = ntagsNum + 1
    for w in self.ways:
      way = self.ways[w]
      self.cursor.execute("INSERT INTO "+self.fname+"_way(id, refs) VALUES (%s, %s);", (way.id, way.refs))
      for k in way.tags:
        self.cursor.execute("INSERT INTO "+self.fname+"_wtag(id, k, v) VALUES (%s, %s, %s);", (way.id, k, way.tags[k]))
        wtagsNum = wtagsNum + 1
    for r in self.relations:
      rel = self.relations[r]
      self.cursor.execute("INSERT INTO "+self.fname+"_rel(id) VALUES (%s);", (rel.id,))
      for k in rel.tags:
        self.cursor.execute("INSERT INTO "+self.fname+"_rtag(id, k, v) VALUES (%s, %s, %s);", (rel.id, k, rel.tags[k]))
        rtagsNum = rtagsNum + 1
      for idx,m in enumerate(rel.members):
        self.cursor.execute("INSERT INTO "+self.fname+"_member(rid, elemID, type, role, idx) VALUES (%s, %s, %s, %s, %s);", (rel.id, m[0], m[1], m[2], idx))
        relMembers = relMembers + 1
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
    self.nodes.clear() 
    self.ways.clear()
    self.relations.clear()
    

(host, db, tableFull, user, password) = sys.argv[1].split(";")
(schema, prefix) = tableFull.split(".")
t1 = datetime.datetime.now()
print ("Connecting to the db...")
conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor = conn.cursor()
print ("Building tables for %s..." % prefix)
# http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s_node');""" % (schema, prefix))
conn.commit()   
ret = cursor.fetchall()
if ret[0][0]:
  # TODO: ask user whether really to delete
  cursor.execute("""DROP TABLE %s.%s_member;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_rtag;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_wtag;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_ntag;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_rel""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_way""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_node;""" % (schema, prefix))
  conn.commit()
    
    
cursor.execute("""CREATE TABLE %s.%s_node (
    id bigint PRIMARY KEY
);""" % (schema, prefix))
cursor.execute("""SELECT AddGeometryColumn('%s', '%s_node', 'pos', 4326, 'POINT', 2);""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_way (
    id bigint PRIMARY KEY, 
    refs bigint[] 
);""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_rel (
    id bigint PRIMARY KEY
);""" % (schema, prefix))

# tags
cursor.execute("""CREATE TABLE %s.%s_ntag (
    id bigint REFERENCES %s.%s_node (id),
    k text,        
    v text        
);""" % (schema, prefix, schema, prefix))
cursor.execute("""CREATE INDEX ON %s.%s_ntag (id);""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_wtag (
    id bigint REFERENCES %s.%s_way (id),
    k text,        
    v text        
);""" % (schema, prefix, schema, prefix))
cursor.execute("""CREATE INDEX ON %s.%s_wtag (id);""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_rtag (
    id bigint REFERENCES %s.%s_rel (id),
    k text,        
    v text        
);""" % (schema, prefix, schema, prefix))
cursor.execute("""CREATE INDEX ON %s.%s_rtag (id);""" % (schema, prefix))

# relation members
cursor.execute("""CREATE TABLE %s.%s_member (
    rid bigint REFERENCES %s.%s_rel (id),
    elemID bigint,
    type text,        
    role text,        
    idx integer        
);""" % (schema, prefix, schema, prefix))
cursor.execute("""CREATE INDEX ON %s.%s_member (rid);""" % (schema, prefix))
cursor.execute("""CREATE INDEX ON %s.%s_member (elemID);""" % (schema, prefix))
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

