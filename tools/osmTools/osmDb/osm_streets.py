#!/usr/bin/env python

# ToDo
# - modi
# - same nodes combination
# - circle roads
# all additional stuff (number of lanes, ...)

import os, string, sys
import datetime

from optparse import OptionParser
from xml.sax import saxutils, make_parser, handler

import psycopg2
import osmDb.osm_db as osmdb

from pylab import *
import matplotlib.patches as patches
from matplotlib.collections import PatchCollection

from osmDb.osm_modes import *
import datetime

def parsePOINT2XY(which, scale=1.):
  which = which[6:-1]
  xy = which.split(" ")
  return [ float(xy[0])/scale, float(xy[1])/scale ]
  
  
def checkSizeAndExistance(rID, valName, val, maxSize):
  if not val: val = ""
  if len(val)>=maxSize:
    print " Prunning value for '%s' for road '%s'" % (valName, rID)
    print "  was: %s" % val
    val = val[0:maxSize]
    print "  is: %s" % val
  return val
  
storedRoads = []

def addRoad(upperType, rID, fromNode, toNode, rtype, modes, numLanes, vmax, rNodes, sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, rGeom):
  sidewalk = checkSizeAndExistance(rID, "sidewalk", sidewalk, 40)
  cycleway = checkSizeAndExistance(rID, "cycleway", cycleway, 40)
  surface = checkSizeAndExistance(rID, "surface", surface, 40)
  lit = checkSizeAndExistance(rID, "lit", lit, 40)
  parking_both = checkSizeAndExistance(rID, "parking_both", parking_both, 40)
  parking_left = checkSizeAndExistance(rID, "parking_left", parking_left, 40)
  parking_right = checkSizeAndExistance(rID, "parking_right", parking_right, 40)
  rep = {"id":rID, "fromNode":fromNode, "toNode":toNode, "osm_type":rtype, "modes":modes, 
    "nodes":','.join([str(x) for x in rNodes]), "lanes":numLanes, "vmax":vmax, "sidewalk":sidewalk, "cycleway":cycleway, 
    "surface":surface, "name":name, "shape":rGeom, "lit":lit, "parking_both":parking_both, "parking_left":parking_left, "parking_right":parking_right}
  storedRoads.append(rep)

def commitRoads(conn, cursor, dbprefix):
  for sr in storedRoads:
    if (sr["modes"]&FOOT)!=0:
      mode_ped = 't'
    else:
      mode_ped = 'f'
    if (sr["modes"]&BICYCLE)!=0:
      mode_bic = 't'
    else:
      mode_bic = 'f'
    if (sr["modes"]&BICYCLE)!=0:
      mode_bic = 't'
    else:
      mode_bic = 'f'
    if (sr["modes"]&(BUS|PSV))!=0:
      mode_pt = 't'
    else:
      mode_pt = 'f'
    if (sr["modes"]&PASSENGER)!=0:
      mode_mit = 't'
    else:
      mode_mit = 'f'
    #print "INSERT INTO osm."+dbprefix+"_network(oid, nodefrom, nodeto, numlanes, length, vmax, street_type, capacity, mode_walk, mode_bike, mode_pt, mode_mit, modes, nodes, sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, the_geom) VALUES('%s', %s, %s, %s, %s, %s, '%s', -1, '%s', '%s', '%s', '%s', %s, '{%s}', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', ST_GeomFromText('MULTILINESTRING((%s))', 4326))" % (sr["id"], sr["fromNode"], sr["toNode"], sr["lanes"], -1, sr["vmax"], sr["osm_type"], mode_ped, mode_bic, mode_pt, mode_mit, sr["modes"], sr["nodes"], sr["sidewalk"], sr["cycleway"], sr["surface"], sr["lit"], sr["name"], sr["parking_both"],sr["parking_left"],sr["parking_right"], sr["shape"])
    cursor.execute("INSERT INTO osm."+dbprefix+"_network(oid, nodefrom, nodeto, numlanes, length, vmax, street_type, capacity, mode_walk, mode_bike, mode_pt, mode_mit, modes, nodes, sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, the_geom) VALUES('%s', %s, %s, %s, %s, %s, '%s', -1, '%s', '%s', '%s', '%s', %s, '{%s}', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', ST_GeomFromText('MULTILINESTRING((%s))', 4326))" % (sr["id"], sr["fromNode"], sr["toNode"], sr["lanes"], -1, sr["vmax"], sr["osm_type"], mode_ped, mode_bic, mode_pt, mode_mit, sr["modes"], sr["nodes"], sr["sidewalk"], sr["cycleway"], sr["surface"], sr["lit"], sr["name"], sr["parking_both"],sr["parking_left"],sr["parking_right"], sr["shape"]))
  conn.commit()
  del storedRoads[:]

                                   
