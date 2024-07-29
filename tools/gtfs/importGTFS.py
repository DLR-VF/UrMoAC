#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Imports a GTFS data set into a database

Call with
    importGTFS.py <INPUT_FOLDER> <TARGET_DB_DEFINITION>
where
    <TARGET_DB_DEFINITION> is <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>
"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2016-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.0"
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

    def __init__(self, srcFolder, dbDef):
        """Initialises the importer
        :param srcFolder: The folder to read data from
        :type srcFolder: str
        :param dbDef: The definition of the database to write the data to
        :type dbDef: str
        """
        self._srcFolder = srcFolder
        (self._host, self._db, schema_prefix, self._user, self._password) = dbDef.split(",")
        self._schema, self._tablePrefix = schema_prefix.split(".")
        # connect to db
        self._conn = psycopg2.connect("host='%s' dbname='%s' user='%s' password='%s'" % (self._host, self._db, self._user, self._password))
        self._cursor = self._conn.cursor()

        
    def drop_tables(self):
        """Drops existing database tables"""
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
        nVals = []
        hadQuotes = False
        for v in vals:
            v2 = v
            if hadQuotes: # had a quote before?
                # then append current entry to the last one
                nVals[-1] = nVals[-1] + "," + v2
                if len(v2)>0 and v2[-1]=='"': # ends with a quote?
                    # move to next entry
                    hadQuotes = False
                    # remove the quote
                    nVals[-1] = nVals[-1][:-1]
                continue
            if len(v2)>0 and v2[0]=='"': # starts with a quote?
                # mark as having a quote, remove quote
                hadQuotes = True
                v2 = v2[1:]
                if v2[-1]=='"': # ends with a quote?
                    # remove quote, plain element
                    v2 = v2[:-1]
                    hadQuotes = False
            nVals.append(v2)
        return nVals


    def _get_columns_to_write(self, fileType, l):
        """Determines which fields shall be imported 
        :param fileType: The fileType type to read and import
        :type fileType: str
        :param l: The header line
        :type l: str
        """
        origNames = l.strip().split(",")
        for i,n in enumerate(origNames):
            origNames[i] = n.replace('"', '').strip()
        columns = []
        srcIndices = []
        for n in gtfs_defs.tableDefinitions[fileType]:
            if n[0] not in origNames:
                if n[2]==gtfs_defs.Presence.REQUIRED:
                    print (" Required column '%s' is missing. Aborting..." % (n[0]))
                    return False
            else:
                columns.append(n)
                srcIndices.append(origNames.index(n[0]))
        return columns, srcIndices
  

    def _import_table(self, fileType):
        """Reads a single GTFS file and writes its contents into a table
        :param fileType: The fileType type to read and import
        :type fileType: str
        @see https://www.geeksforgeeks.org/python-psycopg2-insert-multiple-rows-with-one-query/
        """
        fileName = os.path.join(self._srcFolder, fileType + ".txt")
        print (" Processing " + fileName)
        fd = io.open(fileName, 'r', encoding='utf-8-sig')
        first = True
        num = 0
        entries = []
        # go throught the file
        for l in fd:
            if first: # first element (header)?
                # collect known fields
                columns, srcIndices = self._get_columns_to_write(fileType, l)
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
                call = "CREATE TABLE %s.%s_%s ( " % (self._schema, self._tablePrefix, fileType)
                for ie,e in enumerate(columns):
                    if ie>0: call = call + ", "
                    call = call + "%s %s" % (e[0], gtfs2postgres[e[1]])
                call = call + " );"
                self._cursor.execute(call)
                self._conn.commit()
                if hasPosition:
                    self._cursor.execute("SELECT AddGeometryColumn('%s', '%s_%s', 'pos', 4326, 'POINT', 2);" % (self._schema, self._tablePrefix, fileType))
                    self._conn.commit()
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
                self._cursor.execute("INSERT INTO %s.%s_%s (%s) VALUES " % (self._schema, self._tablePrefix, fileType, namesDB) + (args))
                self._conn.commit()
                entries.clear()
        # commit
        if len(entries)!=0:
            args = ','.join(self._cursor.mogrify(placeHolders, i).decode('utf-8') for i in entries)
            self._cursor.execute("INSERT INTO %s.%s_%s (%s) VALUES " % (self._schema, self._tablePrefix, fileType, namesDB) + (args))
            self._conn.commit()
        if num==0:
            print ("  No data found! Table %s.%s_%s will be deleted." % (self._schema, self._tablePrefix, fileType))
            self._cursor.execute("DROP TABLE IF EXISTS %s.%s_%s;" % (self._schema, self._tablePrefix, fileType))
            self._conn.commit()


    def import_files(self):
        """Goes through the tables and imports existing ones"""
        print ("Importing data")
        for td in gtfs_defs.tableDefinitions:
            # skip non-existing, optional files
            if td in gtfs_defs.optionalTables and not os.path.exists(os.path.join(self._srcFolder, td+".txt")):
                print (" The non-mandatory file '%s.txt' is missing.txt. Ignoring." % td)
                continue
            if not os.path.exists(os.path.join(self._srcFolder, td+".txt")):
                print (" Mandatory file '%s.txt' is missing. Aborting"% td)
                sys.exit()
            self._import_table(td)
        self._cursor.execute("CREATE INDEX ON %s.%s_stop_times (trip_id);"  % (self._schema, self._tablePrefix) )
        self._conn.commit()


    def add_lines_to_stops(self):
        """Adds line information to stops
        :todo Move to a GTFS-manipulating class?
        """
        print ("Extending stops by lines")
        print (" ...retrieving routes")
        route2line = {}
        self._cursor.execute("SELECT route_id,route_short_name from %s.%s_routes;" % (self._schema, self._tablePrefix))
        for t in self._cursor.fetchall():
            route2line[t[0]] = t[1]
        print (" ...retrieving trips")
        trip2route = {}
        self._cursor.execute("SELECT trip_id,route_id from %s.%s_trips;" % (self._schema, self._tablePrefix))
        for t in self._cursor.fetchall():
            trip2route[t[0]] = t[1]
        print (" ...retrieving stop times")
        stop2lines = {}
        self._cursor.execute("SELECT trip_id,stop_id from %s.%s_stop_times;" % (self._schema, self._tablePrefix))
        for t in self._cursor.fetchall():
            routeID = trip2route[t[0]]
            line = route2line[routeID]
            if t[1] not in stop2lines:
                stop2lines[t[1]] = set()
            stop2lines[t[1]].add(line)
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
def import_gtfs(input_folder, target_db):
    """Main method"""
    # check and parse command line parameter and input files
    # - input folder
    if not os.path.exists(input_folder):
        print ("The folder '" + input_folder + "' to import GTFS from does not exist.")
        sys.exit()
    # - input files
    error = False
    for f in ["agency", "calendar", "routes", "stop_times", "stops", "trips"]:
        fn = os.path.join(input_folder, f+".txt")
        if not os.path.exists(fn):
            print ("The mandatory file '" + fn + "' to import GTFS from does not exist.")
            error = True
    if error:
        sys.exit()
    # - output db
    if len(target_db.split(","))<5:
        print ("Missing values in target database definition; should be: <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>")
        sys.exit()
        
    # build the importer
    importer = GTFSImporter(input_folder, target_db)
    # drop existing tables
    importer.drop_tables()
    # import data
    importer.import_files()
    # extend stops by lines
    importer.add_lines_to_stops()


# -- main check
if __name__ == '__main__':
    if len(sys.argv)<3:
        print (""""Error: Parameter is missing\nPlease run with:
    importGTFS.py <INPUT_FOLDER> <TARGET_DB_DEFINITION>
where
    <TARGET_DB_DEFINITION> is <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>""")
        sys.exit(1)
    import_gtfs(sys.argv[1], sys.argv[2])
  
