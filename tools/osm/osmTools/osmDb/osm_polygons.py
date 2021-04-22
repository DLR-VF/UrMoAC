#!/usr/bin/env python

import os, string, sys
import datetime

from optparse import OptionParser
from xml.sax import saxutils, make_parser, handler

import psycopg2
import osmDb.osm_db as osmdb

from pylab import *
import matplotlib.patches as patches
from matplotlib.collections import PatchCollection

def parsePOINT2XY(which, scale=1.):
    which = which[6:-1]
    xy = which.split(" ")
    return [ float(xy[0])/scale, float(xy[1])/scale ]
  
  
def write_polygons(db_name,  user, host, psw, location, ptype, verbose=0):
    if ptype == 'landuse':
        types = {
                 "landuse":["landuse","!!!",True, []],
        }

    if ptype == 'building':
        types = {
                 "building":["building","!!!",True, []],

        }
        
    if ptype == 'amenity':
        types = {                       
                 "amenity":["amenity","!!!",True, []],
        }
    
    if ptype == 'natural':
        types = {                       
                 "natural":["natural","!!!",True, ["tree_row", "coastline", "valley", "ridge", "arete", "cliff"]],
        }

    if ptype == 'shop':
        types = {                       #
                 "shop":["shop","!!!",True, []],
        }
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'"%(db_name,  user, host, psw))
    cursor = conn.cursor()
    db = osmdb.OSMDB(location, conn, cursor)
    
    seen = set()
    for t in types:
        if verbose>0:
            print "Processing %s..." % t
        uType = types[t][0]
        if uType not in seen:
          seen.add(uType)
          if db.tableExists("osm", "%s_%s_polys" % (location, uType)):
            cursor.execute("""DROP TABLE osm.%s_%s_polys;""" % (location, uType))
            conn.commit()
          cursor.execute("""CREATE TABLE osm.%s_%s_polys (
            id bigint, 
            type text,
            level text,
            nodes bigint[][]    
          );""" % (location, uType))
          cursor.execute("""SELECT AddGeometryColumn('osm', '%s_%s_polys', 'shape', 4326, 'POLYGON', 2);""" % (location, uType))
          conn.commit()

        if t.find(".")>=0:
          k,v = t.split(".") 
          tIDs = db.getWayKV_withMatchingKeyValue(k, v)
        else:
          tIDs = db.getWayKV_withMatchingKey(t)
        seenNodes = set()
        for p in tIDs:
          pID = p[0]
          attrs = db.getWayKV_forID(pID)
          for a in attrs:
            if a[1]==t:
              subtype = a[2]
            if a[1]=='building:levels':
              levels = a[2]
          pDef = db.getWay(pID)
          pNodes = db.getNodes_preserveOrder(pDef[0][1], verbose)

          xs = []
          ys = []
          pGeom = []
          for n in pNodes:
            seenNodes.add(n[0])
            pp = parsePOINT2XY(n[1])
            xs.append(pp[0])
            ys.append(pp[1])
            pGeom.append("%s %s" % (pp[0], pp[1])) 
          # store the road
          stype = ""
          if len(pGeom)==1:
            stype = "poi"
          else:
            if (types[t][2] and subtype not in types[t][3]) or pNodes[0][0]==pNodes[-1][0]:
              # should be a polygon
              stype = "polygon"
              if pNodes[0][0]!=pNodes[-1][0]:
                if len(pNodes)<3:
                  # is not a polygon - just two points; probably a mistake
                  stype = "linestring"
                else:
                  pNodes.append(pNodes[0])
                  pGeom.append(pGeom[0])
            else:
              stype = "linestring"
          if stype=="poi":
              continue

          elif stype=="polygon":
            
            if len(pGeom)<4:
              if verbose>0:
                  print 'Polygon could not be written. There are not enough points to create a polygon '
              continue
            else:
              if 'levels' in locals():
                db.addPolygon_with_level("%s_%s_polys" % (location, uType), pID, subtype, levels, pDef[0][1], ",".join(pGeom))
                del levels
              else:
                db.addPolygon("%s_%s_polys" % (location, uType), pID, subtype, pDef[0][1], ",".join(pGeom))


          if verbose>0:  
            print "added %s as %s %s (%s)" % (pID, uType, stype, t)
          
        if t.find(".")>=0:
          k,v = t.split(".") 
          tIDs = db.getNodeKV_withMatchingKeyValue(k, v)
        else:
          tIDs = db.getNodeKV_withMatchingKey(t)
        for p in tIDs:
          pID = p[0]
          if pID in seenNodes:
            continue
          attrs = db.getNodeKV_forID(pID)
          for a in attrs:
            if a[1]==t:
              subtype = a[2]
          n = db.getNodesWGeom([pID])[0]
          pp = parsePOINT2XY(n[-1])

       
