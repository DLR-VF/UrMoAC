#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Imports a GTFS data set into a database

Call with
    importGTFS.py <INPUT_FOLDER> <TARGET_DB_DEFINITION>
where
    <TARGET_DB_DEFINITION> is <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>
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
from enum import IntEnum
import psycopg2
import sys
import os.path
import io
import argparse
import gtfs_defs


# --- data definitions ------------------------------------------------------
"""! @brief A map from GTFS data types to Postgres datatypes"""
gtfs2postgres = {
    gtfs_defs.FieldType.COLOR : "text",
    gtfs_defs.FieldType.CURRENCY_CODE : "text",
    gtfs_defs.FieldType.CURRENCY_AMOUNT : "text",
    gtfs_defs.FieldType.DATE : "text",
    gtfs_defs.FieldType.EMAIL : "text",
    gtfs_defs.FieldType.ENUM : "integer",
    gtfs_defs.FieldType.ID : "text",
    gtfs_defs.FieldType.LANGUAGE_CODE : "text",
    gtfs_defs.FieldType.LATITUDE : "real",
    gtfs_defs.FieldType.LONGITUDE : "real",
    gtfs_defs.FieldType.FLOAT : "real",
    gtfs_defs.FieldType.INTEGER : "integer",
    gtfs_defs.FieldType.PHONE_NUMBER : "text",
    gtfs_defs.FieldType.TIME : "text",
    gtfs_defs.FieldType.TEXT : "text",
    gtfs_defs.FieldType.TIMEZONE : "text",
    gtfs_defs.FieldType.URL : "text"
}


