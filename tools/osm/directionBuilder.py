#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function

import configparser

# ===========================================================================
"""Builds an road network table using an OSM-database representation.

Call with
  osmdb_buildWays <HOST>,<DB>,<SCHEMA>.<PREFIX>,<USER>,<PASSWD>"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2016-2025, Institute of Transport Research, German Aerospace Center (DLR)"
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
import os, string, sys, io
import datetime
from xml.sax import saxutils, make_parser, handler
import argparse
import psycopg2
import osmdb
from osmmodes import *




# --- class definitions -----------------------------------------------------
class Params:
    """Stores the parameter of a OSM-object and controls their usage."""
    
    def __init__(self):
        """Constructor"""       
        self._params = {}  
        self._consumed = set()
        self._errorneous = {}


    def get(self, which, defaultValue=None):
        """Returns the named parameter's value or the default value if the parameter is not known
        :param which: The name of the parameter
        :type which: str
        :param defaultValue: The default value to return
        :type defaultValue: str
        :return: The named parameter's value or the default
        :rtype: str
        """       
        return self._params[which] if which in self._params else defaultValue


    def consume(self, which):
        """Marks the named parameter as consumed
        :param which: The name of the parameter
        :type which: str
        """       
        self._consumed.add(which)


    def mark_bad(self, which):
        """Marks the named parameter as errorneous
        :param which: The name of the parameter
        :type which: str
        """       
        if which in self._params:
            self._errorneous[which] = self._params[which]
  
  
    def get_unconsumed(self, to_skip):
        """Returns the list of unconsumed parameters
        :param to_skip: The list of parameters to skip
        :type to_skip: str
        :return: The list of unconsumed parameters
        :rtype: str
        """       
        ret = {}
        for p in self._params:
            if p in self._consumed:
                continue
            if p in to_skip:
                continue
            ret[p] = self.get(p)
        return ret      



# --- function definitions --------------------------------------------------
def parse_point(which):
    """Parses and returns a single position
    :param which: The wkt-description of the position
    :type which: str
    :return: The position as x/y-tuple
    :rtype: tuple[float, float]
    :todo DANIEL: Use the method from wkt module
    """
    which = which[6:-1]
    xy = which.split(" ")
    return [ float(xy[0]), float(xy[1]) ]
  
  
  
"""The list of read roads """
storedRoads = []



def add_road(upperType, rID, fromNode, toNode, rtype, modes, numLanes, vmax, rNodes, sidewalk, cycleway, surface, lit, name, rGeom):
    """Adds the defined road to the list of read roads
    :param upperType: The upper type of the road (unused)
    :type upperType: str
    :param rID: The ID of the road
    :type rID: str
    :param fromNode: The node the road starts at
    :type fromNode: str
    :param toNode: The node the road ends at
    :type toNode: str
    :param rtype: The type of the road
    :type rtype: str
    :param modes: The modes allowed on this road
    :type modes: int
    :param numLanes: The number of lanes on this road
    :type numLanes: int
    :param vmax: The maximum velocity allowed on this road
    :type vmax: float
    :param rNodes: The geometry nodes along this road
    :type rNodes: list[int]
    :param sidewalk: The type of the sidewalk at this road
    :type sidewalk: str 
    :param cycleway: The type of the cycleway at this road
    :type cycleway: str 
    :param surface: The surface type of this road
    :type surface: str 
    :param lit: Whether this road is lit
    :type lit:  
    :param name: The name of this road
    :type name: str
    :param rGeom: The geometry of this road
    :type rGeom:  
    """
    rep = {"id":rID, "fromNode":fromNode, "toNode":toNode, "osm_type":rtype, "modes":modes, "nodes":','.join([str(x) for x in rNodes]), "lanes":numLanes, "vmax":vmax, "sidewalk":sidewalk, "cycleway":cycleway, "surface":surface, "name":name, "shape":rGeom, "lit":lit}
    storedRoads.append(rep)


def commit_roads(conn, cursor, schema, dbprefix, region, mode):
    """Commits tored roads to the given database
    :param conn: The connection to the database
    :type conn: psycopg2.extensions.connection
    :param cursor: The cursor used to write to the database
    :type cursor: psycopg2.extensions.cursor
    :param schema: The database schema to use
    :type schema: str
    :param dbprefix: The OSM database prefix to use
    :type dbprefix: str
    """
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
        cursor.execute("INSERT INTO %s.%s_%s_%s_network(oid, nodefrom, nodeto, numlanes, length, vmax, street_type, capacity, mode_walk, mode_bike, mode_pt, mode_mit, modes, nodes, sidewalk, cycleway, surface, lit, name, geom) VALUES('%s', %s, %s, %s, %s, %s, '%s', -1, '%s', '%s', '%s', '%s', %s, '{%s}', '%s', '%s', '%s', '%s', '%s', ST_GeomFromText('LINESTRING(%s)', 4326))" % (schema, dbprefix, region, mode, sr["id"], sr["fromNode"], sr["toNode"], sr["lanes"], -1, sr["vmax"], sr["osm_type"], mode_ped, mode_bic, mode_pt, mode_mit, sr["modes"], sr["nodes"], sr["sidewalk"], sr["cycleway"], sr["surface"], sr["lit"], sr["name"], sr["shape"]))
    conn.commit()
    del storedRoads[:]

def get_kvpairs(mode):
    #category = 'railways'  # later choose between rail, local rail, street
    #TODO in externes file

    if mode=='highway':
        # http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany
        return {
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
            #"highway_proposed":{"mode":ALL, "vmax":50, "lanes":1},
            #"highway_construction":{"mode":ALL, "vmax":50, "lanes":1},
            "highway_service":{"mode":ALL, "vmax":20, "lanes":1},
            "highway_track":{"mode":ALL, "vmax":20, "lanes":1},  # all only if destination (http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany)
            "highway_services":{"mode":ALL, "vmax":30, "lanes":1},
            "highway_unsurfaced":{"mode":ALL, "vmax":30, "lanes":1},

            # TODO: DANIEL we should decide which of the following ones may be used by which mode
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
            "highway_ford":{"mode":CLOSED, "vmax":10, "lanes":1}
        }

    elif mode == 'railway':
        #TODO add abandoned and deactivated rail lines
        return {
            "railway_rail":{"mode":RAIL, "vmax":300, "oneway":True, "lanes":1},
            "railway_light_rail":{"mode":RAIL, "vmax":100, "oneway":True, "lanes":1},
            "railway_preserved":{"mode":RAIL, "vmax":100, "oneway":True, "lanes":1},
            "railway_abandoned":{"mode":RAIL, "vmax":0, "oneway":True, "lanes":1},
            "railway_disused": {"mode": RAIL, "vmax": 0, "oneway": True, "lanes": 1}
        }
    elif mode == 'local_pt':
        return {
            "railway_tram": {"mode": RAIL, "vmax": 100, "oneway": True, "lanes": 1},
            "railway_subway": {"mode": RAIL, "vmax": 100, "oneway": True, "lanes": 1},
            "railway_light_rail": {"mode": RAIL, "vmax": 100, "oneway": True, "lanes": 1},
            "railway_monorail": {"mode": RAIL, "vmax": 100, "oneway": True, "lanes": 1},
            "railway_stadtbahn": {"mode": RAIL, "vmax": 100, "oneway": True, "lanes": 1},
            "railway_narrow_gauge": {"mode": RAIL, "vmax": 100, "oneway": True, "lanes": 1}
        }
    elif mode == 'healthcare':
        return {
            "amenity_clinic":{},
            "amenity_doctors":{},
            "amenity_hospital":{},
            "amenity_dentist":{},
            "amenity_pharmacy":{}
        }


def get_directional_modes(rid, defaultOneway, params, modesF):
    """Returns the list of modes allowed on this road in both directions
    :param rid: The ID of the road
    :type rid: str
    :param defaultOneway: Whether the road type's default is oneway
    :type defaultOneway: bool
    :param params: The road's parameter
    :type params: Params
    :param modesF: The forward allowed modes
    :type modesF: int
    """
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
                params.mark_bad("oneway:"+modes1[m]["mml"])
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
                params.mark_bad("oneway:"+modes1[m]["mml"])
                continue
            if accessAllows[modeAccess]:
                modesF = modesF | m
            else:
                modesF = modesF & ~m
    if oneway not in ["true", "yes", "1", "reverse", "-1", "false", "no", "0", None, ""]:
        params.mark_bad("oneway")
    params.consume("oneway")
    # check per-direction information
    for m in modes1:
        modeAccess = params.get(modes1[m]["mml"]+":forward")
        params.consume(modes1[m]["mml"]+":forward")
        if modeAccess not in ["yes", "no"]:
            params.mark_bad(modes1[m]["mml"]+":forward")
            continue
        if modeAccess=="yes":
            modesF = modesF | m
        if modeAccess=="no":
            modesF = modesF & ~m
    for m in modes1:
        modeAccess = params.get(modes1[m]["mml"]+":backward")
        params.consume(modes1[m]["mml"]+":backward")
        if modeAccess not in ["yes", "no"]:
            params.mark_bad(modes1[m]["mml"]+":backward")
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
    return modesF, modesB


def get_lane_number(defaultLanes, params):
    """Determines and returns the number of lanes of this road
    :param defaultLanes: The default lane number
    :type defaultLanes: int
    :param params: The road's parameter
    :type params: Params
    """
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
            params.mark_bad("lanes") 
    params.consume("lanes")
    return l


def get_vmax(defaultSpeed, params):
    """Determines and returns the speed allowed on this road
    :param defaultLanes: The default speed allowed on this road
    :type defaultLanes: float
    :param params: The road's parameter
    :type params: Params
    """
    maxspeed = params.get("maxspeed")
    if maxspeed==None: return defaultSpeed
    v = maxspeed.lower()
    try:
        if v.find("km/h")>=0: v = int(v[:v.find("km/h")].strip()) / 3.6
        elif v.find("kmh")>=0: v = int(v[:v.find("kmh")].strip()) / 3.6
        elif v.find("mph")>=0: v = int(v[:v.find("mph")].strip()) / 3.6 * 1.609344
        elif v=="signals": return defaultSpeed
        elif v=="none": v = 300.
        elif v=="no": v = 300.
        elif v=="walk": v = 5.
        elif v=="DE:rural": v = 100.
        elif v=="DE:urban": v = 50.
        elif v=="DE:living_street": v = 10.
        else: v = float(v)
    except:
        params.mark_bad("maxspeed") 
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
    "service":True,
}


def get_modes(rid, defaultModes, params):
    """Determines and returns the mode restrictions of this road
    :param rid: The ID of this road
    :type rid: str
    :param defaultModes: The default speed allowed on this road
    :type defaultModes: int
    :param params: The road's parameter
    :type params: Params
    """
    # check for motorroad information (http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany)
    motorroad = params.get("motorroad")
    if motorroad=="yes":
        defaultModes = defaultModes & ~SOFT
        defaultModes = defaultModes & ~MOPED
    if motorroad not in ["yes", "no"]: params.mark_bad("motorroad")
    params.consume("motorroad")
    # check for sidewalks
    sidewalk = params.get("sidewalk")
    if sidewalk in ["sidewalk", "crossing", "left", "right", "both", "yes"]:
        defaultModes = defaultModes | FOOT
    if sidewalk not in ["none", "no", "sidewalk", "crossing", "left", "right", "both", "yes", "separate"]:
        params.mark_bad("sidewalk")
    params.consume("sidewalk")
    # check for cycleways
    cycleway = params.get("cycleway")
    if cycleway in ["track", "opposite_track", "segregated", "lane", "shared_lane", "opposite_lane", "share_busway", "right", "opposite", "yes", "shared"]:
        defaultModes = defaultModes | BICYCLE
    if cycleway not in ["track", "opposite_track", "segregated", "lane", "none", "shared_lane", "opposite_lane", "crossing", "share_busway", "right", "no", "opposite", "yes", "shared"]:
        params.mark_bad("cycleway")
    params.consume("cycleway")
    # check for access restrictions
    # - global
    access = params.get("access")
    params.consume("access")
    if access!=None:
        if access not in accessAllows:
            params.mark_bad("access")
        else:  
            if not accessAllows[access]:
                defaultModes = defaultModes & ~ALL
    # - per mode
    for m in modes1:
        modeAccess = params.get(modes1[m]["mml"])
        params.consume(modes1[m]["mml"])
        if modeAccess not in accessAllows:
            params.mark_bad(modes1[m]["mml"])
            continue
        if accessAllows[modeAccess]:
            defaultModes = defaultModes | m
        else:
            defaultModes = defaultModes & ~m
    return defaultModes, cycleway, sidewalk

def modeswitch(mode):
    if mode == 'local_pt':
        return 'railway'
    else:
        return mode
               
def get_params_class(params):
    """Build a parameter class using the given parameter
    :param params: The parameter map
    :type params: dict[str, str]
    :return: A Param-class instance having the given parameter
    :rtype: Param
    """
    ret = Params()
    for p in params:
        ret._params[p[1]] = p[2]
    return ret 

def createTables(db, conn, cursor, dschema, dprefix, region, mode, append, dropprevious):
        #Könnte man noch ändern, dass Pois kein network

    if db.table_exists(dschema, dprefix+"_"+region+"_"+mode+"_network"):
        if dropprevious:
            cursor.execute("""DROP TABLE %s.%s_%s_%s_network;""" % (dschema, dprefix, region, mode))
            conn.commit()
        elif not append:
            print ("osmdb_buildWays: error: destination table already exist", file=sys.stderr)
            return 1
    if not append:
        if mode in ['highway', 'railway', 'local_pt']:
            cursor.execute("""CREATE TABLE %s.%s_%s_%s_network (
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
                sidewalk text,
                cycleway text,
                width double precision,
                surface text,
                lit text,
                name text,
                slope real    
            );""" % (dschema, dprefix, region, mode))
            cursor.execute("""SELECT AddGeometryColumn('%s', '%s_%s_%s_network', 'geom', 4326, 'LINESTRING', 2);""" % (dschema, dprefix, region, mode))
        else:
            cursor.execute("""CREATE TABLE %s.%s_%s_%s_network (
            id bigint,
            k text,
            v text,
            name text
                        );""" % (dschema, dprefix, region, mode))
            cursor.execute("""SELECT AddGeometryColumn('%s', '%s_%s_%s_network', 'geom', 4326, 'POINT', 2);""" % (dschema, dprefix, region, mode))
        conn.commit()


def determineUsedNodes(db, region, mode, verbose, kvpairs):
    #TODO is this necessary?
    # get roads and railroads with some typical classes
    seen = set()
    nodes = {}
    unrecognized = {}
    print ("Determining used nodes")
    #for upperType in ["highway", "railway"]:
    upperType = modeswitch(mode)
    IDs = db.getWayKV_withMatchingKey(upperType, region)
    # get nodes usage
    # currently in memory; this will fail for larger networks
    for i,h in enumerate(IDs):
        hID = h[0]
        if hID in seen:
            continue
        htype = h[1]+"_"+h[2]
        #if htype=="highway_proposed": htype="highway_residential"
        #if htype=="highway_construction": htype="highway_residential"
        if htype=="highway_construction":
            continue
        if htype == "highway_proposed":
            continue
        if htype not in kvpairs:
            if htype not in unrecognized:
                unrecognized[htype] = 0
            unrecognized[htype] = unrecognized[htype] + 1
        else:
            seen.add(hID)
            hDef = db.get_way(hID)
            for n in hDef[0][1]:
                if n in nodes:
                    nodes[n] = nodes[n] + 1
                else:
                    nodes[n] = 1

    if len(unrecognized)>0 and verbose:
        print ("The following types were not recognised and will be thereby dismissed:")
        for u in unrecognized:
            print (" %s (%s instances)" % (u, unrecognized[u]))

    return nodes


#def determineRoadIDs(db, region, mode):




def createStationTable(db, conn, cursor, dschema, dprefix, region, mode, append, dropprevious):
    if db.table_exists(dschema, dprefix + "_" + region + "_" + mode + "_stations"):
        if dropprevious:
            cursor.execute("""DROP TABLE %s.%s_%s_%s_stations;""" % (dschema, dprefix, region, mode))
            conn.commit()
        elif not append:
            print("osmdb_buildWays: error: destination table already exist", file=sys.stderr)
            return 1
    if not append:
        cursor.execute("""CREATE TABLE %s.%s_%s_%s_stations (
            id bigint,
            stationsname text,
            stationtype text,
            category text,
            sub_stops text[], 
            relevant_nodes text[],
            linelist text []
        );""" % (dschema, dprefix, region, mode))
        cursor.execute(  
            """SELECT AddGeometryColumn('%s', '%s_%s_%s_stations', 'geom', 4326, 'POINT', 2);""" % (dschema,
                                                                                                    dprefix, region,
                                                                                                    mode))
        conn.commit()
    if db.table_exists(dschema, dprefix + "_" + region + "_" + mode + "_stops"):
        cursor.execute("""DROP TABLE %s.%s_%s_%s_stops;""" % (dschema, dprefix, region, mode))
        conn.commit()
    cursor.execute("""CREATE TABLE %s.%s_%s_%s_stops (
        id bigint,
        k text,
        v text 
    );""" % (dschema, dprefix, region, mode))
    cursor.execute(
        """SELECT AddGeometryColumn('%s', '%s_%s_%s_stops', 'geom', 4326, 'POINT', 2);""" % (dschema, dprefix, region,
                                                                                             mode))
    conn.commit()

def fillStopsTable(db, conn, cursor, dschema, dprefix, region, mode):
    rest = db.getStops(region, mode, dschema, dprefix)
    print('Stops queried.')
    stationlist = []
    stoplist = []
    haltlist = []
    for st in rest:
        if st[2] == 'station':
            stationlist.append(st[0])
        elif st[2] == 'stop':
            stoplist.append(st[0])  # tramstops gehen hier mit rein
        else:
            haltlist.append(st[0])
        cursor.execute(
            "INSERT INTO %s.%s_%s_%s_stops(id, k, v, geom) VALUES(%s, '%s', '%s', ST_GeomFromEWKB(decode('%s', 'hex')));" % (
                dschema, dprefix, region, mode, st[0], st[1], st[2], st[3]))
    conn.commit()
    return [stationlist, stoplist, haltlist]
    #print('Stops temporarily stored in database.')

def stationLineAdder(db, conn, cursor, dschema, dprefix, region, mode, stationID):

    linelist=[]
    cursor.execute(
        """SELECT sub_stops from %s.%s_%s_%s_stations where id ='%s';""" % (dschema, dprefix, region, mode, stationID))
    stoplist=cursor.fetchall()
    if not stoplist:
        return
    for substops in stoplist[0][0]:
        cursor.execute(
        """Select rid from osm.germany20250516_member where elemid='%s';""" % (substops))
        rels = cursor.fetchall()
        for rel in rels:
            cursor.execute("""
            SELECT v FROM
            osm.germany20250516_rtag
            where id = '%s' and k = 'ref';""" % rel)
            lineslist = cursor.fetchall()
            if lineslist:
                if lineslist[0][0] not in linelist:
                    linelist.append(lineslist[0][0])
    #print('linelist')
    #print(linelist)
    cursor.execute(
        "UPDATE %s.%s_%s_%s_stations SET linelist = ARRAY%s::text[] WHERE id ='%s'"
        % (
            dschema, dprefix, region, mode, linelist, stationID))
    conn.commit()

def aggregateStops(db, conn, cursor, dschema, dprefix, region, mode, stationstophaltlist):
    seen = []
    stationcounter = 0
    stationlist = stationstophaltlist[0]
    stoplist=stationstophaltlist[1]
    haltlist=stationstophaltlist[2]
    for ssh in [stationlist, stoplist, haltlist]:
        for elem in ssh:
            if elem not in seen:
                # if ssh is not stationlist:
                #    seen.append(elem)
                mainstop = db.fetchCloseStops(elem, region, mode, dschema, dprefix)
                # TODO stationparameter über Params class?
                stationname = db.getNodeValue_withKeyAndID(elem, 'name')
                if not stationname:
                    stationname = [['error']]
                stationtype = db.getNodeValue_withKeyAndID(elem, 'station')
                if stationtype:
                    stationtype = stationtype[0][0]
                else:
                    typeindex = 0
                    possibletypes = ['light_rail', 'tram', 'subway', 'monorail', 'stadtbahn']
                    while typeindex < len(possibletypes):
                        stationtype=db.getNodeValue_withKeyAndID(elem, possibletypes[typeindex])
                        if stationtype:
                            stationtype = possibletypes[typeindex]
                            typeindex =len(possibletypes)
                        else:
                            typeindex +=1
                if not stationtype:
                    stationtype = 'indetermined, probably rail'
                if mode == 'railway':
                    if stationtype in ['subway', 'tram_stop', 'tram', 'monorail', 'stadtbahn']:
                        continue
                stationsub_stops = []
                linelist =[]
                relevant_nodes_list = []
                for i in mainstop:
                    stationsub_stops.append(i[0])
                    #nodes = db.fetchCloseNodes(i[0], region, mode, dschema, dprefix)
                    #relevant_nodes_list.append(nodes[0])
                    if i[2] != 'station':
                        seen.append(i[0])
                    #print(i[0])
                    #print('stationsub_stops')
                    #print(stationsub_stops)
                    query = """SELECT nametag.v
                            FROM(
                            SELECT rtags.v as v, rtags.k as k
                            FROM
                                (SELECT rid
                                FROM osm.germany20250516_member
                                WHERE elemid='%s') as stop
                            LEFT JOIN osm.germany20250516_rtag as rtags
                            ON stop.rid=rtags.id) as nametag
                            WHERE nametag.k = 'ref'
                    """ % (i[0])
                    #print(query)
                    cursor.execute(query)
                    lineslist = cursor.fetchall()
                    #print('lineslist')
                    #print (lineslist)
                    if lineslist:
                        #print('lineslist')
                        #print(lineslist)
                        linerlist = [item[0] for item in lineslist]
                        #print('linerlist')
                        #print(linerlist)
                        for line in range(len(linerlist)):
                            if linerlist[line] not in linelist:
                                #print('line')
                                #print(linerlist[line])
                                linelist.append(linerlist[line])


                        """for a in lineslist:
                            if lineslist[a] not in linelist:
                                print(lineslist[a])
                                #print(lineslist[0][0])
                                linelist.append(lineslist[a])"""
                #print('linelist')
                #print (linelist)


                cursor.execute(
                    "INSERT INTO %s.%s_%s_%s_stations(id, stationsname, stationtype, category, sub_stops, linelist, relevant_nodes, geom) VALUES(%s, '%s', '%s', '%s', ARRAY%s, ARRAY%s::text[], ST_GeomFromEWKB(decode('%s', 'hex')));" % (
                        dschema, dprefix, region, mode, mainstop[0][0], stationname[0][0], stationtype, mainstop[0][2],
                        stationsub_stops,  linelist,  mainstop[0][3]))
                stationcounter += 1
                if stationcounter % 1000 == 0:
                    print(str(stationcounter) + ' Stations added.')
                conn.commit()
                #stationLineAdder(db, conn, cursor, dschema, dprefix, region, mode, elem)


    cursor.execute("DROP TABLE %s.%s_%s_%s_stops;" % (dschema, dprefix, region, mode))
    conn.commit()
    return stationcounter

def htypeRailSort(db, htype, mode, kvpairs, hID):
    if htype not in kvpairs:
        htype = ''

    elif htype == 'railway_light_rail':
        if mode == 'railway':
            if db.lightrailsort(hID) == 'stadtbahn':
                htype = ''
        if mode == 'local_pt':
            if db.lightrailsort(hID) == 'stadtbahn':
                # TODO lightrail zu stadtbahn umbenennen
                htype = 'railway_stadtbahn'
                # pass
            else:
                htype = ''

    elif htype == 'railway_subway':
        if db.subwaysort(hID) == 'stadtbahn':
            htype = 'railway_stadtbahn'

    return htype



def railWayFetcher(dschema, dprefix, region, mode, cursor):
    query="SELECT id FROM osmextract.germany_final_network"
    cursor.execute(query)
    ways=cursor.fetchall()
    #print(ways)
    return ways

def leftRightQuery(leftright, id, dschema, dprefix, region, mode):

    if 'l' in leftright:
        lr=''
    elif 'r' in leftright:
        lr='-'
    #print(lr)

    query = """with baseline as (SELECT street_type, geom FROM osmextract.germany_final_network
    where id = %s),
    blcenter as (SELECT ST_PointOnSurface(geom) as geom
    FROM baseline),
    leftparallel as (select ST_OffsetCurve(geom, %s0.00020) as geom
    from baseline),
    closeleftparallel as (select ST_OffsetCurve(geom, %s0.000002) as geom
    from baseline),
    lpcenter as (SELECT ST_PointOnSurface(geom) as geom
    from leftparallel),
    clpcenter as (SELECT ST_PointOnSurface(geom) as geom
    from closeleftparallel),
    lpline as (SELECT ST_Makeline(clpcenter.geom, lpcenter.geom) as geom, 
    baseline.street_type as street_type
    from clpcenter, lpcenter, baseline),
    alllines as (SELECT street_type, geom FROM osmextract.germany_final_network
    where street_type = (Select street_type from lpline))
    SELECT DISTINCT ST_Intersects(lpline.geom, alllines.geom)
    from lpline, alllines""" % (id, lr, lr)

    return query

def dirCheck(cursor, id, dschema, dprefix, region, mode):

    query=leftRightQuery('l', id, dschema, dprefix, region, mode)
    cursor.execute(query)
    leftrightresult = cursor.fetchall()
    #vllt geht schneller wenn len(left)<2?
    if any(True in tup for tup in leftrightresult):
        left = 1
    else:
        left = 0
    query = leftRightQuery('r', id, dschema, dprefix, region, mode)
    cursor.execute(query)
    leftrightresult = cursor.fetchall()
    if any(True in tup for tup in leftrightresult):
        right = 1
    else:
        right = 0

    if left == 1 and right==0:
        dir = 'forward'
    elif left == 0 and right == 1:
        dir = 'backward'
    elif left == 1 and right == 1:
        dir = 'unclear'
    else:
        dir = 'alone'
    return dir

def saveDir(conn, cursor, id, dir, dschema, dprefix, region, mode):
    query="""
    UPDATE osmextract.germany_final_network
    SET pseudodir = '%s'
    where id = %s;""" % (dir, id)
    cursor.execute(query)
    conn.commit()


def pseudoDir(conn, cursor, dschema, dprefix, region, mode):

    cursor.execute("ALTER TABLE osmextract.germany_final_network ADD COLUMN pseudodir text" )
    conn.commit()


    network=railWayFetcher(dschema, dprefix, region, mode, cursor)
    for i in range(len(network)):
        id = network[i][0]
        result = dirCheck(cursor, id, dschema, dprefix, region, mode)
        #print(id)
        #print(result)
        saveDir(conn, cursor, id, result, dschema, dprefix, region, mode)
        if i % 10000 == 0:
            print(str(i) + ' out of ' + str(len(network)) + ' ways examined.')
    print(network)




# --- main
#The real main, as the main below mostly works as an argument handler.
def build_ways(src_db, dest_db, dropprevious, append, unconsumed_file, errorneous_file, verbose, region, mode):
    #Establishes connection
    """Main method"""
    (host, db, schema_prefix, user, password) = src_db.split(",")
    schema, prefix = schema_prefix.split(".")
    (dhost, ddb, dschema_prefix, duser, dpassword) = dest_db.split(",")
    dschema, dprefix = dschema_prefix.split(".")
    t1 = datetime.datetime.now()
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    db = osmdb.OSMDB(schema, prefix, conn, cursor)
    #ddb = osmdb.OSMDB(dschema, dprefix, conn, cursor)
    #loads save point if there is any?
    """unconsumed_output = io.open(unconsumed_file, 'w', encoding='utf8') if unconsumed_file is not None else None
    errorneous_output = io.open(errorneous_file, 'w', encoding='utf8') if errorneous_file is not None else None
    # (re) create tables
    createTables(db, conn, cursor, dschema, dprefix, region, mode, append, dropprevious)
    #gets kv pairs dependent on mode. Hard coded. Returns dict.
    kvpairs = get_kvpairs(mode)
    #For a given mode, determine used nodes filters the ways that match these kvpairs and then extracts the nodes that make up these ways
    nodes = determineUsedNodes(db, region, mode, verbose, kvpairs)



    seen = set()
    num = 0
    print ("Building roads")
    #for upperType in ["highway", "railway"]:
    #for mode 'local_pt', the upper type is changed to 'railway', while railway and highway remain the same, thus matching the key.
    upperType = modeswitch(mode)
    # build roads by splitting geometry
    #Get the IDs of the used ways. Note that we previously stored the nodes of these ways in the list nodes. IDs are a list of [id, k, v]
    IDs = db.getWayKV_withMatchingKey(upperType, region)
    for i,h in enumerate(IDs):
        hID = h[0]
        if hID in seen:
            continue
        htype = h[1]+"_"+h[2]
        #As there are some particularities in the German Railway network, some kvpairs that would be classified as light_rail
        # belong to local_pt and some to railway. Likewise, only certail german subways are subways. The sorting of these
        #special cases is handled by htypeRailSort
        if mode in ['railway', 'local_pt']:
            htype = htypeRailSort(db, htype, mode, kvpairs, hID)
            if not htype:
                continue

        if htype not in list(kvpairs.keys()):
            continue

        #else:
        seen.add(hID)
        # get and check params
        params = get_params_class(db.getWayKV_forID(hID))
        #Setting default values and handling
        defaultModes = kvpairs[htype]["mode"]
        if upperType=="highway" and params.get("railway")!=None:
            ut2 = "railway_" + params.get("railway")
            if ut2 in kvpairs:
                defaultModes = defaultModes | kvpairs[ut2]["mode"]
        if upperType=="railway" and params.get("highway")!=None:
            ut2 = "highway_" + params.get("highway")
            if ut2 in kvpairs:
                defaultModes = defaultModes | kvpairs[ut2]["mode"]
        modes, cycleway, sidewalk = get_modes(hID, defaultModes, params)
        defaultOneway = "oneway" in kvpairs[htype] and kvpairs[htype]["oneway"]
        modesF, modesB = get_directional_modes(hID, defaultOneway, params, modes) # !!! hier - nun die Richtungsabhaengigen dinge
        #Overwrite defaults if there is information
        numLanes = get_lane_number(kvpairs[htype]["lanes"], params)
        vmax = get_vmax(kvpairs[htype]["vmax"], params)
        name = params.get("name", "")
        params.consume("name")
        name = name.replace("'", "") # TODO: DANIEL patch!
        lit = params.get("lit")
        params.consume("lit")
        surface = params.get("surface")
        params.consume("surface")
        params.consume("highway")
        params.consume("railway")
        #For the way id, get the node references
        hDef = db.get_way(hID)[0]
        #Returns node id and node geom for each node in a way.
        hNodes = db.getNodes_preserveOrder(hDef[1])

        # we may have to split the road to avoid loops
        b = 0
        e = 1
        splits = [False]*len(hDef[1])
        while e<len(hDef[1]):
          if hDef[1][e] in hDef[1][b:e]:
            splits[e-1] = True
            b = e
          e = e + 1


        index = 0
        ni = 0
        hGeom = []
        nodeIDs = []
        while ni!=len(hNodes):
            n = hNodes[ni]
            p = parse_point(n[1])
            nodeIDs.append(n[0])
            hGeom.append("%s %s" % (p[0], p[1]))
            if n[0] not in nodes:
                print ("osmdb_buildWays: error: way %s has a node %s not seen before; all nodes: %s" % (hID, n[0], n), file=sys.stderr)
                return 1
            if (nodes[n[0]]>1 or splits[ni]) and ni>0:
                # store the road
                if modesF!=0:
                    add_road(upperType, "f%s#%s" % (hID, index), nodeIDs[0], nodeIDs[-1], htype, modesF, numLanes, vmax, nodeIDs, sidewalk, cycleway, surface, lit, name, ",".join(hGeom))
                    num += 1
                if modesB!=0:
                    add_road(upperType, "b%s#%s" % (hID, index), nodeIDs[-1], nodeIDs[0], htype, modesB, numLanes, vmax, reversed(nodeIDs), sidewalk, cycleway, surface, lit, name, ",".join(reversed(hGeom)))
                    num += 1
                hGeom = []
                hGeom.append("%s %s" % (p[0], p[1]))
                nodeIDs = []
                nodeIDs.append(n[0])
                index = index + 1
            ni = ni + 1
        if len(hGeom)>1:
            if modesF!=0:
                add_road(upperType, "f%s#%s" % (hID, index), nodeIDs[0], nodeIDs[-1], htype, modesF, numLanes, vmax, nodeIDs, sidewalk, cycleway, surface, lit, name, ",".join(hGeom))
                num += 1
            if modesB!=0:
                add_road(upperType, "b%s#%s" % (hID, index), nodeIDs[-1], nodeIDs[0], htype, modesB, numLanes, vmax, reversed(nodeIDs), sidewalk, cycleway, surface, lit, name, ",".join(reversed(hGeom)))
                num += 1

        unconsumed = params.get_unconsumed(["source:lit", "note:name", "postal_code", "name:ru", "created_by", "old_name", "trail_visibility", "source:maxspeed", "source"])
        if unconsumed_output is not None and len(unconsumed)>0:
            unconsumed_output.write(u"%s:%s\n" % (hID, str(unconsumed)))
        if errorneous_output is not None and len(params._errorneous)>0:
            errorneous_output.write(u"%s:%s\n" % (hID, str(params._errorneous)))

        if len(storedRoads)>10000:
            commit_roads(db._conn, db._cursor, dschema, dprefix, region, mode)
            print('Committed 10k ways.')
    commit_roads(db._conn, db._cursor, dschema, dprefix, region, mode)
    cursor.execute("UPDATE %s.%s_%s_%s_network SET length=ST_Length(geom::geography);" % (dschema, dprefix, region, mode))
    conn.commit()
    if unconsumed_output is not None:
        unconsumed_output.close()
    if errorneous_output is not None:
        errorneous_output.close()
    t2 = datetime.datetime.now()
    dt = t2-t1
    print ("Built %s ways." % num)
    print (" in %s" % dt)"""

    #TODO anpassen local_pt für bus ggf
    #kvp: (public_transport stop_position), railway tram_stop, railway station, station subway,
    if mode in ['railway', 'local_pt']:
        print('In Modes \'railway\' and  \'locaL_pt\', aggregated stations are added as a point layer.')
        # Erzeugt eine temporäre StopTable und die StationTable als solche
        #createStationTable(db, conn, cursor, dschema, dprefix, region, mode, append, dropprevious)
        #Die temporäre StopTable wird befüllt mit den Stationen, stops und halten in der region und dem modus. diese abfrage
        #dauert einen moment, spart aber beim vergleichen und eliminieren von stops sehr viel zeit
        #stationstophaltlist = fillStopsTable(db, conn, cursor, dschema, dprefix, region, mode)
        #Aus der Liste der Stationen wird eine gewählt und im umfeld von 200 metern werden alle stops und halte unter dieser aggregiert
        #alle stationen bleiben erhalten, aber die unter diesen stationen aggregierten stops und halte werden nicht mehr einzeln betrachtet
        #wenn alle stationen getestet wurden wird der prozess für stops wiederholt, die nicht bereits unter einer station aggregiert wurden
        #anschließend das selbe für halte
        #stationcount = aggregateStops(db, conn, cursor, dschema, dprefix, region, mode, stationstophaltlist)
        t3=datetime.datetime.now()
        #dt=t3-t2
        #print(str(stationcount)+' stations were added in an additional ')
        print('Adding rail directions.')
        pseudoDir(conn, cursor, dschema, dprefix, region, mode)
        t4=datetime.datetime.now()
        dt=t4-t3
        print('Assigning directions took '+str(dt)+'.')

    print('All done!')




# --- function definitions --------------------------------------------------
# -- main
def main(arguments=None):
    """Main method"""
    # parse options
    if arguments is None:
        arguments = sys.argv[1:]
    # https://stackoverflow.com/questions/3609852/which-is-the-best-way-to-allow-configuration-options-be-overridden-at-the-comman
    defaults = {}
    conf_parser = argparse.ArgumentParser(prog='osmdb_buildWays', add_help=False)
    conf_parser.add_argument("-c", "--config", metavar="FILE", help="Reads the named configuration file")
    args, remaining_argv = conf_parser.parse_known_args(arguments)
    if args.config is not None:
        if not os.path.exists(args.config):
            print ("osmdb_buildWays: error: configuration file '%s' does not exist" % str(args.config), file=sys.stderr)
            raise SystemExit(2)
        #config = configparser.ConfigParser()
        config = configparser.ConfigParser()
        config.read([args.config])
        defaults.update(dict(config.items("DEFAULT")))
    parser = argparse.ArgumentParser(prog='osmdb_buildWays', parents=[conf_parser], 
        description='Builds an road network table using an OSM-database representation', 
        epilog='(c) Copyright 2016-2025, German Aerospace Center (DLR)')
    parser.add_argument('OSMdatabase', metavar='OSM-database', help='The definition of the database to read data from;\n'
            + ' should be a string of the form <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>')
    parser.add_argument('--version', action='version', version='%(prog)s 0.8.2')
    parser.add_argument('-o', '--output', help='The definition of the table to write the network to;\n'
            + ' should be a string of the form <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>')
    parser.add_argument('-R', '--dropprevious', action='store_true', help="Delete destination tables if already existing")
    parser.add_argument('-A', '--append', action='store_true', help="Append read data to existing tables")
    parser.add_argument('--unconsumed-output', default=None, help="Writes not consumed attributes to the given file")
    parser.add_argument('--errorneous-output', default=None, help="Writes errorneous items to the given file")
    parser.add_argument("-v", "--verbose", action="store_true", help="Print what is being done")
    parser.add_argument('-e','--region', help='Defines the region (state or NUTS3) in which the system is build. Defaults to \'germany\' as a whole.')
    parser.add_argument('-m', '--mode', help='Defines the mode of transportation (\'railway\', \'local_pt\' or \'street\').')
    parser.set_defaults(**defaults)
    args = parser.parse_args(remaining_argv)

    # check and parse command line parameter and input files
    errors = []
    # - input db
    if len(args.OSMdatabase.split(","))!=5:
        errors.append("Missing values in OSM database definition;\n must be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
    elif args.OSMdatabase.split(",")[2].count(".")!=1:
        errors.append("The second field of the OSM database definition must have the format <SCHEMA>.<TABLE_PREFIX>")
    gegenden = ['badenwuerttemberg', 'bayern', 'berlin', 'brandenburg', 'bremen', 'hamburg', 'hessen', 'mecklenburgvorpommern',
                'niedersachsen', 'nordrheinwestfalen', 'rheinlandpfalz', 'saarland', 'sachsen', 'sachsenanhalt', 'schleswigholstein', 'thueringen']
    if args.region is None:
        args.region = 'germany'
    elif args.region == 'germany':

        pass
    elif args.region not in gegenden:
        if "nuts3_" in args.region:
            print('Bei Auswahl auf Kreisebene ist darauf zu achten, dass ggf. die Bezeichung in der Datenbank angepasst werden muss (lowercase, keine leer oder sonderzeichen).')
        else:
            gegendstring = ''
            for g in gegenden:
                gegendstring = gegendstring + ', ' + g
            errors.append(
                "region chosen not available;\n must be \'germany\', in: " + gegendstring +" or follow the pattern \'nuts3_<Kreisname in lowercase ohne Leer- oder Sonderzeichen>\'")

    if args.mode is None:
        args.mode = 'railway'
    elif args.mode not in ['highway', 'railway', 'local_pt']:
        errors.append(
            "Mode must be \'railway\',\'local_pt\' or \'highway\'")
    # - output db
    if args.output is not None:
        output = args.output
        if len(output.split(","))!=5:
            errors.append("Missing values in target database definition;\n must be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
        elif output.split(",")[2].count(".")!=1:
            errors.append("The second field of the target database definition must have the format <SCHEMA>.<TABLE_PREFIX>")
    else:
        # checked before
        output = args.OSMdatabase.split(",")
        output[2] = output[2] + "_"+args.region+"_"+args.mode+"_network"
        output = ",".join(output)
    # - report
    if len(errors)!=0:
        parser.print_usage(sys.stderr)
        for e in errors:
            print ("osmdb_buildWays: error: %s" % e, file=sys.stderr)
        print ("osmdb_buildWays: quitting on error.", file=sys.stderr)
        return 1


    # build
    if args.mode in ['highway', 'railway', 'local_pt']:
        return build_ways(args.OSMdatabase, output, args.dropprevious, args.append, args.unconsumed_output, args.errorneous_output, args.verbose, args.region, args.mode)
    else:
        pass
        #return build_pois(args.OSMdatabase, output, args.dropprevious, args.append, args.unconsumed_output, args.errorneous_output, args.verbose, args.region, args.mode)



# -- main check
if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))


"""SELECT loc.id as id, loc.k as k, loc.v as v, loc.geom as geom
FROM 
	(Select tags.id as id, tags.k as k, tags.v as v, node.pos as geom
	FROM 
		(SELECT id, k, v
		FROM osm.germany20250516_ntag
		where k = 'railway' and (v = 'station' or v = 'halt' or v = 'stop')) as tags
	LEFT JOIN osm.germany20250516_node as node
	ON tags.id = node.id) as loc
INNER JOIN osmextract.test_hamburg_railway_network as network
on ST_DWithin(loc.geom, network.geom, 0.0002)"""