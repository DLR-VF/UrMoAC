#!/usr/bin/env python
# =========================================================
# osmdb_BuildWays.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Imports an OSM-file into the database
# Call with
#  osmdb_buildWays <HOST>;<DB>;<SCHEMA>.<PREFIX>;<USER>;<PASSWD>
# =========================================================


# --- imported modules ------------------------------------
import os, string, sys, io
import datetime
from xml.sax import saxutils, make_parser, handler
import psycopg2, osmdb
from osmmodes import *



# --- class definitions -----------------------------------
class Params:
    """ @class Params
    @brief Stores the parameter of a OSM-object and controls their usage
    """
    
    def __init__(self):
        """ @brief Constructor
        @param self The class instance
        """       
        self.params = {}  
        self.consumed = set()
        self.errorneous = {}  
  
  
    def get(self, which, defaultValue=None):
        """ @brief Returns the named parameter's value or the default value if the parameter is not known
        @param self The class instance
        @param which The name of the parameter
        @param defaultValue The default value to return
        """       
        return self.params[which] if which in self.params else defaultValue


    def consume(self, which):
        """ @brief Marks the named parameter as consumed
        @param self The class instance
        @param which The name of the parameter
        """       
        self.consumed.add(which)


    def markBad(self, which):
        """ @brief Marks the named parameter as errorneous
        @param self The class instance
        @param which The name of the parameter
        """       
        if which in self.params:
            self.errorneous[which] = self.params[which]
  
  
    def getUnconsumed(self, toSkip):
        """ @brief Returns the list of unconsumed parameters
        @param self The class instance
        @param toSkip List of parameters to skip
        @return The list of unconsumed parameters
        """       
        ret = {}
        for p in self.params:
            if p in self.consumed:
                continue
            if p in toSkip:
                continue
            ret[p] = self.get(p)
        return ret      



# --- method definitions ----------------------------------
def parsePOINT2XY(which, scale=1.):
    """ @brief Parses and returns a single position
    @param which The wkt-description of the position
    @param scale A scaling parameter (unused)
    @return The position as x/y-tuple
    @todo remove the scale parameter
    @todo Use the method from wkt module
    """
    which = which[6:-1]
    xy = which.split(" ")
    return [ float(xy[0])/scale, float(xy[1])/scale ]
  
  
  
""" @brief The list of read roads """
storedRoads = []



def addRoad(upperType, rID, fromNode, toNode, rtype, modes, numLanes, vmax, rNodes, sidewalk, cycleway, surface, lit, name, rGeom):
    """ @brief Adds the defined road to the list of read roads
    @param upperType The upper type of the road (unused)
    @param rID The ID of the road
    @param fromNode The node the road starts at
    @param toNode The node the road ends at
    @param rtype The type of the road
    @param modes The modes allowed on this road
    @param numLanes The number of lanes on this road
    @param vmax The maximum velocity allowed on this road
    @param rNodes The geometry nodes along this road
    @param sidewalk The type of the sidewalk at this road
    @param cycleway The type of the cycleway at this road
    @param surface The surface type of this road
    @param lit Whether this road is lit
    @param name The name of this road
    @param rGeom The geometry of this road
    """
    rep = {"id":rID, "fromNode":fromNode, "toNode":toNode, "osm_type":rtype, "modes":modes, "nodes":','.join([str(x) for x in rNodes]), "lanes":numLanes, "vmax":vmax, "sidewalk":sidewalk, "cycleway":cycleway, "surface":surface, "name":name, "shape":rGeom, "lit":lit}
    storedRoads.append(rep)