# --- class definitions -----------------------------------------------------
class GTFSImporter:
    """A class for importing GTFS data into a database"""

    def __init__(self, src_folder, db_definition, append, dropprevious, verbose):
        """Initialises the importer
        :param src_folder: The folder to read data from
        :type src_folder: str
        :param db_definition: The definition of the database to write the data to
        :type db_definition: str
        """
        self._src_folder = src_folder
        (self._host, self._db, schema_prefix, self._user, self._password) = db_definition.split(",")
        self._schema, self._tablePrefix = schema_prefix.split(".")
        # connect to db
        self._conn = psycopg2.connect("host='%s' dbname='%s' user='%s' password='%s'" % (self._host, self._db, self._user, self._password))
        self._cursor = self._conn.cursor()
        self._append = append
        self._verbose = verbose
        self._dropprevious = dropprevious
        
        
    def drop_tables(self):
        """Drops existing database tables"""
        if self._verbose: 
            print ("Removing old tables")
        for td in gtfs_defs.tableDefinitions:
            self._cursor.execute("DROP TABLE IF EXISTS %s.%s_%s;" % (self._schema, self._tablePrefix, td))
        self._conn.commit()


    def _split_vals(self, line):
        """Splits a given GTFS line taking into account quotes
        :param line: The line of a GTFS file to split
        :type line: str
        """
        vals = line.split(",")
        new_vals = []
        hadQuotes = False
        for v in vals:
            v2 = v
            if hadQuotes: # had a quote before?
                # then append current entry to the last one
                new_vals[-1] = new_vals[-1] + "," + v2
                if len(v2)>0 and v2[-1]=='"': # ends with a quote?
                    # move to next entry
                    hadQuotes = False
                    # remove the quote
                    new_vals[-1] = new_vals[-1][:-1]
                continue
            if len(v2)>0 and v2[0]=='"': # starts with a quote?
                # mark as having a quote, remove quote
                hadQuotes = True
                v2 = v2[1:]
                if v2[-1]=='"': # ends with a quote?
                    # remove quote, plain element
                    v2 = v2[:-1]
                    hadQuotes = False
            new_vals.append(v2)
        return new_vals


    def _get_columns_to_write(self, file_type, l):
        """Determines which fields shall be imported 
        :param file_type: The file type to read and import
        :type file_type: str
        :param l: The header line
        :type l: str
        """
        orig_names = l.strip().split(",")
        for i,n in enumerate(orig_names):
            orig_names[i] = n.replace('"', '').strip()
        columns = []
        srcIndices = []
        for n in gtfs_defs.tableDefinitions[file_type]:
            if n[0] not in orig_names:
                if n[2]==gtfs_defs.Presence.REQUIRED:
                    print (" Required column '%s' is missing. Aborting..." % (n[0]), file=sys.stderr)
                    return False
            else:
                columns.append(n)
                srcIndices.append(orig_names.index(n[0]))
        return columns, srcIndices
  

    def _import_table(self, file_type):
        """Reads a single GTFS file and writes its contents into a table
        :param file_type: The file type to read and import
        :type file_type: str
        @see https://www.geeksforgeeks.org/python-psycopg2-insert-multiple-rows-with-one-query/
        """
        file_name = os.path.join(self._src_folder, file_type + ".txt")
        if self._verbose: print (" Processing " + file_name)
        #
        self._cursor.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s_%s');" % (self._schema, self._tablePrefix, file_type))
        self._conn.commit()   
        ret = self._cursor.fetchall()
        if ret[0][0] and not self._append:
            print ("Table %s.%s_%s already exists. Aborting. Consider adding the option --dropprevious." % (self._schema, self._tablePrefix, file_type))
            sys.exit(2)
        #
        fd = io.open(file_name, 'r', encoding='utf-8-sig')
        first = True
        num = 0
        entries = []
        # go throught the file
        for l in fd:
            build_table = first and not self._append
            if build_table:
                # collect known fields
                columns, srcIndices = self._get_columns_to_write(file_type, l)
                # build the insertion string
                hasPosition = False
                newNames = []
                for e in columns:
                    newNames.append(e[0])
                    if e[1]==gtfs_defs.FieldType.LATITUDE or e[1]==gtfs_defs.FieldType.LONGITUDE:
                        hasPosition = True
                namesDB = ", ".join(newNames)
                placeHolders = ", ".join(["%s"]*len(newNames))
                if hasPosition:
                    placeHolders = placeHolders + ", ST_GeomFromText('POINT(%s %s)', 4326)"
                    namesDB = namesDB + ", pos" 
                placeHolders = "(" + placeHolders + ")"
                # build the table
                call = "CREATE TABLE %s.%s_%s ( " % (self._schema, self._tablePrefix, file_type)
                for ie,e in enumerate(columns):
                    if ie>0: call = call + ", "
                    call = call + "%s %s" % (e[0], gtfs2postgres[e[1]])
                call = call + " );"
                self._cursor.execute(call)
                self._conn.commit()
                if hasPosition:
                    self._cursor.execute("SELECT AddGeometryColumn('%s', '%s_%s', 'pos', 4326, 'POINT', 2);" % (self._schema, self._tablePrefix, file_type))
                    self._conn.commit()

            if first:
                # skipt to first entry
                first = False
                continue
       
            # process entries
            if l.strip()=="":
                continue
            vals = self._split_vals(l.strip())
            vals = [v.strip() for v in vals]
            newVals = []
            pos = [0, 0]
            for ic, c in enumerate(columns):
                i = srcIndices[ic]
                if c[1]==gtfs_defs.FieldType.INTEGER or c[1]==gtfs_defs.FieldType.ENUM:
                    if len(vals[i])!=0:
                        newVals.append(int(vals[i]))
                    else:
                        newVals.append(-1)
                elif c[1]==gtfs_defs.FieldType.FLOAT or c[1]==gtfs_defs.FieldType.LATITUDE or c[1]==gtfs_defs.FieldType.LONGITUDE:
                    if len(vals[i])!=0:
                        newVals.append(float(vals[i]))
                    elif c[0]=="min_transfer_time":
                        newVals.append(0)
                    else:
                        newVals.append(-1)
                    if hasPosition and c[1]==gtfs_defs.FieldType.LATITUDE:
                        pos[1] = newVals[-1]
                    if hasPosition and c[1]==gtfs_defs.FieldType.LONGITUDE:
                        pos[0] = newVals[-1]
                else:
                    newVals.append(vals[i])
            # append a given position
            if hasPosition: newVals.extend(pos)
            entries.append(list(newVals))
            num += 1
            # commit if collected enough
            if num%10000==0 and num!=0:
                # insert into db
                args = ','.join(self._cursor.mogrify(placeHolders, i).decode('utf-8') for i in entries)
                self._cursor.execute("INSERT INTO %s.%s_%s (%s) VALUES " % (self._schema, self._tablePrefix, file_type, namesDB) + (args))
                self._conn.commit()
                entries.clear()
        # commit
        if len(entries)!=0:
            args = ','.join(self._cursor.mogrify(placeHolders, i).decode('utf-8') for i in entries)
            self._cursor.execute("INSERT INTO %s.%s_%s (%s) VALUES " % (self._schema, self._tablePrefix, file_type, namesDB) + (args))
            self._conn.commit()
        if num==0:
            if self._verbose:
                print ("  No data found! Table %s.%s_%s will be deleted." % (self._schema, self._tablePrefix, file_type))
            self._cursor.execute("DROP TABLE IF EXISTS %s.%s_%s;" % (self._schema, self._tablePrefix, file_type))
            self._conn.commit()


    def import_files(self):
        """Goes through the tables and imports existing ones"""
        if self._verbose:
            print ("Importing data")
        for td in gtfs_defs.tableDefinitions:
            # skip non-existing, optional files
            if td in gtfs_defs.optionalTables and not os.path.exists(os.path.join(self._src_folder, td+".txt")):
                if self._verbose:
                    print (" The non-mandatory file '%s.txt' is missing.txt. Ignoring." % td)
                continue
            if not os.path.exists(os.path.join(self._src_folder, td+".txt")):
                print (" Mandatory file '%s.txt' is missing. Aborting" % td, file=sys.stderr)
                sys.exit(2)
            self._import_table(td)
        self._cursor.execute("CREATE INDEX ON %s.%s_stop_times (trip_id);"  % (self._schema, self._tablePrefix) )
        self._conn.commit()


    def add_stop_ids(self):
        """Adds a running numerical ID to the bus stops"""
        if self._verbose:
            print ("Adding IDs to stops")
        self._cursor.execute("ALTER TABLE %s.%s_stops ADD COLUMN id SERIAL PRIMARY KEY;"  % (self._schema, self._tablePrefix) )
        self._conn.commit()
        

    def add_lines_to_stops(self):
        """Adds line information to stops
        :todo Move to a GTFS-manipulating class?
        """
        if self._verbose:
            print ("Extending stops by lines")
            print (" ...retrieving routes")
        route2line = {}
        self._cursor.execute("SELECT route_id,route_short_name from %s.%s_routes;" % (self._schema, self._tablePrefix))
        for t in self._cursor.fetchall():
            route2line[t[0]] = t[1]
        if self._verbose:
            print (" ...retrieving trips")
        trip2route = {}
        self._cursor.execute("SELECT trip_id,route_id from %s.%s_trips;" % (self._schema, self._tablePrefix))
        for t in self._cursor.fetchall():
            trip2route[t[0]] = t[1]
        if self._verbose:
            print (" ...retrieving stop times")
        stop2lines = {}
        self._cursor.execute("SELECT trip_id,stop_id from %s.%s_stop_times;" % (self._schema, self._tablePrefix))
        for t in self._cursor.fetchall():
            routeID = trip2route[t[0]]
            line = route2line[routeID]
            if t[1] not in stop2lines:
                stop2lines[t[1]] = set()
            stop2lines[t[1]].add(line)
        if self._verbose:
            print (" ...extending stops")
        self._cursor.execute("ALTER TABLE %s.%s_stops ADD lines text" % (self._schema, self._tablePrefix))
        self._conn.commit()
        for n,s in enumerate(stop2lines):
            self._cursor.execute("UPDATE %s.%s_stops SET lines='%s' WHERE stop_id='%s';"  % (self._schema, self._tablePrefix, ",".join(stop2lines[s]), s))
            if n%10000==0:
                self._conn.commit()
        if n%10000!=0:
            self._conn.commit()