class Params:
  def __init__(self):
    self.params = {}  
    self.consumed = set()
    self.errorneous = {}  
  def get(self, which, defaultValue=None):
    return self.params[which] if which in self.params else defaultValue
  def consume(self, which):
    self.consumed.add(which)
  def markBad(self, which):
    if which in self.params:
      self.errorneous[which] = self.params[which]
  def getUnconsumed(self, toSkip):
    ret = {}
    for p in self.params:
      if p in self.consumed:
        continue
      if p in toSkip:
        continue
      ret[p] = self.get(p)
    return ret      


# http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany
highways = {
  "highway_motorway":{"mode":MOTORISED, "vmax":160, "oneway":True, "lanes":2},
  "highway_motorway_link":{"mode":MOTORISED, "vmax":80, "oneway":True, "lanes":1},
  "highway_trunk":{"mode":ALL, "vmax":100, "lanes":2},
  "highway_trunk_link":{"mode":ALL, "vmax":80, "lanes":1},
  "highway_primary":{"mode":ALL, "vmax":100, "lanes":2}, 
  "highway_primary_link":{"mode":ALL, "vmax":80, "lanes":1},
  "highway_secondary":{"mode":ALL, "vmax":100, "lanes":2}, 
  "highway_secondary_link":{"mode":ALL, "vmax":80, "lanes":1},
  "highway_tertiary":{"mode":ALL, "vmax":80, "lanes":1},
  "highway_tertiary_link":{"mode":ALL, "vmax":80, "lanes":1},
  "highway_unclassified":{"mode":ALL, "vmax":80, "lanes":1},
  "highway_residential":{"mode":ALL, "vmax":50, "lanes":1},
  "highway_living_street":{"mode":ALL, "vmax":10, "lanes":1},
  "highway_road":{"mode":ALL, "vmax":30, "lanes":1},
  
  "highway_service":{"mode":DELIVERY, "vmax":20, "lanes":1},
  "highway_track":{"mode":ALL, "vmax":20, "lanes":1},  # all only if destination (http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany)
  "highway_services":{"mode":ALL, "vmax":30, "lanes":1},
  "highway_unsurfaced":{"mode":ALL, "vmax":30, "lanes":1},

  # TODO: we should decide which of the following ones may be used by which mode
  "highway_path":{"mode":SOFT, "vmax":10, "lanes":1},
  "highway_bridleway":{"mode":HORSE, "vmax":10, "lanes":1},
  "highway_cycleway":{"mode":BICYCLE|MOPED, "vmax":10, "lanes":1},
  "highway_pedestrian":{"mode":FOOT, "vmax":10, "lanes":1},
  "highway_footway":{"mode":FOOT, "vmax":10, "lanes":1},
  "highway_step":{"mode":SOFT, "vmax":10, "lanes":1},
  "highway_steps":{"mode":SOFT, "vmax":10, "lanes":1},
  "highway_stairs":{"mode":SOFT, "vmax":10, "lanes":1},
  "highway_bus_guideway":{"mode":BUS, "vmax":50, "lanes":1},
  
  # 
  "highway_raceway":{"mode":CLOSED, "vmax":160, "lanes":1},
  "highway_ford":{"mode":CLOSED, "vmax":10, "lanes":1},

  "railway_rail":{"mode":RAIL, "vmax":300, "oneway":True, "lanes":1},
  "railway_tram":{"mode":RAIL, "vmax":100, "oneway":True, "lanes":1},
  "railway_light_rail":{"mode":RAIL, "vmax":100, "oneway":True, "lanes":1},
  "railway_subway":{"mode":RAIL, "vmax":100, "oneway":True, "lanes":1},
  "railway_preserved":{"mode":RAIL, "vmax":100, "oneway":True, "lanes":1}
}