def commitRoads(conn, cursor, schema, dbprefix):
    """ @brief Commits tored roads to the given database
    @param conn The connection to the database
    @param cursor The cursor to use for storing the roads
    @param schema The schema of the database
    @param dbprefix The prefix of the table
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
        cursor.execute("INSERT INTO %s.%s_network(oid, nodefrom, nodeto, numlanes, length, vmax, street_type, capacity, mode_walk, mode_bike, mode_pt, mode_mit, modes, nodes, sidewalk, cycleway, surface, lit, name, the_geom) VALUES('%s', %s, %s, %s, %s, %s, '%s', -1, '%s', '%s', '%s', '%s', %s, '{%s}', '%s', '%s', '%s', '%s', '%s', ST_GeomFromText('MULTILINESTRING((%s))', 4326))" % (schema, dbprefix, sr["id"], sr["fromNode"], sr["toNode"], sr["lanes"], -1, sr["vmax"], sr["osm_type"], mode_ped, mode_bic, mode_pt, mode_mit, sr["modes"], sr["nodes"], sr["sidewalk"], sr["cycleway"], sr["surface"], sr["lit"], sr["name"], sr["shape"]))
    conn.commit()
    del storedRoads[:]


                                   
                                   
# http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Access-Restrictions#Germany
highways = {
    """ @brief Default road / rail values """
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
    """ @brief Returns the list of modes allowed on this road in both directions
    @param rid The ID of the road
    @param defaultOneway Whether oneway traffic is the default
    @param param The road's parameter
    @param modesF The forward allowed modes
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
    return modesF, modesB


def getLanes(defaultLanes, params):
    """ @brief Determines and returns the number of lanes of this road
    @param defaultLanes The default lane number
    @param params The parameter of this road
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
            params.markBad("lanes") 
    params.consume("lanes")
    return l


def getVMax(defaultSpeed, params):
    """ @brief Determines and returns the speed allowed on this road
    @param defaultSpeed The default speed allowed on this road
    @param params The parameter of this road
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
        params.markBad("maxspeed") 
        v = defaultSpeed
    params.consume("maxspeed")
    return v



