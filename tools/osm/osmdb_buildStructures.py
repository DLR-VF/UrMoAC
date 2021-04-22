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
import sys
import psycopg2
import datetime

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
  cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s' AND v='%s'" % (schema, prefix, subtype2tag[subtype], k, v))
  conn.commit()
  for r in cursor.fetchall():
    ret.add(r[0])
  return ret

def getAndConvertGeometry(conn, cursor, schema, prefix, objects, subtype, destGeom):
  ret = []
  if subtype=="node":
    for o in objects:
      cursor.execute("SELECT id,ST_AsText(pos) FROM %s.%s_node WHERE id=%s" % (schema, prefix, o))
      conn.commit()
      r = cursor.fetchone()
      ret.append([int(r[0]), "node", r[1]])
  elif subtype=="way":
    for o in objects:
      cursor.execute("SELECT id,refs FROM %s.%s_way WHERE id=%s" % (schema, prefix, o))
      conn.commit()
      geom = []
      r = cursor.fetchone()
      ns = r[1]
      for n in ns:
        cursor.execute("SELECT id,ST_AsText(pos) FROM %s.%s_node WHERE id=%s" % (schema, prefix, n))
        conn.commit()
        r2 = cursor.fetchone()
        geom.append(r2[1])
      ret.append([o, "way", geom])
  elif subtype=="rel":
    print ("relations are not supported!")
    sys.exit()
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
  for d in defs[subtype]:
    # get objects
    objects = None
    ds = d.split("&")
    for d in ds:
      k,v = d.split("=")
      oss = getObjects(conn, cursor, schema, prefix, subtype, k, v)
      if not objects:
        objects = oss
      else:
        objects = objects.intersection(oss) 
    # get object geometries
    objects = getAndConvertGeometry(conn, cursor, schema, prefix, objects, subtype, "point")
    objs.extend(objects)

# -- write extracted objects
# --- open connection
(host, db, tableFull, user, password) = sys.argv[3].split(";")
(schema, name) = tableFull.split(".")
conn2 = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor2 = conn2.cursor()
# --- build tables
cursor2.execute("DROP TABLE IF EXISTS %s.%s" % (schema, name))
cursor2.execute("CREATE TABLE %s.%s ( gid bigint, type varchar(4) );" % (schema, name))
cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'the_geom', 4326, 'POINT', 2);" % (schema, name))
conn2.commit()
# --- insert objects
cnt = 0
for o in objs:
  cursor2.execute("INSERT INTO %s.%s(gid, type, the_geom) VALUES (%s, '%s', ST_GeomFromText('%s', 4326));" % (schema, name, o[0], o[1], o[2]))
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


