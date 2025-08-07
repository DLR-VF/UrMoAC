#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Builds a table with defined structures (not the network) using an 
OSM-database representation.

Call with
  osmdb_buildStructures.py <INPUT_TABLES_PREFIX> <DEF_FILE> <OUTPUT_TABLE> 
where <INPUT_TABLES_PREFIX> is defined as:
  <HOST>,<DB>,<SCHEMA>,<PREFIX>,<USER>,<PASSWD>  
and <OUTPUT_TABLE> is defined as:
  <HOST>,<DB>,<SCHEMA>,<NAME>,<USER>,<PASSWD>  
"""
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
import sys
import os
import psycopg2
import datetime
import math
import osm
import argparse 
script_dir = os.path.dirname( __file__ )
mymodule_dir = os.path.join(script_dir, '..', 'helper')
sys.path.append(mymodule_dir)
from wkt import *
from geom_helper import *


# --- data definitions ------------------------------------------------------
"""A map from a data type to the respective tags"""
subtype2tag = {
    "node": "ntag",
    "way": "wtag",
    "rel": "rtag"
}


# --- class definitions -----------------------------------------------------
class OSMExtractor:
    """A class for extracting defined structures from OSM
  
    The class reads a set of definitions of the things to extract from a definitions file.
    The definitions are stored as !!!.
    OSM data is retrieved from a database that was imported using "osm2db.py".
    @see osm2db.py  
    """
    
    def __init__(self):
        """Constructor"""
        # ids of matching objects
        self._objectIDs = { "node": [], "way": [], "rel": [] }
        # definitions of what to load
        self._defs = { "node": [], "way": [], "rel": [] }
        self._roles = set()
        # matching objects with geometries
        self._objectGeoms = { "node": {}, "way": {}, "rel": {} }
        # seen IDs for removing duplicate IDs
        self._idMapping = { "node": {}, "way": {}, "rel": {} }
        # map of load definitions to ids
        self._id2type = { "node": {}, "way": {}, "rel": {} }
    

    def load_definitions(self, file_name):
        """Loads the definitions about what to extract from a file
        :param file_name: The name of the file to read the definitions from
        :type file_name: str
        """
        fd = open(file_name)
        subtype = ""
        for l in fd:
            if len(l.strip())==0: continue # skip empty lines
            elif l[0]=='#': continue # skip comments
            elif l[0]=='[':
                # parse subtype
                subtype = l[1:l.find(']')].strip()
                continue
            elif l[0]=='<':
                # parse roles
                self._roles.add(l[1:l.find('>')].strip())
                continue
            # parse definition
            if subtype=="*":
                self._defs["node"].append(l.strip())
                self._defs["way"].append(l.strip())
                self._defs["rel"].append(l.strip())
            else:
                self._defs[subtype].append(l.strip())
        return self._defs
    

    def _get_objects(self, conn, cursor, schema, prefix, subtype, op, k, v):
        """Returns the IDs of the objects in the given OSM table that match the given definitions 
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        :param schema: The database schema to use
        :type schema: str
        :param prefix: The OSM database prefix to use
        :type prefix: str
        :param subtype: The OSM subtype to extract information about
        :type subtype: str
        :param op: The comparison operation
        :type op: str
        :param k: The key to match
        :type k: str
        :param v: The value to match, "*" if any
        :type v: str
        :return: The results of the query
        :rtype: 
        :todo: Make database connection an attribute of the class
        """
        ret = set()
        """
        if subtype=="rel":
            ret.add(11071436)
        return ret
        """
        if k=="*": # fetch all
            cursor.execute("SELECT id FROM %s.%s_%s" % (schema, prefix, subtype))
        elif v=='*': # fetch all with a matching key
            cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s'" % (schema, prefix, subtype2tag[subtype], k))
        else: 
            # fetch all with a key/value pair
            cursor.execute("SELECT id FROM %s.%s_%s WHERE k='%s' AND v%s'%s'" % (schema, prefix, subtype2tag[subtype], k, op, v))
        conn.commit()
        for r in cursor.fetchall():
            ret.add(int(r[0]))
        # !!! add option for extracting (a) certain structure/s by id
        return ret


    def get_object_ids(self, conn, cursor, schema, prefix):
        """Returns the IDs of the objects in the given OSM data that match the given definitions 
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        :param schema: The database schema to use
        :type schema: str
        :param prefix: The OSM database prefix to use
        :type prefix: str
        :todo: Make database connection an attribute of the class
        """
        for subtype in ["node", "way", "rel"]:
            print (" ... for %s" % subtype)
            for definition in self._defs[subtype]:
                # get objects
                subdefs = definition.split("&")
                collected = None
                for sd in subdefs:
                    sd = sd.strip()
                    if sd=="*":
                        oss = self._get_objects(conn, cursor, schema, prefix, subtype, "*", "*", "*")
                    else:
                        for op in ["!=", "!~", "=", "~"]:
                            if sd.find(op)>=0:
                                k,v = sd.split(op)
                                oss = self._get_objects(conn, cursor, schema, prefix, subtype, op, k, v)
                                break
                    if collected!=None:
                        collected = collected.intersection(oss)
                    else:
                        collected = oss
                    for os in oss:
                        if os not in self._id2type[subtype]:
                            self._id2type[subtype][os] = []
                        self._id2type[subtype][os].append(sd)
                if len(self._objectIDs[subtype])==0:
                    self._objectIDs[subtype] = collected
                else:
                    self._objectIDs[subtype] = self._objectIDs[subtype].union(collected) 
            print (" ... %s found" % len(self._objectIDs[subtype]))
            # !!! make this optional
            #for o in self._objectIDs[subtype]:
            #  print ("%s %s" % (subtype, o))
        # scan for duplicates
        print ("Scanning for duplicates")
        next_id = 0
        seenIDs = set()
        for subtype in ["node", "way", "rel"]:
            for id in self._objectIDs[subtype]:
                if id not in seenIDs:
                    seenIDs.add(id)
                    continue
                pid = id
                while True:
                    id = next_id
                    next_id += 1
                    if id not in seenIDs:
                        seenIDs.add(id)
                        self._idMapping[subtype][pid] = id
                        print (" Found duplicate id '%s'. Renaming %s to '%s'." % (pid, subtype, id))
                        break
   
   
    
    def collect_referenced_objects(self, conn, cursor, schema, prefix):
        """Collects all needed geometry information 
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        :param schema: The database schema to use
        :type schema: str
        :param prefix: The OSM database prefix to use
        :type prefix: str
        :todo: Make database connection an attribute of the class
        """
        def divide_chunks(l, n):
            for i in range(0, len(l), n):
                yield l[i:i + n]
        
        missingRELids = list(self._objectIDs["rel"])
        missingWAYids = set(self._objectIDs["way"])
        missingNODEids = set(self._objectIDs["node"])
        area = osm.OSMArea()
    
        # collect relation members, first
        seenRELs = set(missingRELids)
        rel2items = {}
        print (" ... for relations")
        while len(missingRELids)!=0:
            missingRELidsN = set()
            for mRELids in divide_chunks(missingRELids, 10000):
                idstr = ",".join([str(id) for id in mRELids])
                cursor.execute("SELECT rid,elemid,type,role FROM %s.%s_member WHERE rid in (%s) ORDER BY rid,role,idx" % (schema, prefix, idstr))
                conn.commit()
                for r in cursor.fetchall():
                    # close the currently processed relation if a new starts
                    rid = int(r[0])
                    relation = area.get_relation(rid)
                    if not relation:
                        relation = osm.OSMRelation(rid)
                        area.add_relation(relation)
                    role = r[3]
                    iid = int(r[1])
                    relation.add_member(iid, r[2], r[3])
                    if r[2]=="rel" or r[2]=="relation":
                        if iid==int(r[0]):
                            print ("Self-referencing relation %s" % r[0])
                            continue
                        if iid not in seenRELs:
                            missingRELidsN.add(iid)
                    elif r[2]=="way":
                        missingWAYids.add(iid)
                    elif r[2]=="node":
                        missingNODEids.add(iid)
                    else:
                        print ("Check type '%s'" % r[2])
            missingRELids = list(missingRELidsN)
        # collect ways
        print (" ... for ways")
        # https://stackoverflow.com/questions/30773911/union-of-multiple-sets-in-python
        npoints = [missingNODEids]
        if len(missingWAYids)!=0:
            for mWAYids in divide_chunks(list(missingWAYids), 10000):
                idstr = ",".join([str(id) for id in mWAYids])
                cursor.execute("SELECT id,refs FROM %s.%s_way WHERE id in (%s)" % (schema, prefix, idstr))
                conn.commit()
                for r in cursor.fetchall():
                    area.add_way(osm.OSMWay(int(r[0]), r[1]))
                    npoints.append(r[1])
        missingNODEids = set.union(*npoints)    
        # collect nodes
        print (" ... for nodes")
        if len(missingNODEids)!=0:
            for mNODEids in divide_chunks(list(missingNODEids), 10000):
                idstr = ",".join([str(id) for id in mNODEids])
                cursor.execute("SELECT id,ST_AsText(pos) FROM %s.%s_node WHERE id in (%s)" % (schema, prefix, idstr))
                conn.commit()
                for r in cursor.fetchall():
                    area.add_node(osm.OSMNode(int(r[0]), parse_POINT2D(r[1])))
        # clear - no longer used
        missingNODEids = []
        missingWAYids = []
        missingRELids = []
        return area


    def _check_commit(self, forced, entries, types, conn, cursor, schema, name):
        """Inserts read objects if forced or if their number is higher than 10000
        :param entries: Descriptions of the objects to insert
        :type entries: list[str]
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        :param schema: The database schema to use
        :type schema: str
        :param name: The name of the database table
        :type name: str
        :todo: Make database connection an attribute of the class
        """
        if not forced and len(entries)<10000:
            return
        if len(entries)==0:
            return
        args = ','.join(cursor.mogrify("(%s, %s, %s, ST_GeomFromText(%s, 4326), ST_GeomFromText(%s, 4326), ST_Centroid(ST_ConvexHull(ST_GeomFromText(%s, 4326))))", i).decode('utf-8') for i in entries)
        cursor.execute("INSERT INTO %s.%s(id, oid, type, polygon, geom_collection, centroid) VALUES " % (schema, name) + (args))
        conn.commit()
        args = ','.join(cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in types)
        cursor.execute("INSERT INTO %s.%s_types(id, oid, type) VALUES " % (schema, name) + (args))
        conn.commit()
        del entries[:]
        del types[:]
        

    def _add_item(self, entries, types, item, conn, cursor, schema, name):
        """Prebuilds the given item's insertion string and checks whether it shall be submitted
        :param entries: Descriptions of the objects to insert to extend
        :type entries: list[str]
        :param types: The types of the objects to add
        :type types: list[str]
        :param item: The item to add to the objects and types
        :type item: 
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        :param schema: The database schema to use
        :type schema: str
        :param name: The name of the database table
        :type name: str
        :todo: Make database connection an attribute of the class
        """
        oid, type, polys, geom = item.get_description_with_polygons()
        if len(geom)==0:
            print ("Missing geometry for %s %s" % (geom[1], geom[0]))
            return
        id = oid
        if oid in self._idMapping[type]:
            id = self._idMapping[type][oid]
        geom = "GEOMETRYCOLLECTION(" + geom + ")"
        centroid = geom
        if polys!=None and len(polys)!=0:
            # remove polygons within other
            toRemove = []
            for i,p1 in enumerate(polys):
                for j,p2 in enumerate(polys):
                    if i==j: continue
                    if polygon_in_polygon(p1[0], p2[0]): toRemove.append(i)
                    if polygon_in_polygon(p2[0], p1[0]): toRemove.append(j)
            npolys = []
            for i,p in enumerate(polys):
                if i in toRemove: continue
                npolys.append(p)
            polys = npolys
            #
            npolys = []
            for poly in polys:
                npoly = []
                for polypart in poly:
                    npolypart = "(" + ",".join(["%s %s" % (p[0], p[1]) for p in polypart]) + ")"
                    npoly.append(npolypart)
                npolys.append("(" + ",".join(npoly) + ")")
            polys = "MULTIPOLYGON(" + ",".join(npolys) + ")"
            centroid = polys
        else:
            polys = "MULTIPOLYGON EMPTY"
        entries.append([id, oid, type, polys, geom, centroid])
        for t in self._id2type[type][oid]:
            types.append([id, oid, t])
        self._check_commit(False, entries, types, conn, cursor, schema, name)


    def store_objects(self, area, conn, cursor, schema, name):
        """Stores objects and their geometries in the given table(s)
        
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        :param schema: The database schema to use
        :type schema: str
        :param name: The name of the database to store the objects in
        :type name: str
        :todo: Make database connection an attribute of the class
        """
        entries = []
        types = []
        fr = 0
        for rID in self._objectIDs["rel"]:
            if area._relations[rID].build_geometry(area):
                self._add_item(entries, types, area._relations[rID], conn, cursor, schema, name)
            else: fr += 1
        fw = 0
        for wID in self._objectIDs["way"]:
            if area._ways[wID].build_geometry(area):
                self._add_item(entries, types, area._ways[wID], conn, cursor, schema, name)
            else: fw += 1
        for nID in self._objectIDs["node"]:
            self._add_item(entries, types, area._nodes[nID], conn, cursor, schema, name)
        self._check_commit(True, entries, types, conn, cursor, schema, name)
        #
        return len(self._objectIDs["node"])+len(self._objectIDs["way"])+len(self._objectIDs["rel"]), fw, fr


    def save_mapping_if_exists(self, filename):
        """Saves the mapping of duplicate IDs to new ones if existing
        :param filename: The file to save the mapping to
        :type filename: str
        """
        if len(self._idMapping)==0:
            return
        with open(filename, "w") as fd:
            for type in self._idMapping:
                for id in self._idMapping[type]:
                    fd.write("%s;%s;%s\n" % (type, self._idMapping[type][id], id))


# --- function definitions --------------------------------------------------
# --- main
def build_structures(srcdb, deffile, dstdb, dropprevious, append, verbose):
    t1 = datetime.datetime.now()
    # -- open connection
    (host, db, schema_prefix, user, password) = srcdb.split(",")
    schema, prefix = schema_prefix.split(".")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()

    # -- load definitions of things to extract
    extractor = OSMExtractor()
    print ("Loading definition of things to extract")
    extractor.load_definitions(deffile)
    print ("Determining object IDs")
    extractor.get_object_ids(conn, cursor, schema, prefix)
    print ("Collecting object geometries")
    area = extractor.collect_referenced_objects(conn, cursor, schema, prefix)

    # -- write extracted objects
    # --- open connection
    print ("Building destination databases")
    (host, db, schema_name, user, password) = dstdb.split(",")
    schema, name = schema_name.split(".")
    conn2 = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor2 = conn2.cursor()
    # --- build tables
    if dropprevious:
        cursor2.execute("DROP TABLE IF EXISTS %s.%s" % (schema, name))
    if not append:
        cursor2.execute("CREATE TABLE %s.%s ( id bigint, oid bigint, type varchar(4) );" % (schema, name))
        cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'centroid', 4326, 'POINT', 2);" % (schema, name))
        cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'polygon', 4326, 'MULTIPOLYGON', 2);" % (schema, name))
        cursor2.execute("SELECT AddGeometryColumn('%s', '%s', 'geom_collection', 4326, 'GEOMETRYCOLLECTION', 2);" % (schema, name))
        cursor2.execute("DROP TABLE IF EXISTS %s.%s_types" % (schema, name))
        cursor2.execute("CREATE TABLE %s.%s_types (id bigint, oid bigint, type text);" % (schema, name))
        conn2.commit()
    # --- insert objects
    print ("Building and storing objects")
    num, fw, fr = extractor.store_objects(area, conn2, cursor2, schema, name)
    # --- write mapping of duplicate ids
    extractor.save_mapping_if_exists(name + "_mapping.txt")
    # --- finish
    t2 = datetime.datetime.now()
    dt = t2-t1
    print ("Built %s objects" % num)
    print (" in %s" % dt)
    if fw>0: print (" %s ways could not be build" % fw)
    if fr>0: print (" %s relations could not be build" % fr)



# --- function definitions --------------------------------------------------
# -- main
def main(arguments=None):
    """Main method"""
    # parse options
    if arguments is None:
        arguments = sys.argv[1:]
    # https://stackoverflow.com/questions/3609852/which-is-the-best-way-to-allow-configuration-options-be-overridden-at-the-comman
    defaults = {}
    conf_parser = argparse.ArgumentParser(prog='osmdb_buildStructures', add_help=False)
    conf_parser.add_argument("-c", "--config", metavar="FILE", help="Reads the named configuration file")
    args, remaining_argv = conf_parser.parse_known_args(arguments)
    if args.config is not None:
        if not os.path.exists(args.config):
            print ("osmdb_buildStructures: error: configuration file '%s' does not exist" % str(args.config), file=sys.stderr)
            raise SystemExit(2)
        config = configparser.ConfigParser()
        config.read([args.config])
        defaults.update(dict(config.items("DEFAULT")))
    parser = argparse.ArgumentParser(prog='osmdb_buildStructures', parents=[conf_parser], 
        description='Builds an road network table using an OSM-database representation', 
        epilog='(c) Copyright 2016-2025, German Aerospace Center (DLR)')
    parser.add_argument('OSMdatabase', metavar='OSM-database', help='The definition of the database to read data from;\n'
            + ' should be a string of the form <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>')
    parser.add_argument('definition', help='Defines the file to load the definitions of things to extract from')
    parser.add_argument('output', metavar='OSM-database', help='The definition of the database to read data from;\n'
            + ' should be a string of the form <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>')
    parser.add_argument('-R', '--dropprevious', action='store_true', help="Delete destination tables if already existing")
    parser.add_argument('-A', '--append', action='store_true', help="Append read data to existing tables")
    parser.add_argument('--version', action='version', version='%(prog)s 0.8.2')
    parser.add_argument("-v", "--verbose", action="store_true", help="Print what is being done")
    parser.set_defaults(**defaults)
    args = parser.parse_args(remaining_argv)

    # check and parse command line parameter and input files
    errors = []
    # - input db
    if len(args.OSMdatabase.split(","))!=5:
        errors.append("Missing values in target database definition;\n must be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
    elif args.OSMdatabase.split(",")[2].count(".")!=1:
        errors.append("The second field of the target database definition must have the format <SCHEMA>.<TABLE_PREFIX>")
    # - output db
    if len(args.output.split(","))!=5:
        errors.append("Missing values in target database definition;\n must be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
    elif args.output.split(",")[2].count(".")!=1:
        errors.append("The second field of the target database definition must have the format <SCHEMA>.<TABLE_PREFIX>")
    # - definition file
    if not os.path.exists(args.definition):
        errors.append(f"Missing definition file '{args.definition}'")
    # - report
    if len(errors)!=0:
        parser.print_usage(sys.stderr)
        for e in errors:
            print ("osmdb_buildStructures: error: %s" % e, file=sys.stderr)
        print ("osmdb_buildStructures: quitting on error.", file=sys.stderr)
        return 1

    return build_structures(args.OSMdatabase, args.definition, args.output, args.dropprevious, args.append, args.verbose)


# -- main check
if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
    
