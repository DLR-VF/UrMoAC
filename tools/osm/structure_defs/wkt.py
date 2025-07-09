#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Defines geometry objects and parses WKT."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2022-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.2"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Production"
# ===========================================================================
# - https://github.com/DLR-VF/UrMoAC
# - https://www.dlr.de/vf
# ===========================================================================


# --- imports ---------------------------------------------------------------
from enum import IntEnum


# --- enum definitions ------------------------------------------------------
class GeometryType(IntEnum):
    """An enumeration of known geometry types"""
    POINT = 0
    LINESTRING = 1
    MULTILINESTRING = 2
    POLYGON = 3
    MULTIPOLYGON = 4
    GEOMETRYCOLLECTION = 5


# --- method definitions ----------------------------------------------------
def patchify(polys, **kwargs):
    """Returns a matplotlib patch representing the polygon with holes.

    polys is an iterable (i.e list) of polygons, each polygon is a numpy array
    of shape (2, N), where N is the number of points in each polygon. The first
    polygon is assumed to be the exterior polygon and the rest are holes. The
    first and last points of each polygon may or may not be the same.

    This is inspired by
    https://sgillies.net/2010/04/06/painting-punctured-polygons-with-matplotlib.html
    and
    https://stackoverflow.com/questions/8919719/how-to-plot-a-complex-polygon

    Example usage:
    ext = np.array([[-4, 4, 4, -4, -4], [-4, -4, 4, 4, -4]])
    t = -np.linspace(0, 2 * np.pi)
    hole1 = np.array([2 + 0.4 * np.cos(t), 2 + np.sin(t)])
    hole2 = np.array([np.cos(t) * (1 + 0.2 * np.cos(4 * t + 1)),
                      np.sin(t) * (1 + 0.2 * np.cos(4 * t))])
    hole2 = np.array([-2 + np.cos(t) * (1 + 0.2 * np.cos(4 * t)),
                      1 + np.sin(t) * (1 + 0.2 * np.cos(4 * t))])
    hole3 = np.array([np.cos(t) * (1 + 0.5 * np.cos(4 * t)),
                      -2 + np.sin(t)])
    holes = [ext, hole1, hole2, hole3]
    patch = patchify([ext, hole1, hole2, hole3])
    ax = plt.gca()
    ax.add_patch(patch)
    ax.set_xlim([-6, 6])
    ax.set_ylim([-6, 6])
    """
    import numpy as np

    def reorder(poly, cw=True):
        """Reorders the polygon to run clockwise or counter-clockwise
        according to the value of cw. It calculates whether a polygon is
        cw or ccw by summing (x2-x1)*(y2+y1) for all edges of the polygon,
        see https://stackoverflow.com/a/1165943/898213.
        """
        # Close polygon if not closed
        if not np.allclose(poly[:, 0], poly[:, -1]):
            poly = np.c_[poly, poly[:, 0]]
        direction = ((poly[0] - np.roll(poly[0], 1)) * (poly[1] + np.roll(poly[1], 1))).sum() < 0
        if direction == cw:
            return poly
        else:
            return poly[::-1]

    def ring_coding(n):
        """Returns a list of len(n) of this format:
        [MOVETO, LINETO, LINETO, ..., LINETO, LINETO CLOSEPOLY]
        """
        codes = [Path.LINETO] * n
        codes[0] = Path.MOVETO
        codes[-1] = Path.CLOSEPOLY
        return codes

    ccw = [True] + ([False] * (len(polys) - 1))
    polys = [reorder(poly, c) for poly, c in zip(polys, ccw)]
    codes = np.concatenate([ring_coding(p.shape[1]) for p in polys])
    vertices = np.concatenate(polys, axis=1)
    return PathPatch(Path(vertices.T, codes), kwargs)


def encode_complex_polygon(polys, close):
    import matplotlib.path
    s = []
    c = []
    for poly in polys:
        s.extend(poly)
        c.append(matplotlib.path.Path.MOVETO)
        c.extend([matplotlib.path.Path.LINETO]*(len(poly)-1))
        if close:
            s.append([-1, -1])
            c.append(matplotlib.path.Path.CLOSEPOLY) # 79: CLOSEPOLY
    return s, c



# --- class definitions -----------------------------------------------------
class Geometry:
    """An abstract geometry"""

    def __init__(self, type, shape):
        """ Constructor
        
        @param type The type of the geometry
        @param shape The geometry representation
        """
        self._type = type
        self._shape = shape

    
    def artist(self):
        """Returns a matplotlib artist that represents this geometry"""
        raise ValueError("abstract Geometry type") # pragma: no cover


    def bounds(self):
        """Returns the bounds of this geometry"""
        raise ValueError("abstract Geometry type") # pragma: no cover


    def shape(self):
        """Returns the shape of this geometry"""
        return self._shape



