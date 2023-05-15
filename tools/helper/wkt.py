#!/usr/bin/env python
# =========================================================
# wkt.py
#
# @author Daniel Krajzewicz
# @date 23.06.2022
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Helper methods and classes for dealing with WKTs
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2022-2023 DLR Institute of Transport Research
# All rights reserved.
# =========================================================


# --- imported modules ------------------------------------
from enum import IntEnum


# --- enum definitions ------------------------------------
class GeometryType(IntEnum):
  """! @brief An enumeration of known geometry types"""
  POINT = 0
  LINESTRING = 1
  MULTILINE = 2
  POLYGON = 3
  MULTIPOLYGON = 4
  GEOMETRYCOLLECTION = 5


# --- class definitions -----------------------------------
class Geometry:
  """! @brief A class for storing abstract geometries"""

  def __init__(self, type, shape):
    """! @brief Constructor
    
    @param type The type of the geometry
    @param shape The geometry representation
    """
    self._type = type
    self._shape = shape
    self._patch = None
    
    
  def toPatch(self):
    """! @brief Converts the geometry to a matplotlib patch
    
    @return The geometry as patch
    """
    import matplotlib as mpl
    # return if already built
    if self._patch:
      return self._patch
    # build
    if self._type==GeometryType.POINT:
      self._patch = Circle(self._shape)
    elif self._type==GeometryType.LINESTRING:
      codes = [1]
      codes.extend([2]*(len(self._shape)-1))
      self._patch = mpl.path.Path(self._shape, codes)
    elif self._type==GeometryType.MULTILINE:
      s = []
      c = []
      for shp in self._shape:
        s.extend(shp)
        s.append([-1, -1])
        c.append(1) # MOVETO
        c.extend([2]*(len(shp)-1)) # 2: LINETO
        c.append(0) # 0: STOP
      self._patch = mpl.path.Path(s, c, closed=False)
    elif self._type==GeometryType.POLYGON:
      self._patch = Polygon(self._shape)
    elif self._type==GeometryType.MULTIPOLYGON:
      self._patch = Polygon(self._shape[0]) # !!!
    return self._patch
  def getBounds(self):
    if self._type==GeometryType.POINT:
      return [self._shape[0][0], self._shape[0][1], self._shape[0][0], self._shape[0][1]]
    elif self._type==GeometryType.LINESTRING or self._type==GeometryType.POLYGON:
      bounds = [self._shape[0][0], self._shape[0][1], self._shape[0][0], self._shape[0][1]]
      for p in self._shape:
        bounds[0] = min(bounds[0], p[0])
        bounds[1] = min(bounds[1], p[1])
        bounds[2] = max(bounds[2], p[0])
        bounds[3] = max(bounds[3], p[1])
      return bounds
    elif self._type==GeometryType.MULTILINE or self._type==GeometryType.MULTIPOLYGON:
      bounds = [self._shape[0][0][0], self._shape[0][0][1], self._shape[0][0][0], self._shape[0][0][1]]
      for l in self._shape:
        for p in l:
          bounds[0] = min(bounds[0], p[0])
          bounds[1] = min(bounds[1], p[1])
          bounds[2] = max(bounds[2], p[0])
          bounds[3] = max(bounds[3], p[1])
      return bounds
  def getShape(self):
    return self._shape



# --- function definitions --------------------------------
def parseMULTIPOLY2XYlists(which):
  """! @brief Parses the given geometry assuming it's a 2D MULTIPOLYGON
    
  @return The parsed geometry as list of position lists
  """
  which = which[which.find("("):which.rfind(")")+1]
  numOpen = 0
  cpolys = []
  cpoly = []
  i = 0
  while i<len(which):
    if which[i]=='(': 
      if numOpen==2:
        e = which.find(")", i)
        polypart = which[i+1:e]
        points = polypart.split(",")
        cpoints = []
        for p in points:
          if len(p.strip())==0:
            continue
          xy = p.strip().split(" ")
          cpoints.append( [ float(xy[0]), float(xy[1]) ] )
        cpoly.append(cpoints)
        i = e
      else:
        numOpen += 1
    elif which[i]==')':
      numOpen -= 1
      if numOpen==1:
        cpolys.append(cpoly)
        #print (cpoly)
        cpoly = []
    i += 1
  return cpolys