def getDirectionalModes(rid, defaultOneway, params, modesF):
  modesB = modesF
  if defaultOneway:
    modesB = modesB & ~MOTORISED
    modesB = modesB & ~BICYCLE
    modesB = modesB & ~RAIL
    modesB = modesB & ~TRAM
    # check tracks
    tracks = params.get("tracks")
    if tracks!=None:
      try:
        if int(tracks)>1:
          modesB = modesF
      except:
        if tracks.find(";")>=0:
          modesB = modesF
  # check oneway    
  oneway = params.get("oneway")
  if oneway in ["true", "yes", "1"] or (defaultOneway and oneway not in ["false", "no", "0"]):
    modesB = modesB & ~MOTORISED
    modesB = modesB & ~BICYCLE
    modesB = modesB & ~RAIL
    modesB = modesB & ~TRAM
    for m in modes1:
      modeAccess = params.get("oneway:"+modes1[m]["mml"])
      params.consume("oneway:"+modes1[m]["mml"])
      if modeAccess not in accessAllows:
        params.markBad("oneway:"+modes1[m]["mml"])
        continue
      if accessAllows[modeAccess]:
        modesB = modesB | m
      else:
        modesB = modesB & ~m
  if oneway in ["reverse", "-1"]:
    modesF = modesF & ~MOTORISED
    modesB = modesB & ~BICYCLE
    modesB = modesB & ~RAIL
    modesB = modesB & ~TRAM
    for m in modes1:
      modeAccess = params.get("oneway:"+modes1[m]["mml"])
      params.consume("oneway:"+modes1[m]["mml"])
      if modeAccess not in accessAllows:
        params.markBad("oneway:"+modes1[m]["mml"])
        continue
      if accessAllows[modeAccess]:
        modesF = modesF | m
      else:
        modesF = modesF & ~m
  if oneway not in ["true", "yes", "1", "reverse", "-1", "false", "no", "0", None, ""]:
    params.markBad("oneway")
  params.consume("oneway")
  # check per-direction information
  for m in modes1:
    modeAccess = params.get(modes1[m]["mml"]+":forward")
    params.consume(modes1[m]["mml"]+":forward")
    if modeAccess not in ["yes", "no"]:
      params.markBad(modes1[m]["mml"]+":forward")
      continue
    if modeAccess=="yes":
      modesF = modesF | m
    if modeAccess=="no":
      modesF = modesF & ~m
  for m in modes1:
    modeAccess = params.get(modes1[m]["mml"]+":backward")
    params.consume(modes1[m]["mml"]+":backward")
    if modeAccess not in ["yes", "no"]:
      params.markBad(modes1[m]["mml"]+":backward")
      continue
    if modeAccess=="yes":
      modesB = modesB | m
    if modeAccess=="no":
      modesB = modesB & ~m
  # check other information
  junction = params.get("junction")
  if oneway==None or oneway=="":
    # check roundabouts
    if junction=="roundabout":
      modesB = modesB & ~MOTORISED
      modesB = modesB & ~BICYCLE
      modesB = modesB & ~RAIL
      modesB = modesB & ~TRAM
  params.consume("junction")
  #
  return modesF, modesB




def getLanes(defaultLanes, params):
  lanes = params.get("lanes")
  if lanes==None:
    return defaultLanes
  l = defaultLanes
  if lanes.find(";")>=0:
    vals = lanes.split(";")
    l = min([int(l) for l in vals])
  else:
    try: 
      l = int(lanes)
    except:
      params.markBad("lanes") 
  params.consume("lanes")
  return l

def getVMax(defaultSpeed, params):
  maxspeed = params.get("maxspeed")
  if maxspeed==None:
    return defaultSpeed
  v = maxspeed.lower()
  if v.find("km/h")>=0:
    v = int(v[:v.find("km/h")].strip()) / 3.6
  elif v.find("kmh")>=0:
    v = int(v[:v.find("kmh")].strip()) / 3.6
  elif v.find("mph")>=0:
    v = int(v[:v.find("mph")].strip()) / 3.6 * 1.609344
  elif v=="signals":
    return defaultSpeed
  elif v=="none":
    v = 300.
  elif v=="no":
    v = 300.
  elif v=="walk":
    v = 5.
  elif v=="DE:rural":
    v = 100.
  elif v=="DE:urban":
    v = 50.
  elif v=="DE:living_street":
    v = 10.
  else:
    try: 
      v = float(v)
    except:
      params.markBad("maxspeed") 
      v = defaultSpeed
  params.consume("maxspeed")
  return v



accessAllows = {
  "yes":True,
  "private":False,
  "no":False,
  "permissive":True,
  "agricultural":False,
  "use_sidepath":True,
  "delivery":False,
  "designated":True,
  "dismount":True,
  "discouraged":True,
  "forestry":False,
  "destination":True,
  "customers":True,
  "official":True, # should be "designated"
  "emergency":False,
  "service":False,
}


