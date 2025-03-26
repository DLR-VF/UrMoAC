#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Imports an OSM-file into a PostGIS database.

Call with
   osm2db.py <HOST>,<DB>,<SCHEMA>.<PREFIX>,<USER>,<PASSWD> <FILE>"""
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
import os
import string
import sys
import datetime
from xml.sax import saxutils, make_parser, handler
import psycopg2
import argparse
import osm


# --- class definitions -----------------------------------------------------
# --- OSMReader
class OSMReader(handler.ContentHandler):
    """A reader that parses an OSM XML file and writes it to a database."""

    def __init__(self, schema, dbprefix, conn, cursor, verbose):
        """Initialises the reader
        :param schema: The database schema to write the data to
        :type schema: str
        :param dbprefix: The database prefix to write the data to
        :type dbprefix: str
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        """
        self._fname = schema+"."+dbprefix
        self._conn = conn
        self._cursor = cursor
        self._verbose = verbose
        self._last = None
        self._elements = []
        self._nodes = []
        self._ways = []
        self._relations = []
        self.stats = {"nodes":0, "ways":0, "node_attrs":0, "way_attrs":0, "rels":0, "n_rmembers":0, "rel_attrs":0}
  
  
    def startElement(self, name, attrs):
        """Called when an element begins
        :param name: The name of the opened element
        :type name: str
        :param attrs: The element's attributes
        :type attrs: xml.sax.xmlreader.AttributesImpl
        """
        self._elements.append(name)
        if name=="osm" or name=="bounds":
            pass
        elif name=="node":
            id = int(attrs["id"])
            n = osm.OSMNode(id, [float(attrs["lon"]), float(attrs["lat"])])
            self._nodes.append(n)
            self._last = n
        elif name=="way":
            id = int(attrs["id"])
            e = osm.OSMWay(id)
            self._ways.append(e)
            self._last = e
        elif name=="nd":
            n = int(attrs["ref"])
            self._last.add_node_id(n)
        elif name=="relation":
            id = int(attrs["id"])
            r = osm.OSMRelation(id)
            self._relations.append(r)
            self._last = r
        elif name=="member":
            n = int(attrs["ref"])
            self._last.add_member(n, attrs["type"], attrs["role"])
        elif name=='tag' and self._last!=None:
            k = attrs['k']
            v = attrs['v']
            self._last.add_tag(k, v)
      
      
    def endElement(self, name):
        """Called when an element ends
        :param name: The name of the closed element
        :type name: str
        """
        l = self._elements[-1]
        if l=="node" or l=="way" or l=="relation":
            self._last = None
            self._check_commit(False)
        self._elements = self._elements[:-1]


    def _check_commit(self, force):
        """Commits stored entries if a sufficient number has been reached
        :param force: Whether elements shall be written in any case (on end)
        :type force: bool
        """
        if not force and (len(self._nodes)+len(self._ways)+len(self._relations))<10000:
            return
        n_ntags = 0
        n_wtags = 0
        n_rtags = 0
        n_rmembers = 0
        # nodes
        nodes_to_add = []
        node_tags_to_add = []
        for n in self._nodes:
            nodes_to_add.append((n.id, n.pos[0], n.pos[1]))
            for k in n.tags:
                node_tags_to_add.append((n.id, k, n.tags[k]))
                n_ntags = n_ntags + 1
        if len(nodes_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s, ST_GeomFromText('POINT(%s %s)', 4326))", i).decode('utf-8') for i in nodes_to_add)
            self._cursor.execute("INSERT INTO %s_node(id, pos) VALUES " % (self._fname) + (args))
        if len(node_tags_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in node_tags_to_add)
            self._cursor.execute("INSERT INTO %s_ntag(id, k, v) VALUES " % (self._fname) + (args))
        # ways
        ways_to_add = []
        way_tags_to_add = []
        for w in self._ways:
            ways_to_add.append((w.id, w.refs))
            for k in w.tags:
                way_tags_to_add.append((w.id, k, w.tags[k]))
                n_wtags = n_wtags + 1
        if len(ways_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s, %s)", i).decode('utf-8') for i in ways_to_add)
            self._cursor.execute("INSERT INTO %s_way(id, refs) VALUES " % (self._fname) + (args))
        if len(way_tags_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in way_tags_to_add)
            self._cursor.execute("INSERT INTO %s_wtag(id, k, v) VALUES " % (self._fname) + (args))
        # relations
        rels_to_add = []
        rel_tags_to_add = []
        members_to_add = []
        for r in self._relations:
            rels_to_add.append((r.id,))
            for k in r.tags:
                rel_tags_to_add.append((r.id, k, r.tags[k]))
                n_rtags = n_rtags + 1
            for idx,m in enumerate(r.members):
                members_to_add.append((r.id, m[0], m[1], m[2], idx))
                n_rmembers = n_rmembers + 1
        if len(rels_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s)", i).decode('utf-8') for i in rels_to_add)
            self._cursor.execute("INSERT INTO %s_rel(id) VALUES " % (self._fname) + (args))
        if len(rel_tags_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s, %s, %s)", i).decode('utf-8') for i in rel_tags_to_add)
            self._cursor.execute("INSERT INTO %s_rtag(id, k, v) VALUES " % (self._fname) + (args))
        if len(members_to_add)>0:
            args = ','.join(self._cursor.mogrify("(%s, %s, %s, %s, %s)", i).decode('utf-8') for i in members_to_add)
            self._cursor.execute("INSERT INTO %s_member(rid, elemID, type, role, idx) VALUES " % (self._fname) + (args))
        #
        if self._verbose:
            print (" %s nodes (%s keys), %s ways (%s keys), and %s relations (%s keys, %s members)" % (len(self._nodes), n_ntags, len(self._ways), n_wtags, len(self._relations), n_rtags, n_rmembers))
        self.stats["nodes"] = self.stats["nodes"] + len(self._nodes)
        self.stats["ways"] = self.stats["ways"] + len(self._ways)
        self.stats["node_attrs"] = self.stats["node_attrs"] + n_ntags
        self.stats["way_attrs"] = self.stats["way_attrs"] + n_wtags
        self.stats["rels"] = self.stats["rels"] + len(self._relations)
        self.stats["n_rmembers"] = self.stats["n_rmembers"] + n_rmembers
        self.stats["rel_attrs"] = self.stats["rel_attrs"] + n_rtags
        if len(self._nodes)+len(self._ways)+len(self._relations)>0:
            self._conn.commit() 
        self._nodes = []
        self._ways = []
        self._relations = []
    

# --- function definitions --------------------------------------------------
# --- main 
def osm2db(db_def, input_file, dropprevious, append, verbose):
    """Main method
    :param db_def: The definition of the database tables to generate
    :type db_def: str
    :param input_file: The OSM file to parse
    :type input_file: str
    """
    # connect to the database
    (host, db, schema_prefix, user, password) = db_def.split(",")
    schema, prefix = schema_prefix.split(".")
    t1 = datetime.datetime.now()
    print ("Connecting to the db...")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    print ("Building tables for %s..." % prefix)
    
    # check whether the tables already exist
    # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
    cursor.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s_node');" % (schema, prefix))
    conn.commit()   
    ret = cursor.fetchall()
    if ret[0][0]:
        if dropprevious:
            # delete if already existing
            cursor.execute("DROP TABLE %s.%s_member;" % (schema, prefix))
            cursor.execute("DROP TABLE %s.%s_rtag;" % (schema, prefix))
            cursor.execute("DROP TABLE %s.%s_wtag;" % (schema, prefix))
            cursor.execute("DROP TABLE %s.%s_ntag;" % (schema, prefix))
            cursor.execute("DROP TABLE %s.%s_rel;" % (schema, prefix))
            cursor.execute("DROP TABLE %s.%s_way;" % (schema, prefix))
            cursor.execute("DROP TABLE %s.%s_node;" % (schema, prefix))
            conn.commit()
        elif not append:
            print ("osm2db: error: destination tables already exist", file=sys.stderr)
            return 1
    
    # build the tables
    if not append:
        cursor.execute("CREATE TABLE %s.%s_node (id bigint PRIMARY KEY);" % (schema, prefix))
        cursor.execute("CREATE TABLE %s.%s_way (id bigint PRIMARY KEY, refs bigint[]);" % (schema, prefix))
        cursor.execute("CREATE TABLE %s.%s_rel (id bigint PRIMARY KEY);" % (schema, prefix))
        cursor.execute("SELECT AddGeometryColumn('%s', '%s_node', 'pos', 4326, 'POINT', 2, true);" % (schema, prefix))
        # --- tags
        cursor.execute("CREATE TABLE %s.%s_ntag ( id bigint REFERENCES %s.%s_node (id), k text, v text );" % (schema, prefix, schema, prefix))
        cursor.execute("CREATE INDEX ON %s.%s_ntag (id);" % (schema, prefix))
        cursor.execute("CREATE TABLE %s.%s_wtag ( id bigint REFERENCES %s.%s_way (id), k text, v text );" % (schema, prefix, schema, prefix))
        cursor.execute("CREATE INDEX ON %s.%s_wtag (id);" % (schema, prefix))
        cursor.execute("CREATE TABLE %s.%s_rtag ( id bigint REFERENCES %s.%s_rel (id), k text, v text );" % (schema, prefix, schema, prefix))
        cursor.execute("CREATE INDEX ON %s.%s_rtag (id);" % (schema, prefix))
        cursor.execute("CREATE TABLE %s.%s_member ( rid bigint REFERENCES %s.%s_rel (id), elemID bigint, type text, role text, idx integer );" % (schema, prefix, schema, prefix))
        cursor.execute("CREATE INDEX ON %s.%s_member (rid);" % (schema, prefix))
        cursor.execute("CREATE INDEX ON %s.%s_member (elemID);" % (schema, prefix))
        conn.commit()

    # parsing the document and adding contents to the db
    print (f"Parsing '{input_file}'...")
    parser = make_parser()
    r = OSMReader(schema, prefix, conn, cursor, verbose)
    parser.setContentHandler(r)
    parser.parse(input_file)
    r._check_commit(True)
    cursor.close()
    conn.close()

    # report
    t2 = datetime.datetime.now()
    print ("Finished.")
    print ("Summary:")
    print (" %s nodes with %s attributes" % (r.stats["nodes"], r.stats["node_attrs"]))
    print (" %s ways with %s attributes" % (r.stats["ways"], r.stats["way_attrs"]))
    print (" %s relations with %s members and %s attributes" % (r.stats["rels"], r.stats["n_rmembers"], r.stats["rel_attrs"]))
    dt = t2-t1
    print ("In %s" % dt)
    return 0


def main(arguments=None):
    """main function"""
    # parse options
    if arguments is None:
        arguments = sys.argv[1:]
    # https://stackoverflow.com/questions/3609852/which-is-the-best-way-to-allow-configuration-options-be-overridden-at-the-comman
    defaults = {}
    conf_parser = argparse.ArgumentParser(prog='osm2db', add_help=False)
    conf_parser.add_argument("-c", "--config", metavar="FILE", help="Reads the named configuration file")
    args, remaining_argv = conf_parser.parse_known_args(arguments)
    if args.config is not None:
        if not os.path.exists(args.config):
            print ("osm2db: error: configuration file '%s' does not exist" % str(args.config), file=sys.stderr)
            raise SystemExit(2)
        config = configparser.ConfigParser()
        config.read([args.config])
        defaults.update(dict(config.items("DEFAULT")))
    parser = argparse.ArgumentParser(prog='osm2db', parents=[conf_parser], 
        description='Imports an OSM file into a PostGIS database', 
        epilog='(c) Copyright 2016-2025, German Aerospace Center (DLR)')
    parser.add_argument('OSMdatabase', metavar='OSM-database', help='The definition of the database to write the data into;\n'
            + ' should be a string of the form <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>')
    parser.add_argument('OSMfile', metavar='OSM-file', help='The OSM-file to read')
    parser.add_argument('--version', action='version', version='%(prog)s 0.8.2')
    parser.add_argument('-R', '--dropprevious', action='store_true', help="Delete destination tables if already existing")
    parser.add_argument('-A', '--append', action='store_true', help="Append read data to existing tables")
    parser.add_argument("-v", "--verbose", action="store_true", help="Print what is being done")
    parser.set_defaults(**defaults)
    args = parser.parse_args(remaining_argv)

    # check and parse command line parameter and input files
    errors = []
    # - output db
    if len(args.OSMdatabase.split(","))!=5:
        errors.append("Missing values in target database definition;\n must be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
    elif args.OSMdatabase.split(",")[2].count(".")!=1:
        errors.append("The second field of the target database definition must have the format <SCHEMA>.<TABLE_PREFIX>")
    # - report
    if len(errors)!=0:
        parser.print_usage(sys.stderr)
        for e in errors:
            print ("osm2db: error: %s" % e, file=sys.stderr)
        print ("osm2db: quitting on error.", file=sys.stderr)
        return 2

    # run
    return osm2db(args.OSMdatabase, args.OSMfile, args.dropprevious, args.append, args.verbose)


# -- main check
if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))

