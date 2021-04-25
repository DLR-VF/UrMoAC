#!/usr/bin/env python
# =========================================================
# postgishelper.py
# @author Daniel Krajzewicz
# @date 27.05.2015
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Some postgis parser methods
# =========================================================
class MMultipoly:
  def __init__(self):
    self.polys = []
    self.color = None
    self.value = None
  def addPoly(self, poly):
    self.polys.append(poly)

def parseMULTIPOLY2XYlists(which, scale=1.):
  which = which[13:-1]
  which = which.split("(")
  cpolys = []
  for polygon in which:
    if len(polygon.strip())==0:
      continue
    polygon = polygon.replace(")", "")
    points = polygon.split(",")
    cpoints = []
    for p in points:
      if len(p.strip())==0:
        continue
      xy = p.split(" ")
      cpoints.append( [ float(xy[0])/scale, float(xy[1])/scale ] )
    cpolys.append(cpoints)
  return cpolys

def parsePOINT2XY(which, scale=1.):
  which = which[6:-1]
  xy = which.split(" ")
  return [ float(xy[0])/scale, float(xy[1])/scale ]

