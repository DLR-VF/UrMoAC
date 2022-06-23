#!/usr/bin/env python
# =========================================================
# osmdb_getStructures.py
# @author Daniel Krajzewicz
# @author Simon Nieland
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


# --- imported modules ------------------------------------
import sys, os
import psycopg2
import datetime

script_dir = os.path.dirname( __file__ )
mymodule_dir = os.path.join(script_dir, '..', 'helper')
sys.path.append(mymodule_dir)
from wkt import *


# --- global definitions ----------------------------------
"""! @brief A map from a data type to the respective tags"""
subtype2tag = {
  "node": "ntag",
  "way": "wtag",
  "rel": "rtag"
}


# --- class definitions -----------------------------------
class OSMExtractor:
  """! @brief A class for extracting defined structures from OSM
  
  The class reads a set of definitions of the things to extract from a definitions file.
  The definitions are stored as !!!.
  OSM data is retrieved from a database that was imported using "osm2db.py".
  @see osm2db.py  
  """
  def __init__(self):
    """! brief Constructor"""
    self._defs = { "node": [], "way": [], "rel": [] }
    self._objectIDs = { "node": [], "way": [], "rel": [] }
    self._objectGeoms = { "node": {}, "way": {}, "rel": {} }
    

  def loadDefinitions(self, fileName):
    """! @brief Loads the definitions about what to extract from a file
    
    @param fileName The name of the file to read the definitions from
    """
    fd = open(fileName)
    subtype = ""
    for l in fd:
      if len(l.strip())==0:
        continue
      if l[0]=='#':
        continue
      if l[0]=='[':
        subtype = l[1:l.find(']')].strip()
        continue
      self._defs[subtype].append(l.strip())
    return self._defs
    

  def _getObjects(self, conn, cursor, schema, prefix, subtype, k, v):
    """! @brief Returns the IDs of the objects in the given OSM table that match the given definitions 
    
    @param conn The database connection to use
    @param cursor The database cursor to use 
    @param schema The database sceham to use
    @param prefix The OSM database prefix to use
    @param subtype The OSM subtype to extract information about
    @param k The key value to match
    @param v The value value to match, "*" if any
    @todo Make database connection an attribute of the class
    """
    ret = set()
    if v=='*':
      cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s'" % (schema, prefix, subtype2tag[subtype], k))
    else:
      cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s' AND v='%s'" % (schema, prefix, subtype2tag[subtype], k, v))
    conn.commit()
    for r in cursor.fetchall():
      ret.add(int(r[0]))
    # !!! add option for extracting (a) certain structure/s
    #ret = set()
    #if subtype=="rel":
    #  ret.add(1995405)
    return ret


  def loadObjectIDs(self, conn, cursor, schema, prefix):
    """! @brief Returns the IDs of the objects in the given OSM data that match the given definitions 
    
    @param conn The database connection to use
    @param cursor The database cursor to use 
    @param schema The database sceham to use
    @param prefix The OSM database prefix to use
    @todo Make database connection an attribute of the class
    """
    for subtype in ["node", "way", "rel"]:
      print (" ... for %s" % subtype)
      for d in self._defs[subtype]:
        # get objects
        ds = d.split("&")
        for d in ds:
          k,v = d.split("=")
          oss = self._getObjects(conn, cursor, schema, prefix, subtype, k, v)
          if len(self._objectIDs[subtype])==0:
            self._objectIDs[subtype] = oss
          else:
            self._objectIDs[subtype] = self._objectIDs[subtype].union(oss) 
      print (" ... %s found" % len(self._objectIDs[subtype]))
      # !!! make this optional
      #for o in self._objectIDs[subtype]:
      #  print ("%s %s" % (subtype, o))


  def _getWayGeom(self, w, way2points, node2pos):
    """! @brief Returns the geometry of the named way using the given, previously loaded data
    
    @param w The way to get the geometry for
    @param way2points Container of way geometries
    @param node2pos Container of node positions
    @todo Make way2points, node2pos class attributes
    """
    if w not in way2points:
      print ("Missing node list definition for way %s" % w)
      return None
    points = []
    for n in way2points[w]:
      if n not in node2pos:
        print ("Missing position for node %s within way %s" % (n, w))
        continue
      points.append(node2pos[n])
    return points
    

  def _getRelGeom(self, r, rel2items, way2points, node2pos):
    """! @brief Returns the geometry of the named releation using the given, previously loaded data
    
    @param r The relation to get the geometry for
    @param rel2items Container of relation definitions
    @param way2points Container of way geometries
    @param node2pos Container of node positions
    @todo Make rel2items, way2points, node2pos class attributes
    """
    if r not in rel2items:
      print ("Missing item list definition for rel %s" % r)
      return None
    items = {}
    for i in rel2items[r]:
      if i[1]=="rel":
        g = self._getRelGeom(i[0], rel2items, way2points, node2pos)
        if not g:
          print ("Empty geometry for relation %s in relation %s" % (i[0], r))
          continue
        if i[2] not in items:
          items[i[2]] = []
        items[i[2]].append([GeometryType.MULTIPOLYGON, g])
      elif i[1]=="way":
        g = self._getWayGeom(i[0], way2points, node2pos)
        if not g:
          print ("Empty geometry for way %s in relation %s" % (i[0], r))
          continue
        if i[2] not in items:
          items[i[2]] = []
        items[i[2]].append([GeometryType.LINESTRING, g])
      elif i[1]=="node":
        continue
        if i[0] not in node2pos:
          print ("Missing position of node %s in relation %s" % (i[0], r))
          continue
        items[i[2]].append([GeometryType.POINT, node2pos[i[0]]])
      else:
        print ("Unknown geometry type %s in relation %s" % (i[1], i[0]))
    return items
    
    
  def _signed_area(self, poly):
    """! brief Returns the orientation of the polygon
    
    Taken from https://gis.stackexchange.com/questions/298290/checking-if-vertices-of-polygon-are-in-clockwise-or-anti-clockwise-direction-in
    with adaptations.
    
    @param poly The polygon to evaluate
    """
    return sum(poly[i][0]*(poly[i+1][1]-poly[i-1][1]) for i in range(1, len(poly)-1))/2.0    


  def collectObjectGeometries(self, conn, cursor, schema, prefix, roles=None):
    """! @brief Collects all needed geometry information 
    
    @param conn The database connection to use
    @param cursor The database cursor to use 
    @param schema The database sceham to use
    @param prefix The OSM database prefix to use
    @param roles If given, only roles included in this list are loaded
    @todo Make database connection an attribute of the class
    @fixme Roles are not used
    @todo split into sub-functions
    """
    missingRELids = list(self._objectIDs["rel"])
    missingWAYids = set(self._objectIDs["way"])
    missingNODEids = set(self._objectIDs["node"])
    
    # collect relation members, first
    seenRELs = set(missingRELids)
    rel2items = {}
    print (" ... for relations")
    while len(missingRELids)!=0:
      idstr = ",".join([str(id) for id in missingRELids])
      cursor.execute("SELECT rid,elemid,type,role FROM %s.%s_member WHERE rid in (%s) ORDER BY rid,role,idx" % (schema, prefix, idstr))
      conn.commit()
      missingRELids = []
      for r in cursor.fetchall():
        # close the currently processed relation if a new starts
        rid = int(r[0])
        if rid not in rel2items:
          rel2items[rid] = []
        # get a member item, add it
        iid = int(r[1])
        role = r[3]
        if roles and role not in roles:
          continue
        rel2items[rid].append((iid, r[2], r[3]))
        if r[2]=="rel" or r[2]=="relation":
          if iid==int(r[0]) or iid in seenRELs:
            print ("Self-referencing relation %s" % r[0])
            continue
          missingRELids.append(iid)
        elif r[2]=="way":
          missingWAYids.add(iid)
        elif r[2]=="node":
          missingNODEids.add(iid)
        else:
          print ("Check type '%s'" % r[2])
    # collect ways 
    way2points = {}
    print (" ... for ways")
    # https://stackoverflow.com/questions/30773911/union-of-multiple-sets-in-python
    npoints = [missingNODEids]
    if len(missingWAYids)!=0:
      idstr = ",".join([str(id) for id in missingWAYids])
      cursor.execute("SELECT id,refs FROM %s.%s_way WHERE id in (%s)" % (schema, prefix, idstr))
      conn.commit()
      for r in cursor.fetchall():
        id = int(r[0])
        way2points[id] = r[1]
        npoints.append(way2points[id])
    missingNODEids = set.union(*npoints)    
    # collect nodes
    node2pos = {}
    print (" ... for nodes")
    if len(missingNODEids)!=0:
      idstr = ",".join([str(id) for id in missingNODEids])
      cursor.execute("SELECT id,ST_AsText(pos) FROM %s.%s_node WHERE id in (%s)" % (schema, prefix, idstr))
      conn.commit()
      for r in cursor.fetchall():
        node2pos[int(r[0])] = parsePOINT2XY(r[1])

    # build geometries
    #  nodes
    for n in self._objectIDs["node"]:
      if n not in node2pos:
        print ("Missing position for node %s" % n)
        continue
      self._objectGeoms["node"][n] = [GeometryType.POINT, node2pos[n]]
    #  ways
    for w in self._objectIDs["way"]:
      wgeom = self._getWayGeom(w, way2points, node2pos)
      if not wgeom:
        continue
      self._objectGeoms["way"][w] = [GeometryType.LINESTRING, wgeom]
    #  relations
    for r in self._objectIDs["rel"]:
      rgeom = self._getRelGeom(r, rel2items, way2points, node2pos)
      if not rgeom:
        continue
      for t in rgeom:
        # only "outer" has to be rechecked?
        if t!="outer":
          continue
        # compute possible consecutions (forward / backward)
        sws = rgeom[t]
        for i,w1 in enumerate(sws):
          w1.append([])
          for j,w2 in enumerate(sws):
            if i==j:
              continue
            if w1[1][-1]==w2[1][0]: # ok, plain continuation
              w1[-1].append([j, 0])
            if w1[1][-1]==w2[1][-1]: # second would have to be mirrored
              w1[-1].append([j, 1])
            if w1[1][0]==w2[1][0]: # this would have to be mirrored
              w1[-1].append([j, 2])
            if w1[1][0]==w2[1][-1]: # both have to be mirrored
              w1[-1].append([j, 3])
        
        # compute possible combinations
        ways = []
        seen = []
        for w in sws[0][2]:
          ways.append([w])
          seen.append(set())
          seen[-1].add(w[0])
        while True:
          nways = []
          nseen = []
          for i,w in enumerate(ways):
            next = w[-1][0]
            for w1 in sws[next][2]:
              if w1[0] in seen[i]:
                continue
              nways.append(list(w))
              nways[-1].append(w1)
              nseen.append(set(seen[i]))
              nseen[-1].add(w1[0])
          ways = nways
          seen = nseen
          if len(nways)==0 or len(nways[0])==len(sws):
            break;

        # check consecutions' validities
        valids = []
        rotations = []
        ngeoms = []
        for q,w in enumerate(ways):
          if q!=1:
            continue
          valid = True
          rotation = True
          mirrorNext = False
          lastGeom = None
          seen = set()
          ngeom = []
          for i,w1 in enumerate(w):
            if w1[0] in seen:
              valid = False
              rotation = False
              break
            seen.add(w1[0])
            geom = sws[w1[0]][1]
            if len(ngeom)>0 and geom[-1]==ngeom[-1]: # !!!
              geom = list(reversed(list(geom)))
            mirrorNext = (w1[1]&1)
            if len(ngeom)>0:
              ngeom.extend(geom[1:])
            else:
              ngeom.extend(geom)
            lastGeom = sws[w1[0]][1]
          valids.append(valid)
          if rotation:
            rotation = self._signed_area(ngeom)<0
          rotations.append(rotation)
          ngeoms.append(ngeom)
        # check
        if True not in valids:
          print ("Invalid geometry")
          continue
        rgeom[t] = [[GeometryType.MULTIPOLYGON, ngeoms[valids.index(True)]]]
      for t in rgeom:
        for poly in rgeom[t]:
          if poly[1][0]!=poly[1][-1]:
            poly[1].append(poly[1][0])
          rotation = self._signed_area(poly[1])<0
          if t=="outer" and rotation>0:
            poly[1].reverse()
          if t=="inner" and rotation<0:
            poly[1].reverse()
      self._objectGeoms["rel"][r] = [GeometryType.MULTIPOLYGON, rgeom]


  def _getGeomObjects(self):
    """! @brief Converts computed geometries into a database representation
    
    @return A list of objects with geometries
    """
    ret = []
    # nodes
    for n in self._objectGeoms["node"]:
      p = self._objectGeoms["node"][n][1]
      pxm = p[0]-.0001
      pym = p[1]-.0001
      pxp = p[0]+.0001
      pyp = p[1]+.0001
      poly = []
      poly.append("%s %s" % (pxm, pym))
      poly.append("%s %s" % (pxp, pym))
      poly.append("%s %s" % (pxp, pyp))
      poly.append("%s %s" % (pxm, pyp))
      poly.append("%s %s" % (pxm, pym))
      ret.append([n, "node", [poly]])
    # ways
    for w in self._objectGeoms["way"]:
      l = self._objectGeoms["way"][w][1]
      poly = ["%s %s" % (p[0], p[1]) for p in l]
      if poly[0]!=poly[-1]:
        poly.append(poly[0])
      ret.append([w, "way", [poly]])
    # relations
    for r in self._objectGeoms["rel"]:
      g = self._objectGeoms["rel"][r]
      npolys = []
      seen = set()
      for role in ["outer", "inner"]:
        seen.add(role)
        if role not in g[1]:
          continue
        for poly in g[1][role]:
          npolys.append(["%s %s" % (p[0], p[1]) for p in poly[1]])
      for role in g[1]:
        if role in seen:
          continue
        for poly in g[1][role]:
          npolys.append(["%s %s" % (p[0], p[1]) for p in poly[1]])
      ret.append([r, "rel", npolys])
    return ret


  def storeObjects(self, conn, cursor, schema, name, namePS):
    """! @brief Stores objects and their geometries in the given table(s)
        
    @param conn The database connection to use
    @param cursor The database cursor to use 
    @param schema The database sceham to use
    @param name The name of the database to store the objects in
    @param namePS The name of the database to store the objects' outline as points in
    @todo Make database connection an attribute of the class
    """
    geoms = self._getGeomObjects()
    cnt = 0
    for o in geoms:
      points = []
      polys = []
      hadSmall = False
      if len(o[2])==0:
        print ("No geometry")
        continue
      for poly in o[2]:
        if len(poly)<4:
          hadSmall = True
          continue
        points.extend(poly)
        polys.append("(" + ",".join(poly) + ")")
      if len(polys)==0:
        print ("No geometry")
        continue
      polys = "(" + ",".join(polys) + ")"
      pointsS = ",".join(points)
      cursor.execute("INSERT INTO %s.%s(gid, type, centroid, shape) VALUES (%s, '%s', ST_Centroid(ST_ConvexHull(ST_GeomFromText('MULTIPOINT(%s)', 4326))), ST_GeomFromText('MULTIPOLYGON(%s)', 4326));" % (schema, name, o[0], o[1], pointsS, polys))
      cnt = cnt + 1
      for idx,p in enumerate(points):
        p = p.replace("(", "").replace(")", "").strip()
        cursor2.execute("INSERT INTO %s.%s(gid, idx, point) VALUES (%s, '%s', ST_GeomFromText('POINT(%s)', 4326));" % (schema, namePS, o[0], idx, p))
        cnt = cnt + 1
      if cnt>1000:
        cnt = 0
        conn.commit()
    conn.commit()
    return len(geoms)

    
