#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""A network representation."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2024, Institute of Transport Research, German Aerospace Center (DLR)"
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


# --- class definitions -----------------------------------------------------
class Edge:
    def __init__(self, id, geom, data=None):
        self._id = id
        self._geom = geom
        self._data = data
    def bounds(self):
        return self._geom.bounds()
    def within(self, bounds):
        within = False
        for p in self._geom._shape[0]:
            if p[0]<bounds[0] or p[0]>bounds[2]:
                continue
            if p[1]<bounds[1] or p[1]>bounds[3]:
                continue
            within = True
            break            
        return within
    def geometry(self):
        return self._geom
    def artist(self, **kwargs):
        return self._geom.artist(**kwargs)
    def data(self):
        return self._data
    def reproject(self, transformer):
        shape = []
        for p in self._geom._shape[0]:
            shape.append(transformer.transform(p[0], p[1]))
        self._geom._shape[0] = shape


class Net:
    def __init__(self):
        self._bounds = None
        self._edges = []
    def update_bounds(self, e):
        b = e.bounds()
        if not self._bounds:
            self._bounds = b
        else:
            self._bounds[0] = min(self._bounds[0], b[0])
            self._bounds[1] = min(self._bounds[1], b[1])
            self._bounds[2] = max(self._bounds[2], b[2])
            self._bounds[3] = max(self._bounds[3], b[3])
    def addEdge(self, e):
        self._edges.append(e)
        self.update_bounds(e)
    def bounds(self):
        return self._bounds
    def edges(self):
        return self._edges
    def prune(self, bounds):
        edges = []
        for e in self._edges:
            if not e.isWith(bounds):
                continue
            edges.append(e)
        self._edges = edges
        self._bounds = bounds
    def reproject(self, fromEPSG, toEPSG):
        from pyproj import Transformer
        transformer = Transformer.from_crs(fromEPSG, toEPSG)
        self._bounds = None
        for e in self._edges:
            e.reproject(transformer)
            self.update_bounds(e)
    def getWidthMap(self, defaultWidth=1.):
        ret = {}
        for e in self._edges:
            ret[e.getData()["street_type"]] = defaultWidth
        return ret
    def dumpPickle(self, file, reportDuration=False):
        """! @brief Saves the container to a pickle
        
        Currently, only the trip table (_id2trips) is saved.
        
        @param file The file to save the pickle to
        @param reportDuration If True, the duration will be printed
        """
        import pickle
        import datetime
        t1 = datetime.datetime.now()
        fd = open(file, "wb")
        pickle.dump(self, fd)
        fd.close()
        t2 = datetime.datetime.now()
        if reportDuration: print(t2 - t1)
    def loadPickle(file, reportDuration=False):
        """! @brief Loads the container from a pickle

        The loaded trips are stored in _id2trips.
        The boundary is reset to None.

        @param file The file to read the pickle from
        @param reportDuration If True, the duration will be printed
        """
        import pickle
        import datetime
        t1 = datetime.datetime.now()
        fd = open(file, "rb")
        network = pickle.load(fd)
        fd.close()
        t2 = datetime.datetime.now()
        if reportDuration: print(t2 - t1)
        return network
    def buildRTree(self):
        import rtree    # noqa
        self._rtree = rtree.index.Index()
        self._rtree.interleaved = True
        for ei,edge in enumerate(self._edges):
            self._rtree.add(ei, edge.getBounds())
        return self._rtree


# --- function definitions ----------------------------------------------------
def _loadNetFromDB(d, proj, stringData=[], vmaxS="vmax"):
    import psycopg2, wkt
    print (d)
    dbd = d.split(";")
    conn = psycopg2.connect("host='%s' dbname='%s' user='%s' password='%s'" % (dbd[0], dbd[1], dbd[3], dbd[4]))
    cursor = conn.cursor()
    add = ""
    if stringData:
        add = "," + ",".join(stringData)
    cursor.execute("SELECT oid,ST_AsText(ST_TRANSFORM(geom, %s))%s FROM %s;" % (proj, add, dbd[2]))
    net = Net()
    for r in cursor.fetchall():
        data = {}
        for i,s in enumerate(stringData):
            data[s] = r[2+i]
        e = Edge(r[0], wkt.wkt2geometry(r[1]), data)
        net.addEdge(e)
    return net

def loadNet(d, proj, stringData=[], vmaxS="vmax"):
    if d.startswith("postgresql:"):
        return _loadNetFromDB(d[11:], proj, stringData)
    elif d.endswith("wkt"):
        return _loadNetFromWKT(d, proj, stringData)
    raise ValueError("Unknown source '%s'" % d)


def plotNet(ax, net, color="#000000", alpha=1, widthmap = {
        "highway_primary": .5,
        "highway_primary_link": .5,
        "highway_secondary": .4,
        "highway_secondary_link": .4,
        "highway_motorway": .2,
        "highway_motorway_link": .2,
        "highway_tertiary": .2,
        "highway_tertiary_link": .2,
        "highway_residential": .1,
        "highway_residential_link": .1
    }, width_scale=1., zorder=1000):
    import matplotlib.pyplot as plt
    from matplotlib.collections import LineCollection
    
    edges = net.edges()
    artists = []
    for e in edges:
        st = e.data()["street_type"]
        if st not in widthmap:
                continue
        ax.add_artist(e.artist(color=color, lw=widthmap[st]*width_scale, zorder=zorder))
        

    
