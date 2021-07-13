#!/usr/bin/env python
# =========================================================
# importGTFS.py
# @author Daniel Krajzewicz
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Imports a given GTFS data set
# Call with
#  importGTFS <INPUT_PATH> <SCHEMA>.<PREFIX>
# =========================================================
import psycopg2, sys, os.path
import getpass

from tools.db_config import db_host, db_user, db_name

def myValSplit(line):
  vals = line.split(",")
  nVals = []
  hadQuotes = False
  for v in vals:
    #v2 = v.strip()
    v2 = v
    if hadQuotes:
      if len(v2)==0:
        print ("------------------")
        print (line)
        print (vals)
        raise "haha"
      nVals[-1] = nVals[-1] + "," + v2
      if v2[-1]=='"':
        hadQuotes = False
        nVals[-1] = nVals[-1][:-1]
      continue
    if len(v2)>0 and v2[0]=='"': 
      hadQuotes = True
      v2 = v2[1:]
      if v2[-1]=='"':
        v2 = v2[:-1]
        hadQuotes = False
    nVals.append(v2)
  return nVals
  

def readImportTable(conn, cursor, tableName, fileName, posNames, realNames, intNames):
  print ("Processing " + fileName)
  fd = open(fileName)
  first = True
  num = 0
  for l in fd:
  
    if first:
      names = l.strip().split(",")
      namesDB = ", ".join(names)
      placeHolders = ", ".join(["%s"]*len(names))
      if len(posNames)!=0:
        placeHolders = placeHolders + ", ST_GeomFromText('POINT(%s %s)', 4326)"
        namesDB = namesDB + ", pos" 
      first = False
      continue
    
    vals = myValSplit(l.strip())
    pos = []
    for i,n in enumerate(names):
      n = n.replace('"', '')
      if n in posNames:
        pos.append(float(vals[i]))
      if n in realNames:
        if len(vals[i])!=0:
          vals[i] = float(vals[i])
        elif n=="min_transfer_time":
          vals[i] = 0
      if n in intNames:
        if len(vals[i])!=0:
          vals[i] = int(vals[i])
    if len(posNames)!=0:
      vals.append(pos[1])
      vals.append(pos[0])
    #print namesDB
    #print placeHolders
    #print vals
    cursor.execute("INSERT INTO "+tableName+"("+namesDB+") VALUES ("+placeHolders+");", vals)
    num += 1
    if num>1000:
      conn.commit()
      num = 0
  conn.commit()

inputFolder = sys.argv[1]
(schema, prefix) = sys.argv[2].split(".")
db_password = getpass.getpass(f"Enter password for database {db_name}: ")

conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db_name, db_user, db_host, db_password))
cursor = conn.cursor()

cursor.execute(f"""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s_agency');""" % (schema, prefix))
if cursor.fetchall()[0][0]:
  cursor.execute("""DROP TABLE %s.%s_agency;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_calendar;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_calendar_dates;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_routes;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_stop_times;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_stops;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_transfers;""" % (schema, prefix))
  cursor.execute("""DROP TABLE %s.%s_trips;""" % (schema, prefix))
  conn.commit()
  pass
cursor.execute("""CREATE TABLE %s.%s_transfers ( from_stop_id varchar(16),to_stop_id varchar(16),transfer_type smallint,min_transfer_time real,from_trip_id varchar(16),to_trip_id varchar(16),from_route_id varchar(16),to_route_id varchar(16) );""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_stops ( stop_id varchar(12),stop_code varchar(40),stop_name varchar(80),stop_desc text,stop_lat real,stop_lon real,location_type smallint,parent_station varchar(16));""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_agency ( agency_id varchar(8),agency_name text,agency_url text,agency_timezone varchar(40),agency_lang varchar(2),agency_phone varchar(20) );""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_calendar ( service_id varchar(8),monday smallint,tuesday smallint,wednesday smallint,thursday smallint,friday smallint,saturday smallint,sunday smallint,start_date integer,end_date integer );""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_calendar_dates ( service_id varchar(8),date integer,exception_type smallint );""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_routes ( route_id varchar(12),agency_id varchar(8),route_short_name varchar(8),route_long_name varchar(80),route_desc text,route_type smallint,route_url varchar(40),route_color varchar(6),route_text_color varchar(20) );""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_stop_times ( trip_id integer,arrival_time varchar(8),departure_time varchar(8),stop_id varchar(16),stop_sequence smallint,stop_headsign varchar(80),pickup_type smallint,drop_off_type smallint,shape_dist_traveled text);""" % (schema, prefix))
cursor.execute("""CREATE TABLE %s.%s_trips ( route_id varchar(12),service_id varchar(8),trip_id integer,trip_headsign varchar(80),trip_short_name varchar(8),direction_id varchar(8),block_id varchar(8),shape_id varchar(8) );""" % (schema, prefix))
conn.commit()
cursor.execute("""SELECT AddGeometryColumn('%s', '%s_stops', 'pos', 4326, 'POINT', 2);""" % (schema, prefix))
conn.commit()


readImportTable(conn, cursor, "%s.%s_transfers" % (schema, prefix), os.path.join(inputFolder, "transfers.txt"), [], ["min_transfer_time"], ["transfer_type"])
readImportTable(conn, cursor, "%s.%s_stops" % (schema, prefix), os.path.join(inputFolder, "stops.txt"), ["stop_lat", "stop_lon"], ["stop_lat", "stop_lon"], ["location_type"])
readImportTable(conn, cursor, "%s.%s_trips" % (schema, prefix), os.path.join(inputFolder, "trips.txt"), [], [], ["trip_id"])# ["direction_id"])
readImportTable(conn, cursor, "%s.%s_agency" % (schema, prefix), os.path.join(inputFolder, "agency.txt"), [], [], [])
readImportTable(conn, cursor, "%s.%s_calendar" % (schema, prefix), os.path.join(inputFolder, "calendar.txt"), [], [], ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "start_date", "end_date"])
readImportTable(conn, cursor, "%s.%s_calendar_dates" % (schema, prefix), os.path.join(inputFolder, "calendar_dates.txt"), [], [], ["date", "exception_type"])
readImportTable(conn, cursor, "%s.%s_routes" % (schema, prefix), os.path.join(inputFolder, "routes.txt"), [], [], ["route_type"])
readImportTable(conn, cursor, "%s.%s_stop_times" % (schema, prefix), os.path.join(inputFolder, "stop_times.txt"), [], [], ["pickup_type", "drop_off_type", "stop_sequence", "trip_id"])
cursor.execute("""CREATE INDEX ON %s.%s_stop_times (trip_id);"""  % (schema, prefix) )
conn.commit()




