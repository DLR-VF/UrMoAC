#!/usr/bin/env python
# =========================================================
# importGTFS.py
# @author Daniel Krajzewicz
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Imports a given GTFS data set
# =========================================================
import psycopg2, sys, os.path, io


def myValSplit(line):
  vals = line.split(",")
  nVals = []
  hadQuotes = False
  for v in vals:
    #v2 = v.strip()
    v2 = v
    if hadQuotes:
      nVals[-1] = nVals[-1] + "," + v2
      if len(v2)>0 and v2[-1]=='"':
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
  fd = io.open(fileName, 'r', encoding='utf8')
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
        else:
          vals[i] = -1
    if len(posNames)!=0:
      vals.append(pos[1])
      vals.append(pos[0])
    cursor.execute("INSERT INTO "+tableName+"("+namesDB+") VALUES ("+placeHolders+");", vals)
    num += 1
    if num>10000:
      conn.commit()
      num = 0
  conn.commit()

# parse command line parameter
if len(sys.argv)<3:
  print ("importGTFS.py <INPUT_FOLDER> <TARGET_DB_DEFINITION>")
  print ("  where <TARGET_DB_DEFINITION> is <HOST>;<DB>;<SCHEMA>;TABLE_PREFIX>;<USER>;<PASSWD>")
  sys.exit()
# - input folder and files
inputFolder = sys.argv[1]
if not os.path.exists(inputFolder):
  print ("The folder '" + inputFolder + "' to import GTFS from does not exist.")
  sys.exit()
error = False
for f in ["agency", "calendar", "calendar_dates", "routes", "stop_times", "stops", "trips"]:
  fn = os.path.join(inputFolder, f+".txt")
  if not os.path.exists(fn):
    print ("The mandatory file '" + fn + "' to import GTFS from does not exist.")
    error = True
if error:
  sys.exit()
# - output db
if len(sys.argv[2].split(";"))<6:
  print ("Missing values in target database definition; should be: <HOST>;<DB>;<SCHEMA>;TABLE_PREFIX>;<USER>;<PASSWD>")
  sys.exit()
(host, dbName, schema, tableName, user, password) = sys.argv[2].split(";")

# connect to db
conn = psycopg2.connect("host='%s' dbname='%s' user='%s' password='%s'" % (host, dbName, user, password))
cursor = conn.cursor()

# build tables
cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s_agency');""" % (schema, tableName))
if cursor.fetchall()[0][0]:
  print ("Removing old tables")
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_agency;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_calendar;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_calendar_dates;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_routes;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_stop_times;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_stops;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_transfers;""" % (schema, tableName))
  cursor.execute("""DROP TABLE IF EXISTS %s.%s_trips;""" % (schema, tableName))
  conn.commit()
  pass
print ("Building tables")
cursor.execute("""CREATE TABLE %s.%s_transfers ( from_stop_id varchar(16),to_stop_id varchar(16),transfer_type smallint,min_transfer_time real,from_trip_id varchar(16),to_trip_id varchar(16),from_route_id varchar(16),to_route_id varchar(16) );""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_stops ( stop_id varchar(12),stop_code varchar(40),stop_name varchar(80),stop_desc text,stop_lat real,stop_lon real,location_type smallint,parent_station varchar(16));""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_agency ( agency_id varchar(8),agency_name text,agency_url text,agency_timezone varchar(40),agency_lang varchar(2),agency_phone varchar(20) );""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_calendar ( service_id varchar(8),monday smallint,tuesday smallint,wednesday smallint,thursday smallint,friday smallint,saturday smallint,sunday smallint,start_date integer,end_date integer );""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_calendar_dates ( service_id varchar(8),date integer,exception_type smallint );""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_routes ( route_id varchar(12),agency_id varchar(8),route_short_name varchar(8),route_long_name varchar(80),route_desc text,route_type smallint,route_url varchar(40),route_color varchar(6),route_text_color varchar(20) );""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_stop_times ( trip_id integer,arrival_time varchar(8),departure_time varchar(8),stop_id varchar(16),stop_sequence smallint,stop_headsign varchar(80),pickup_type smallint,drop_off_type smallint,shape_dist_traveled text);""" % (schema, tableName))
cursor.execute("""CREATE TABLE %s.%s_trips ( route_id varchar(12),service_id varchar(8),trip_id integer,trip_headsign varchar(80),trip_short_name varchar(8),direction_id varchar(8),block_id varchar(8),shape_id varchar(8) );""" % (schema, tableName))
conn.commit()
cursor.execute("""SELECT AddGeometryColumn('%s', '%s_stops', 'pos', 4326, 'POINT', 2);""" % (schema, tableName))
conn.commit()

# importing data
print ("Importing data")
if os.path.exists(os.path.join(inputFolder, "transfers.txt")):
  readImportTable(conn, cursor, "%s.%s_transfers" % (schema, tableName), os.path.join(inputFolder, "transfers.txt"), [], ["min_transfer_time"], ["transfer_type"])
readImportTable(conn, cursor, "%s.%s_stops" % (schema, tableName), os.path.join(inputFolder, "stops.txt"), ["stop_lat", "stop_lon"], ["stop_lat", "stop_lon"], ["location_type"])
readImportTable(conn, cursor, "%s.%s_trips" % (schema, tableName), os.path.join(inputFolder, "trips.txt"), [], [], ["trip_id"])# ["direction_id"])
readImportTable(conn, cursor, "%s.%s_agency" % (schema, tableName), os.path.join(inputFolder, "agency.txt"), [], [], [])
readImportTable(conn, cursor, "%s.%s_calendar" % (schema, tableName), os.path.join(inputFolder, "calendar.txt"), [], [], ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "start_date", "end_date"])
readImportTable(conn, cursor, "%s.%s_calendar_dates" % (schema, tableName), os.path.join(inputFolder, "calendar_dates.txt"), [], [], ["date", "exception_type"])
readImportTable(conn, cursor, "%s.%s_routes" % (schema, tableName), os.path.join(inputFolder, "routes.txt"), [], [], ["route_type"])
readImportTable(conn, cursor, "%s.%s_stop_times" % (schema, tableName), os.path.join(inputFolder, "stop_times.txt"), [], [], ["pickup_type", "drop_off_type", "stop_sequence", "trip_id"])
cursor.execute("""CREATE INDEX ON %s.%s_stop_times (trip_id);"""  % (schema, tableName) )
conn.commit()

# extending stops by lines
print ("Extending stops by lines")
route2line = {}
cursor.execute("SELECT route_id,route_short_name from %s.%s_routes;" % (schema, tableName))
for t in cursor.fetchall():
  route2line[t[0]] = t[1]
trip2route = {}
cursor.execute("SELECT trip_id,route_id from %s.%s_trips;" % (schema, tableName))
for t in cursor.fetchall():
  trip2route[t[0]] = t[1]
stop2lines = {}
cursor.execute("SELECT trip_id,stop_id from %s.%s_stop_times;" % (schema, tableName))
for t in cursor.fetchall():
  routeID = trip2route[t[0]]
  line = route2line[routeID]
  if t[1] not in stop2lines:
    stop2lines[t[1]] = set()
  stop2lines[t[1]].add(line)
cursor.execute("ALTER TABLE %s.%s_stops ADD lines text" % (schema, tableName))
conn.commit()
for s in stop2lines:
  cursor.execute("UPDATE %s.%s_stops SET lines='%s' WHERE stop_id='%s';"  % (schema, tableName, ",".join(stop2lines[s]), s))
conn.commit()