def getModes(rid, defaultModes, params):
  # check for motorroad information (http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany)
  motorroad = params.get("motorroad")
  if motorroad=="yes":
    defaultModes = defaultModes & ~SOFT
    defaultModes = defaultModes & ~MOPED
  if motorroad not in ["yes", "no"]: params.markBad("motorroad")
  params.consume("motorroad")
  # check for sidewalks
  sidewalk = params.get("sidewalk")
  if sidewalk in ["sidewalk", "crossing", "left", "right", "both", "yes"]:
    defaultModes = defaultModes | FOOT
  if sidewalk not in ["none", "no", "sidewalk", "crossing", "left", "right", "both", "yes", "separate"]:
    params.markBad("sidewalk")
  params.consume("sidewalk")
  # check for cycleways
  cycleway = params.get("cycleway")
  if cycleway in ["track", "opposite_track", "segregated", "lane", "shared_lane", "opposite_lane", "share_busway", "right", "opposite", "yes", "shared"]:
    defaultModes = defaultModes | BICYCLE
  if cycleway not in ["track", "opposite_track", "segregated", "lane", "none", "shared_lane", "opposite_lane", "crossing", "share_busway", "right", "no", "opposite", "yes", "shared"]:
    params.markBad("cycleway")
  params.consume("cycleway")
  # check for access restrictions
  # - global
  access = params.get("access")
  params.consume("access")
  if access!=None:
    if access not in accessAllows:
      params.markBad("access")
    else:  
      if not accessAllows[access]:
#        defaultModes = defaultModes | ALL
#      else:
        defaultModes = defaultModes & ~ALL
  # - per mode
  for m in modes1:
    modeAccess = params.get(modes1[m]["mml"])
    params.consume(modes1[m]["mml"])
    if modeAccess not in accessAllows:
      params.markBad(modes1[m]["mml"])
      continue
    if accessAllows[modeAccess]:
      defaultModes = defaultModes | m
    else:
      defaultModes = defaultModes & ~m
  return defaultModes, cycleway, sidewalk

               
def getParams(params):
  ret = Params()
  for p in params:
    ret.params[p[1]] = p[2]
  return ret 