# --- function definitions --------------------------------------------------
# -- main
def main(arguments=None):
    """Main method"""
    # parse options
    if arguments is None:
        arguments = sys.argv[1:]
    # https://stackoverflow.com/questions/3609852/which-is-the-best-way-to-allow-configuration-options-be-overridden-at-the-comman
    defaults = {}
    conf_parser = argparse.ArgumentParser(prog='importGTFS', add_help=False)
    conf_parser.add_argument("-c", "--config", metavar="FILE", help="Reads the named configuration file")
    args, remaining_argv = conf_parser.parse_known_args(arguments)
    if args.config is not None:
        if not os.path.exists(args.config):
            print ("importGTFS: error: configuration file '%s' does not exist" % str(args.config), file=sys.stderr)
            raise SystemExit(2)
        config = configparser.ConfigParser()
        config.read([args.config])
        defaults.update(dict(config.items("DEFAULT")))
    parser = argparse.ArgumentParser(prog='importGTFS', parents=[conf_parser], 
        description='Imports a GTFS file set into a PostGIS database', 
        epilog='(c) Copyright 2016-2025, German Aerospace Center (DLR)')
    parser.add_argument('GTFSdatabase', metavar='GTFS-database', help='The definition of the database to write the data into;'
            + ' should be a string of the form <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>')
    parser.add_argument('GTFSfolder', metavar='GTFS-folder', help='The folder the GTFS files are located in')
    parser.add_argument('--version', action='version', version='%(prog)s 0.8.2')
    parser.add_argument('-R', '--dropprevious', action='store_true', help="Delete destination tables if already existing")
    parser.add_argument('-A', '--append', action='store_true', help="Append read data to existing tables")
    parser.add_argument('-C', '--keep-open', dest='keep_open', action='store_true', help="If not set, line information is not added to stops")
    parser.add_argument('-I', '--add-ids', dest="add_ids", action='store_true', help="Whan set, adds integer ids to stops")
    parser.add_argument("-v", "--verbose", action="store_true", help="Print what is being done")
    parser.set_defaults(**defaults)
    args = parser.parse_args(remaining_argv)

    # check and parse command line parameter and input files
    errors = []
    # - input folder
    if not os.path.exists(args.GTFSfolder):
        errors.append("The folder " + args.GTFSfolder + " to import GTFS from does not exist.")
    # - input files
    for f in ["agency", "calendar", "routes", "stop_times", "stops", "trips"]:
        fn = os.path.join(args.GTFSfolder, f+".txt")
        if not os.path.exists(fn):
            errors.append("The mandatory file '" + fn + "' to import GTFS from does not exist.")
    # - output db
    if len(args.GTFSdatabase.split(","))!=5:
        errors.append("Missing values in target database definition;\n must be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
    elif args.GTFSdatabase.split(",")[2].count(".")!=1:
        errors.append("The second field of the target database definition must have the format <SCHEMA>.<TABLE_PREFIX>")
    # - report
    if len(errors)!=0:
        parser.print_usage(sys.stderr)
        for e in errors:
            print ("importGTFS: error: %s" % e, file=sys.stderr)
        print ("importGTFS: quitting on error.", file=sys.stderr)
        return 1
        
    # build the importer
    importer = GTFSImporter(args.GTFSfolder, args.GTFSdatabase, args.append, args.dropprevious, args.verbose)
    # drop existing tables
    if args.dropprevious:
        importer.drop_tables()
    # import data
    importer.import_files()
    # extend stops by (numerical) ids
    if args.add_ids:
        importer.add_stop_ids()
    # extend stops by lines
    if not args.keep_open:
        importer.add_lines_to_stops()
    return 0


# -- main check
if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
  
