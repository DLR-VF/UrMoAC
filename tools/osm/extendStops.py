#!/usr/bin/env python
# =========================================================
# extendStops.py
# @author Daniel Krajzewicz
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Sets the information about lines passing the stops
# =========================================================
import psycopg2, sys, os.path

conn = psycopg2.connect("dbname='XXX' user='XXX' host='XXX' password='XXX'")
cursor = conn.cursor()

route2line = {}
cursor.execute("SELECT route_id,route_short_name from public.berlin_gtfs20171216_routes;")
for t in cursor.fetchall():
  route2line[t[0]] = t[1]
trip2route = {}
cursor.execute("SELECT trip_id,route_id from public.berlin_gtfs20171216_trips;")
for t in cursor.fetchall():
  trip2route[t[0]] = t[1]
stop2lines = {}
cursor.execute("SELECT trip_id,stop_id from public.berlin_gtfs20171216_stop_times;")
for t in cursor.fetchall():
  routeID = trip2route[t[0]]
  line = route2line[routeID]
  if t[1] not in stop2lines:
    stop2lines[t[1]] = set()
  stop2lines[t[1]].add(line)


cursor.execute("ALTER TABLE public.berlin_gtfs20171216_stops ADD lines text")
conn.commit()
for s in stop2lines:
  cursor.execute("UPDATE public.berlin_gtfs20171216_stops SET lines='%s' WHERE stop_id='%s';" % (",".join(stop2lines[s]), s))
conn.commit()
   