def write_street_network(db_name,  user, host, psw, location, verbose=0 ):
  t1 = datetime.datetime.now()
  conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'"%(db_name,  user, host, psw))
  cursor = conn.cursor()
  db = osmdb.OSMDB(location, conn, cursor)
  #db = osmDb.OSMDB(sys.argv[1], conn, cursor)

  # TODO: now do something about the schema!!!
  #t_ = sys.argv[1][sys.argv[1].find(".")+1:]

  fdo1 = open("logs/%s_unconsumed.txt" % location, "w")
  fdo2 = open("logs/%s_errorneous.txt" % location, "w")

  # (re) create tables
  if db.tableExists("osm", location+"_network"):
      cursor.execute("""DROP TABLE osm.%s_network;""" % (location))
      conn.commit()
  cursor.execute("""CREATE TABLE osm.%s_network (
    id serial PRIMARY KEY,
    oid text,
    nodefrom bigint, 
    nodeto bigint, 
    numlanes smallint,
    length double precision,
    vmax double precision,
    street_type text,
    capacity double precision,
    mode_walk boolean,
    mode_bike boolean,
    mode_pt boolean,
    mode_mit boolean,    
    modes bigint,
    nodes bigint[],
    sidewalk varchar(40),
    cycleway varchar(40),
    width double precision,
    surface varchar(40),
    lit varchar(40),
    name text,
    slope real, 
    parking_both varchar(40),
    parking_left varchar(40),
    parking_right varchar(40)   
  );""" % location)
  cursor.execute("""SELECT AddGeometryColumn('osm', '%s_network', 'the_geom', 4326, 'MULTILINESTRING', 2);""" % (location))
  conn.commit()


  # get roads and railroads with some typical classes
  seen = set()
  nodes = {}
  for upperType in ["highway", "railway"]:
    if verbose>0:
        print "reading "+upperType+" nodes"
    IDs = db.getWayKV_withMatchingKey(upperType)
    # get nodes usage
    # currently in memory; this will fail for larger networks
    for i,h in enumerate(IDs):
      hID = h[0]
      if hID in seen:
        continue
      seen.add(hID)
      htype = h[1]+"_"+h[2]
      if htype not in highways:
        #print "unrecognized highway type %s" % htype
        continue
      else:
        hDef = db.getWay(hID)
        #print hDef
        for n in hDef[0][1]:
          if n in nodes:
            nodes[n] = nodes[n] + 1
          else:
            nodes[n] = 1 

  seen = set()
  for upperType in ["highway", "railway"]:
    
    # build roads by splitting geometry
    IDs = db.getWayKV_withMatchingKey(upperType)
    for i,h in enumerate(IDs):
      hID = h[0]
      if hID in seen:
        continue
      htype = h[1]+"_"+h[2]
      if htype not in highways:
        continue
      else:
        seen.add(hID)
        # get and check params
        params = getParams(db.getWayKV_forID(hID))
        
        defaultModes = highways[htype]["mode"]
        if upperType=="highway" and params.get("railway")!=None:
          ut2 = "railway_" + params.get("railway")
          if ut2 in highways:
            defaultModes = defaultModes | highways[ut2]["mode"] 
        if upperType=="railway" and params.get("highway")!=None:
          ut2 = "highway_" + params.get("highway")
          if ut2 in highways:
            defaultModes = defaultModes | highways[ut2]["mode"] 
        modes, cycleway, sidewalk = getModes(hID, defaultModes, params)
        
        defaultOneway = "oneway" in highways[htype] and highways[htype]["oneway"] 
        
        modesF, modesB = getDirectionalModes(hID, defaultOneway, params, modes) # !!! hier - nun die Richtungsabhaengigen dinge        
        
        # ab hier wieder alles ok
        numLanes = getLanes(highways[htype]["lanes"], params)
        vmax = getVMax(highways[htype]["vmax"], params)
        name = params.get("name", "")
        params.consume("name")
        name = name.replace("'", "") # TODO: patch!
        lit = params.get("lit")
        params.consume("lit")
        surface = params.get("surface")
        params.consume("surface")

        parking_right = params.get("parking:lane:right")
        params.consume("parking:lane:right")
        
        parking_left = params.get("parking:lane:left")
        params.consume("parking:lane:left")
        
        parking_both= params.get("parking:lane:both")
        params.consume("parking:lane:both")
        
        params.consume("highway")
        params.consume("railway")
        
        #
        hDef = db.getWay(hID)
        hNodes = db.getNodes_preserveOrder(hDef[0][1], verbose)
        index = 0
        ni = 0
        hGeom = []
        nodeIDs = []
        while ni!=len(hNodes):
          n = hNodes[ni]
          p = parsePOINT2XY(n[1])
          nodeIDs.append(n[0])
          hGeom.append("%s %s" % (p[0], p[1]))
          if nodes[n[0]]>1 and ni>0:
            # store the road
            if modesF!=0:
              addRoad(upperType, "f%s#%s" % (hID, index), nodeIDs[0], nodeIDs[-1], htype, modesF, numLanes, vmax, nodeIDs, sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, ",".join(hGeom))
            if modesB!=0:
              addRoad(upperType, "b%s#%s" % (hID, index), nodeIDs[-1], nodeIDs[0], htype, modesB, numLanes, vmax, reversed(nodeIDs), sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, ",".join(reversed(hGeom)))
            hGeom = []
            hGeom.append("%s %s" % (p[0], p[1]))
            nodeIDs = []
            nodeIDs.append(n[0])
            index = index + 1
          ni = ni + 1
        if len(hGeom)>1: 
          if modesF!=0:     
            addRoad(upperType, "f%s#%s" % (hID, index), nodeIDs[0], nodeIDs[-1], htype, modesF, numLanes, vmax, nodeIDs, sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, ",".join(hGeom))
          if modesB!=0:     
            addRoad(upperType, "b%s#%s" % (hID, index), nodeIDs[-1], nodeIDs[0], htype, modesB, numLanes, vmax, reversed(nodeIDs), sidewalk, cycleway, surface, lit, name, parking_both, parking_left, parking_right, ",".join(reversed(hGeom)))

        unconsumed = params.getUnconsumed(["source:lit", "note:name", "postal_code", "name:ru", "created_by", "old_name", "trail_visibility",
          "source:maxspeed", "source"])
        if len(unconsumed)>0: fdo1.write("%s:%s\n" % (hID, str(unconsumed)))
        if len(params.errorneous)>0: fdo2.write("%s:%s\n" % (hID, str(params.errorneous)))

      if len(storedRoads)>1000:
        commitRoads(db.conn, db.cursor, location)

  commitRoads(db.conn, db.cursor, location)
  cursor.execute("""UPDATE osm.%s_network SET length=ST_Length(the_geom::geography);""" % (location))
  conn.commit()
  fdo1.close()
  fdo2.close()
  t2 = datetime.datetime.now()
  dt = t2-t1
  if verbose>0:
      print "In %s" % dt



     
