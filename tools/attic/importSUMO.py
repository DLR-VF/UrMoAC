#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Imports a SUMO road network

Call with
    sumo_import <HOST>;<DB>;<SCHEMA>.<TABLENAME>;<USER>;<PASSWD> <SUMO_NET>"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2019-2024, Institute of Transport Research, German Aerospace Center (DLR)"
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
import os, string, sys
import datetime
import psycopg2
from optparse import OptionParser
from xml.sax import saxutils, make_parser, handler
from osmmodes import *



SUMO2MODE = {
    "pedestrian": FOOT,
    "bus": BUS,
    "bicycle": BICYCLE,
    "passenger": PASSENGER,
    "private": PASSENGER
}    
    

storedRoads = []


def addRoad(rID, fromNode, toNode, modes, numLanes, vmax, rNodes, name, length, rGeom):
    rep = {"id":rID, "fromNode":fromNode, "toNode":toNode, "modes":modes, 
        "nodes":','.join([str(x) for x in rNodes]), "lanes":numLanes, "vmax":vmax, "name":name, "length":length, "shape":rGeom}
    storedRoads.append(rep)

def commitRoads(conn, cursor, schema, table):
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
        shape = sr["shape"].split(" ")
        nshape = []
        for ss in shape:
            s = ss.split(",")
            s[0] = float(s[0]) + 415335.88
            s[1] = float(s[1]) + 660658.02
            nshape.append("%s %s" % (s[0], s[1]))    
        nshape = ",".join(nshape)
        cursor.execute("INSERT INTO %s.%s(oid, nodefrom, nodeto, numlanes, length, vmax, mode_walk, mode_bike, mode_pt, mode_mit, modes, nodes, name, geom) VALUES('%s', %s, %s, %s, %s, %s, '%s', '%s', '%s', '%s', %s, '{%s}', '%s', ST_Transform(ST_GeomFromText('MULTILINESTRING((%s))', 32618), 4326))" % (schema, table, sr["id"], sr["fromNode"], sr["toNode"], sr["lanes"], sr["length"], sr["vmax"], mode_ped, mode_bic, mode_pt, mode_mit, sr["modes"], sr["nodes"], sr["name"], nshape))
    conn.commit()
    del storedRoads[:]


class SUMOReader(handler.ContentHandler):
    def __init__(self, schema, table, conn, cursor):
        self.schema = schema
        self.table = table
        self.fname = schema+"."+table
        self.conn = conn
        self.cursor = cursor
        self.last = None
        self.elements = []
        self.types = {}
        self.nodeIds = {}
        self.lastNodeId = 0

    def _getNodeID(self, node):
        if node in self.nodeIds:
            return self.nodeIds[node]
        ret = self.lastNodeId
        self.nodeIds[node] = ret 
        self.lastNodeId += 1
        return ret
    
    
    def _getAllowed(self, attrs, preset=None):
        if preset is None:    
            allow = set()
            allow.add(FOOT)
            allow.add(BICYCLE)
            allow.add(PASSENGER)
            allow.add(BUS)
        else:
            allow = set(preset)
        if "allow" in attrs:
            allow = set()
            for m in    attrs["allow"].split(" "):
                if m in SUMO2MODE:
                    allow.add(SUMO2MODE[m])    
        if "disallow" in attrs:
            for m in    attrs["disallow"].split(" "):
                if m in SUMO2MODE and SUMO2MODE[m] in allow:
                    allow.remove(SUMO2MODE[m])    
        return allow
    
    def startElement(self, name, attrs):
        self.elements.append(name)

        if name=="type":
            id = attrs["id"]
            vmax = attrs["speed"]
            allow = self._getAllowed(attrs)
            self.types[id] = {"vmax":vmax, "allowed":allow}

        elif name=="edge":
            self.edgeId = attrs["id"]
            if self.edgeId[0]==":": 
                self.edgeId = None
                return
            fromNode = self._getNodeID(attrs["from"])
            toNode = self._getNodeID(attrs["to"])
            shape = []
            if "shape" in attrs:
                shape = attrs["shape"]
            type = attrs["type"]
            vmax = 0
            vmax = self.types[type]["vmax"]
            allowed = self.types[type]["allowed"]
            allowed = self._getAllowed(attrs, allowed)
            length = -1
            if "length" in attrs:
                length = attrs["length"]
            self.last = {"id":attrs["id"], "from":fromNode, "to":toNode, "shape":shape, "type":type, "vmax":vmax, "allowed":allowed, "length":length, "shape":shape, "numLanes":0}

        elif name=="lane" and self.edgeId!=None:
            if "vmax" in attrs: self.last["vmax"] = max(self.last["vmax"], attrs["vmax"])
            if "length" in attrs and self.last["length"]<0:
                self.last["length"] = attrs["length"] 
            if "allowed" in attrs:
                allowed = attrs["allowed"]
                self.last["allowed"] = self.last["allowed"].union(allowed)
            if len(self.last["shape"])==0: self.last["shape"] = attrs["shape"]
            self.last["numLanes"] = self.last["numLanes"] + 1            

            
    def endElement(self, name):
        l = self.elements[-1]
        if l=="edge" and self.edgeId!=None:
            modes = 0
            for m in self.last["allowed"]:
                modes = modes | m 
            addRoad(self.last["id"], self.last["from"], self.last["to"], modes, self.last["numLanes"], self.last["vmax"], [], self.last["id"], self.last["length"], self.last["shape"])
            self.edgeId = None
            self.last = None
            if len(storedRoads)>10000:
                commitRoads(self.conn, self.cursor, self.schema, self.table)
        self.elements = self.elements[:-1]

     
    
(host, db, tableFull, user, password) = sys.argv[1].split(";")
(schema, table) = tableFull.split(".")
t1 = datetime.datetime.now()
conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor = conn.cursor()

# (re) create tables
cursor.execute("""DROP TABLE IF EXISTS %s.%s;""" % (schema, table))
conn.commit()
cursor.execute("""CREATE TABLE %s.%s (
    id serial PRIMARY KEY,
    oid text,
    nodefrom bigint, 
    nodeto bigint, 
    numlanes smallint,
    length double precision,
    vmax double precision,
    mode_walk boolean,
    mode_bike boolean,
    mode_pt boolean,
    mode_mit boolean,        
    modes bigint,
    nodes bigint[],
    name text        
);""" % (schema, table))
cursor.execute("""SELECT AddGeometryColumn('%s', '%s', 'geom', 4326, 'MULTILINESTRING', 2);""" % (schema, table))
conn.commit()


t1 = datetime.datetime.now()

print ("Parsing '%s'..." % sys.argv[2])
parser = make_parser()
r = SUMOReader(schema, table, conn, cursor)
parser.setContentHandler(r)
parser.parse(sys.argv[2])
commitRoads(conn, cursor, schema, table)


t2 = datetime.datetime.now()
dt = t2-t1
print ("In %s" % dt)