class Point(Geometry):
    """A point"""

    def __init__(self, shape):
        """ Constructor
        
        @param shape The geometry representation
        """
        Geometry.__init__(self, GeometryType.POINT, shape)


    def artist(self, **kwargs):
        """Returns a matplotlib artist that represents this geometry"""
        import matplotlib.patches
        return matplotlib.patches.Circle(self._shape, **kwargs)


    def bounds(self):
        """Returns the bounds of this geometry"""
        return [self._shape[0], self._shape[1], self._shape[0], self._shape[1]]



class LineString(Geometry):
    """A linestring"""

    def __init__(self, shape):
        """ Constructor
        
        @param shape The geometry representation
        """
        Geometry.__init__(self, GeometryType.LINESTRING, shape)


    def artist(self, **kwargs):
        """Returns a matplotlib artist that represents this geometry"""
        import matplotlib.patches
        import matplotlib.path
        path = matplotlib.path.Path(self._shape, closed=False)
        return matplotlib.patches.PathPatch(path, fill=False, **kwargs)


    def bounds(self):
        """Returns the bounds of this geometry"""
        bounds = [self._shape[0][0], self._shape[0][1], self._shape[0][0], self._shape[0][1]]
        for p in self._shape:
            bounds[0] = min(bounds[0], p[0])
            bounds[1] = min(bounds[1], p[1])
            bounds[2] = max(bounds[2], p[0])
            bounds[3] = max(bounds[3], p[1])
        return bounds


    
    

class Polygon(Geometry):
    """A polygon"""

    def __init__(self, shape):
        """ Constructor
        
        @param shape The geometry representation
        """
        Geometry.__init__(self, GeometryType.POLYGON, shape)


    def artist(self, **kwargs):
        """Returns a matplotlib artist that represents this geometry"""
        import matplotlib.patches
        import matplotlib.path
        s, c = encode_complex_polygon(self._shape, True)
        path = matplotlib.path.Path(s, c, closed=True)
        return matplotlib.patches.PathPatch(path, **kwargs)


    def bounds(self):
        """Returns the bounds of this geometry"""
        bounds = [self._shape[0][0][0], self._shape[0][0][1], self._shape[0][0][0], self._shape[0][0][1]]
        for poly in self._shape:
            for p in poly:
                bounds[0] = min(bounds[0], p[0])
                bounds[1] = min(bounds[1], p[1])
                bounds[2] = max(bounds[2], p[0])
                bounds[3] = max(bounds[3], p[1])
        return bounds


    def wkt(self):
        d = []
        for poly in self._shape:
            npoly = ["%s %s" % (p[0], p[1]) for p in poly]
            d.append("(" + ",".join(npoly) + ")")
        d = ",".join(d)
        return "'POLYGON(" + d + ")'"


    


class MultiLineString(Geometry):
    """A multilinestring"""

    def __init__(self, shape):
        """ Constructor
        
        @param shape The geometry representation
        """
        Geometry.__init__(self, GeometryType.MULTILINESTRING, shape)


    def artist(self, **kwargs):
        """Returns a matplotlib artist that represents this geometry"""
        import matplotlib.patches
        import matplotlib.path
        s, c = encode_complex_polygon(self._shape, False)
        path = matplotlib.path.Path(s, closed=False)
        return matplotlib.patches.PathPatch(path, fill=False, **kwargs)


    def bounds(self):
        """Returns the bounds of this geometry"""
        bounds = [self._shape[0][0][0], self._shape[0][0][1], self._shape[0][0][0], self._shape[0][0][1]]
        for line in self._shape:
            for p in line:
                bounds[0] = min(bounds[0], p[0])
                bounds[1] = min(bounds[1], p[1])
                bounds[2] = max(bounds[2], p[0])
                bounds[3] = max(bounds[3], p[1])        
        return bounds

    