accessAllows = {
    """ @brief A mapping of mode restrictions"""
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


def getModes(rid, defaultModes, params):
    """ @brief Determines and returns the mode restrictions of this road
    @param rid The ID of this road
    @param defaultModes The modes allowed per default
    @param params The parameter of this road
    """
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
    """ @brief Build a parameter class using the given parameter
    @param param The parameter map
    @return A Param-class instance having the given parameter
    """
    ret = Params()
    for p in params:
        ret.params[p[1]] = p[2]
    return ret 
  


# --- main method -----------------------------------------
def main(argv):
    """ @brief Main method
    """
    (host, db, tableFull, user, password) = sys.argv[1].split(";")
    (schema, prefix) = tableFull.split(".")
    t1 = datetime.datetime.now()
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    db = osmdb.OSMDB(schema, prefix, conn, cursor)
    fdo1 = io.open("%s_unconsumed.txt" % prefix,'w',encoding='utf8')
    fdo2 = io.open("%s_errorneous.txt" % prefix,'w',encoding='utf8')
    # (re) create tables
    if db.tableExists(schema, prefix+"_network"):
        cursor.execute("""DROP TABLE %s.%s_network;""" % (schema, prefix))
        conn.commit()
    cursor.execute("""CREATE TABLE %s.%s_network (
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
    );""" % (schema, prefix))
    cursor.execute("""SELECT AddGeometryColumn('%s', '%s_network', 'the_geom', 4326, 'MULTILINESTRING', 2);""" % (schema, prefix))
    conn.commit()

    # get roads and railroads with some typical classes
    seen = set()
    nodes = {}
    unrecognized = {}
    print ("Determining used nodes")
    for upperType in ["highway", "railway"]:
        IDs = db.getWayKV_withMatchingKey(upperType)
        # get nodes usage
        # currently in memory; this will fail for larger networks
        for i,h in enumerate(IDs):
            hID = h[0]
            if hID in seen:
                continue
            htype = h[1]+"_"+h[2]
            #if htype=="highway_proposed": htype="highway_residential"
            #if htype=="highway_construction": htype="highway_residential"
            if htype not in highways:
                if htype not in unrecognized:
                    unrecognized[htype] = 0
                unrecognized[htype] = unrecognized[htype] + 1
            else:
                seen.add(hID)
                hDef = db.getWay(hID)
                for n in hDef[0][1]:
                    if n in nodes:
                        nodes[n] = nodes[n] + 1
                    else:
                        nodes[n] = 1 

    if len(unrecognized)>0:
        print ("The following types were not recognised and will be thereby dismissed:")
        for u in unrecognized:
            print (" %s (%s instances)" % (u, unrecognized[u]))

    seen = set()
    num = 0
    print ("Building roads")
    for upperType in ["highway", "railway"]:
        # build roads by splitting geometry
        IDs = db.getWayKV_withMatchingKey(upperType)
        for i,h in enumerate(IDs):
            hID = h[0]
            if hID in seen:
                continue
            htype = h[1]+"_"+h[2]
            #if htype=="highway_proposed": htype="highway_residential"
            #if htype=="highway_construction": htype="highway_residential"
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
      
                numLanes = getLanes(highways[htype]["lanes"], params)
                vmax = getVMax(highways[htype]["vmax"], params)
                name = params.get("name", "")
                params.consume("name")
                name = name.replace("'", "") # TODO: patch!
                lit = params.get("lit")
                params.consume("lit")
                surface = params.get("surface")
                params.consume("surface")

                params.consume("highway")
                params.consume("railway")
                #
                hDef = db.getWay(hID)
                hNodes = db.getNodes_preserveOrder(hDef[0][1])
                index = 0
                ni = 0
                hGeom = []
                nodeIDs = []

                while ni!=len(hNodes):
                    n = hNodes[ni]
                    p = parsePOINT2XY(n[1])
                    nodeIDs.append(n[0])
                    hGeom.append("%s %s" % (p[0], p[1]))
                    if n[0] not in nodes:
                        print ("Critical error: way %s has a node %s not seen before; all nodes: %s" % (hID, n[0], n))
                    if nodes[n[0]]>1 and ni>0:
                        # store the road
                        if modesF!=0:
                            addRoad(upperType, "f%s#%s" % (hID, index), nodeIDs[0], nodeIDs[-1], htype, modesF, numLanes, vmax, nodeIDs, sidewalk, cycleway, surface, lit, name, ",".join(hGeom))
                            num += 1
                        if modesB!=0:
                            addRoad(upperType, "b%s#%s" % (hID, index), nodeIDs[-1], nodeIDs[0], htype, modesB, numLanes, vmax, reversed(nodeIDs), sidewalk, cycleway, surface, lit, name, ",".join(reversed(hGeom)))
                            num += 1
                        hGeom = []
                        hGeom.append("%s %s" % (p[0], p[1]))
                        nodeIDs = []
                        nodeIDs.append(n[0])
                        index = index + 1
                    ni = ni + 1
                if len(hGeom)>1: 
                    if modesF!=0:     
                        addRoad(upperType, "f%s#%s" % (hID, index), nodeIDs[0], nodeIDs[-1], htype, modesF, numLanes, vmax, nodeIDs, sidewalk, cycleway, surface, lit, name, ",".join(hGeom))
                        num += 1
                    if modesB!=0:     
                        addRoad(upperType, "b%s#%s" % (hID, index), nodeIDs[-1], nodeIDs[0], htype, modesB, numLanes, vmax, reversed(nodeIDs), sidewalk, cycleway, surface, lit, name, ",".join(reversed(hGeom)))
                        num += 1

                unconsumed = params.getUnconsumed(["source:lit", "note:name", "postal_code", "name:ru", "created_by", "old_name", "trail_visibility", "source:maxspeed", "source"])
                if len(unconsumed)>0: 
                    fdo1.write(u"%s:%s\n" % (hID, str(unconsumed)))
                if len(params.errorneous)>0:
                    fdo2.write(u"%s:%s\n" % (hID, str(params.errorneous)))

            if len(storedRoads)>10000:
                commitRoads(db.conn, db.cursor, schema, prefix)
    commitRoads(db.conn, db.cursor, schema, prefix)
    cursor.execute("UPDATE %s.%s_network SET length=ST_Length(the_geom::geography);" % (schema, prefix))
    conn.commit()
    fdo1.close()
    fdo2.close()
    t2 = datetime.datetime.now()
    dt = t2-t1
    print ("Built %s roads" % num)
    print (" in %s" % dt)




# -- main check
if __name__ == '__main__':
  main(sys.argv)
   