def parsePOLY2XYlists(which, scale=1.):
  """! @brief Parses the given geometry assuming it's a 2D POLYGON
    
  @return The parsed geometry as a list of positions
  """
  which = which[9:-2]
  #print which
  points = which.split(",")
  cpoints = []
  for p in points:
    if len(p.strip())==0:
      continue
    xy = p.split(" ")
    cpoints.append( [ float(xy[0])/scale, float(xy[1])/scale ] )
  return cpoints


def parsePOINT2XY(which, scale=1.):
  """! @brief Parses the given geometry assuming it's a 2D POINT
    
  @return The parsed geometry as a position
  """
  which = which[6:-1]
  xy = which.split(" ")
  return [ float(xy[0])/scale, float(xy[1])/scale ]


def parseMULTILINE2XYlists(which, scale=1.):
  """! @brief Parses the given geometry assuming it's a 2D MULTILINE
    
  @return The parsed geometry list of position lists
  """
  which = which[16:-1]
  which = which.split("(")
  clines = []
  for line in which:
    if len(line.strip())==0:
      continue
    line = line.replace(")", "")
    line = line.split(",")
    cpoints = []
    for p in line:
      if len(p.strip())==0:
        continue
      xy = p.split(" ")
      cpoints.append( [ float(xy[0])/scale, float(xy[1])/scale ] )
    clines.append(cpoints)
  return clines

def parseMULTILINE2XYZMlists(which, scale=1.):
  """! @brief Parses the given geometry assuming it's a 2D MULTILINE
    
  @return The parsed geometry list of positions lists
  """
  which = which[20:-1]
  which = which.split("(")
  clines = []
  for line in which:
    if len(line.strip())==0:
      continue
    line = line.replace(")", "")
    line = line.split(",")
    cpoints = []
    for p in line:
      if len(p.strip())==0:
        continue
      xy = p.split(" ")
      cpoints.append( [ float(xy[0])/scale, float(xy[1])/scale ] )
    clines.append(cpoints)
  return clines


def parseLINESTRING2XYlists(which, scale=1.):
  """! @brief Parses the given geometry assuming it's a 2D LINESTRING
    
  @return The parsed geometry list of positions
  """
  which = which[11:-1]
  which = which.split(",")
  cline = []
  for p in which:
    xy = p.split(" ")
    cline.append( [ float(xy[0])/scale, float(xy[1])/scale ] )
  return cline


def wkt2geometry(wkt):
  """! @brief Parses the given WKT into a Geometry object
  
  @param wkt The WKT
  @return The Geometry representing the WKT
  """
  if wkt.startswith("POINT"):
    shape = parsePOINT2XY(wkt)
    return Geometry(GeometryType.POINT, shape)
  elif wkt.startswith("LINESTRING"):
    shape = parseLINESTRING2XYlists(wkt)
    return Geometry(GeometryType.LINESTRING, shape)
  elif wkt.startswith("MULTILINE"):
    shape = parseMULTILINE2XYlists(wkt)
    return Geometry(GeometryType.MULTILINE, shape)
  elif wkt.startswith("POLYGON"):
    shape = parsePOLY2XYlists(wkt)
    return Geometry(GeometryType.POLYGON, shape)
  elif wkt.startswith("MULTIPOLYGON"):
    shape = parseMULTIPOLY2XYlists(wkt)
    return Geometry(GeometryType.MULTIPOLYGON, shape)
  else:
    raise ValueError("Unknown geometry '%s'" % wkt)