# - main
# -- open connection
t1 = datetime.datetime.now()
(host, db, tableFull, user, password) = sys.argv[1].split(";")
(schema, prefix) = tableFull.split(".")
conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor = conn.cursor()

# -- load definitions of things to extract
extractor = OSMExtractor()
print ("Loading definition of things to extract")
extractor.loadDefinitions(sys.argv[2])
print ("Determining object IDs")
extractor.loadObjectIDs(conn, cursor, schema, prefix)
print ("Determining object geometries")
extractor.collectObjectGeometries(conn, cursor, schema, prefix)

# -- write extracted objects
# --- open connection
print ("Building destination databases")
(host, db, tableFull, user, password) = sys.argv[3].split(";")
(schema, name) = tableFull.split(".")
namePS = name + "_ps"
conn2 = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor2 = conn2.cursor()
# --- build tables
cursor2.execute("DROP TABLE IF EXISTS %s.%s" % (schema, name))
cursor2.execute("CREATE TABLE %s.%s ( gid bigint, type varchar(4) );" % (schema, name))
cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'centroid', 4326, 'POINT', 2);" % (schema, name))
cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'shape', 4326, 'MULTIPOLYGON', 2);" % (schema, name))
conn2.commit()

cursor2.execute("DROP TABLE IF EXISTS %s.%s" % (schema, namePS))
cursor2.execute("CREATE TABLE %s.%s ( gid bigint, idx int );" % (schema, namePS))
cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'point', 4326, 'POINT', 2);" % (schema, namePS))
conn2.commit()

# --- insert objects
print ("Storing objects")
num = extractor.storeObjects(conn2, cursor2, schema, name, namePS)

# --- finish
t2 = datetime.datetime.now()
dt = t2-t1
print ("Built %s objects" % num)
print (" in %s" % dt)


