#!/usr/bin/env python
# =========================================================
# osmdb_getStructures.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 14.08.2019
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Extracts specific structures (not the network) from an OSM-database representation
# Call with
#  osmdb_buildStructures.py <INPUT_TABLE> <DEF_FILE> <OUTPUT_TABLE> 
# where <INPUT_TABLE> is defined as:
#  <HOST>;<DB>;<SCHEMA>.<PREFIX>;<USER>;<PASSWD>  
# and <OUTPUT_TABLE> is defined as:
#  <HOST>;<DB>;<SCHEMA>.<NAME>;<USER>;<PASSWD>  
# =========================================================
import sys, os
import psycopg2
import datetime

from db_config import db_name, db_user, db_password, db_host

script_dir = os.path.dirname( __file__ )
mymodule_dir = os.path.join( script_dir, '..', 'helper' )
sys.path.append( mymodule_dir )
from postgishelper import *

subtype2tag = {
  "node": "ntag",
  "way": "wtag",
  "rel": "rtag"
}

def loadDefinitions(fileName):
  fd = open(fileName)
  defs = { "node": [], "way": [], "rel": [] }
  subtype = ""
  for l in fd:
    if len(l.strip())==0:
      continue
    if l[0]=='#':
      continue
    if l[0]=='[':
      subtype = l[1:l.find(']')].strip()
      continue
    defs[subtype].append(l.strip())
  #print (defs)
  return defs

def getObjects(conn, cursor, schema, prefix, subtype, k, v):
  ret = set()
  if v=='*':
    cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s'" % (schema, prefix, subtype2tag[subtype], k))
  else:
    cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s' AND v='%s'" % (schema, prefix, subtype2tag[subtype], k, v))
  conn.commit()
  for r in cursor.fetchall():
    ret.add(r[0])
  return ret


def fetchNodeGeom(conn, cursor, schema, prefix, o, geoms):
  cursor.execute("SELECT id,ST_AsText(pos) FROM %s.%s_node WHERE id=%s" % (schema, prefix, o))
  conn.commit()
  r = cursor.fetchone()
  geoms.append(parsePOINT2XY(r[1]))
  
def fetchWayGeom(conn, cursor, schema, prefix, o, geoms):
  cursor.execute("SELECT id,refs FROM %s.%s_way WHERE id=%s" % (schema, prefix, o))
  conn.commit()
  r = cursor.fetchone()
  ns = r[1]
  for n in ns:
    cursor.execute("SELECT id,ST_AsText(pos) FROM %s.%s_node WHERE id=%s" % (schema, prefix, n))
    conn.commit()
    r2 = cursor.fetchone()
    geoms.append(parsePOINT2XY(r2[1]))
  
def fetchRelGeom(conn, cursor, schema, prefix, o, geoms, seen):
  cursor.execute("SELECT elemid,type FROM %s.%s_member WHERE rid=%s" % (schema, prefix, o))
  conn.commit()
  items = []
  for r in cursor.fetchall():
    iid = int(r[0])
    if r[1]=="rel" or r[1]=="relation":
      if iid==int(o) or iid in seen:
        print ("Self-referencing relation %s" % o)
        continue
      seen.add(iid)
    items.append((iid, r[1]))
  if len(items)==0:
    print ("Empty relation %s" % o)
  for i in items:
    if i[1]=="node":
      fetchNodeGeom(conn, cursor, schema, prefix, i[0], geoms)
    elif i[1]=="way":
      fetchWayGeom(conn, cursor, schema, prefix, i[0], geoms)
    elif (i[1]=="rel" or  i[1]=="relation"):
      fetchRelGeom(conn, cursor, schema, prefix, i[0], geoms, seen)
    else:
      print ("Unknown type '%s' for relation member (id=%s, rel=%s)" % (i[1], i[0], o))
  

def getAndConvertGeometry(conn, cursor, schema, prefix, objects, subtype, destGeom):
  ret = []
  if subtype=="node":
    for o in objects:
      geoms = []
      fetchNodeGeom(conn, cursor, schema, prefix, o, geoms)
      if len(geoms)!=0: ret.append([o, "node", geoms])
  elif subtype=="way":
    for o in objects:
      geoms = []
      fetchWayGeom(conn, cursor, schema, prefix, o, geoms)
      if len(geoms)!=0: ret.append([o, "way", geoms])
  elif subtype=="rel" or subtype=="relation":
    for o in objects:
      seen = set()
      geoms = []
      fetchRelGeom(conn, cursor, schema, prefix, o, geoms, seen)
      if len(geoms)!=0: ret.append([o, "rel", geoms])
  else:
    print ("unknown type!")
    sys.exit()
  return ret      

# - main
t1 = datetime.datetime.now()
# -- open connection
(host, db, tableFull, user, password) = sys.argv[1].split(";")
(schema, prefix) = tableFull.split(".")
conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor = conn.cursor()
# -- load definitions of things to extract
defs = loadDefinitions(sys.argv[2])
# -- parse definitions
objs = []
for subtype in ["node", "way", "rel"]:
  objects = None
  print ("Fetching '%s' objects" % subtype)
  for d in defs[subtype]:
    # get objects
    ds = d.split("&")
    for d in ds:
      k,v = d.split("=")
      oss = getObjects(conn, cursor, schema, prefix, subtype, k, v)
      if not objects:
        objects = oss
      else:
        objects = objects.intersection(oss) 
    # get object geometries
  print ("Fetching '%s' geometries" % subtype)
  objects = getAndConvertGeometry(conn, cursor, schema, prefix, objects, subtype, "point")
  objs.extend(objects)

# -- write extracted objects
# --- open connection
# database credentials moved to config.py
(schema, name) = sys.argv[3].split(".")
conn2 = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db_name, db_user, db_host, db_password))
cursor2 = conn2.cursor()
# --- build tables
cursor2.execute("DROP TABLE IF EXISTS %s.%s" % (schema, name))
cursor2.execute("CREATE TABLE %s.%s ( gid bigint, type varchar(4) );" % (schema, name))
cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'the_geom', 4326, 'POINT', 2);" % (schema, name))
conn2.commit()
# --- insert objects
cnt = 0
for o in objs:
  points = ["%s %s" % (g[0], g[1]) for g in o[2]]
  points = ",".join(points)
  #print("INSERT INTO %s.%s(gid, type, the_geom) VALUES (%s, '%s', ST_Centroid(ST_ConvexHull(ST_GeomFromText('MULTIPOINT(%s)', 4326))));" % (schema, name, o[0], o[1], points))
  cursor2.execute("INSERT INTO %s.%s(gid, type, the_geom) VALUES (%s, '%s', ST_Centroid(ST_ConvexHull(ST_GeomFromText('MULTIPOINT(%s)', 4326))));" % (schema, name, o[0], o[1], points))
  cnt = cnt + 1
  if cnt>10000:
    cnt = 0
    conn2.commit()
conn2.commit()
# --- finish
t2 = datetime.datetime.now()
dt = t2-t1
print ("Built %s objects" % len(objs))
print (" in %s" % dt)