class MultiPolygon(Geometry):
    """A multipolygon"""

    def __init__(self, shape):
        """ Constructor
        
        @param shape The geometry representation
        """
        Geometry.__init__(self, GeometryType.MULTIPOLYGON, shape)


    def artist(self, **kwargs):
        """Returns a matplotlib artist that represents this geometry"""
        import matplotlib.patches
        import matplotlib.path
        ss = []
        cs = []
        for poly in self._shape:
            s, c = encode_complex_polygon(poly, True)
            ss.extend(s)
            cs.extend(c)
        path = matplotlib.path.Path(ss, cs)
        return matplotlib.patches.PathPatch(path, **kwargs)


    def bounds(self):
        """Returns the bounds of this geometry"""
        bounds = None
        for cpoly in self._shape:
            for poly in cpoly:
                for p in poly:
                    bounds = [ p[0], p[1], p[0], p[1] ]
                    break
        for cpoly in self._shape:
            for poly in cpoly:
                for p in poly:
                    bounds[0] = min(bounds[0], p[0])
                    bounds[1] = min(bounds[1], p[1])
                    bounds[2] = max(bounds[2], p[0])
                    bounds[3] = max(bounds[3], p[1])        
        return bounds

    
# --- function definitions --------------------------------------------------
def parse_POINT2D(which):
    """Parses the given geometry assuming it's a 2D POINT
    
    @return The parsed geometry as a position
    """
    if which.find("EMPTY")>0:
        return None
    which = which[which.find("(")+1:which.rfind(")")]
    xy = which.split(" ")
    return [ float(xy[0]), float(xy[1]) ]


def parse_LINESTRING2D(which):
    """Parses the given geometry assuming it's a 2D LINESTRING
    
    @return The parsed geometry list of positions
    """
    if which.find("EMPTY")>0:
        return None
    which = which[which.find("(")+1:which.rfind(")")]
    which = which.split(",")
    cline = []
    for p in which:
        xy = p.strip().split(" ")
        cline.append( [ float(xy[0]), float(xy[1]) ] )
    return cline


def parse_MULTILINESTRING2D(which):
    """Parses the given geometry assuming it's a 2D MULTILINESTRING
    
    @return The parsed geometry list of position lists
    """
    if which.find("EMPTY")>0:
        return None
    which = which[which.find("("):which.rfind(")")+1]
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
            xy = p.strip().split(" ")
            cpoints.append( [ float(xy[0]), float(xy[1]) ] )
        clines.append(cpoints)
    return clines


def parse_POLYGON2D(which):
    """Parses the given geometry assuming it's a 2D POLYGON
    
    @return The parsed geometry as a list of positions
    """
    if which.find("EMPTY")>0:
        return None
    which = which[which.find("("):which.rfind(")")+1]
    numOpen = 0
    cpoly = []
    i = 0
    while i<len(which):
        if which[i]=='(': 
            if numOpen==1:
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
        i += 1
    return cpoly


def parse_MULTIPOLYGON2D(which):
    """Parses the given geometry assuming it's a 2D MULTIPOLYGON
    
    @return The parsed geometry as list of position lists
    """
    if which.find("EMPTY")>0:
        return None
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
                cpoly = []
        i += 1
    return cpolys



def wkt2geometry(wkt):
    """Parses the given WKT into a Geometry object
  
    @param wkt The WKT
    @return The Geometry representing the WKT
    """
    wkt = wkt.upper()
    if wkt.startswith("POINT"):
        shape = parse_POINT2D(wkt)
        if shape==None: return None
        return Point(shape)
    elif wkt.startswith("LINESTRING"):
        shape = parse_LINESTRING2D(wkt)
        if shape==None: return None
        return LineString(shape)
    elif wkt.startswith("MULTILINESTRING"):
        shape = parse_MULTILINESTRING2D(wkt)
        if shape==None: return None
        return MultiLineString(shape)
    elif wkt.startswith("POLYGON"):
        shape = parse_POLYGON2D(wkt)
        if shape==None: return None
        return Polygon(shape)
    elif wkt.startswith("MULTIPOLYGON"):
        shape = parse_MULTIPOLYGON2D(wkt)
        if shape==None: return None
        return MultiPolygon(shape)
    else:
        raise ValueError("Unknown geometry '%s'" % wkt)


def wkt2lists(wkt):
    """Parses the given WKT into a Geometry object
  
    @param wkt The WKT
    @return Lists of different dimensions in dependence of the WKT type
    """
    wkt = wkt.upper()
    if wkt.startswith("POINT"):
        return parse_POINT2D(wkt)
    elif wkt.startswith("LINESTRING"):
        return parse_LINESTRING2D(wkt)
    elif wkt.startswith("MULTILINESTRING"):
        return parse_MULTILINESTRING2D(wkt)
    elif wkt.startswith("POLYGON"):
        return parse_POLYGON2D(wkt)
    elif wkt.startswith("MULTIPOLYGON"):
        return parse_MULTIPOLYGON2D(wkt)
    else:
        raise ValueError("Unknown geometry '%s'" % wkt)


