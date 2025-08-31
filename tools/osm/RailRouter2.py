
import psycopg2
import json
from shapely.geometry import shape, Point
from shapely.ops import unary_union
from geopy.distance import geodesic
import osmium
import json
import numpy as np
import folium
#import folium.plugins as plugins
from folium.plugins import HeatMap
import requests
import geopandas as gpd
from sqlalchemy import create_engine
import matplotlib.pyplot as plt
import pandas as pd
import contextily as ctx

from tools.osm.testfile import gdfrailWayFetcher

engine = create_engine('postgresql://bras_ml:2kbV296nCa@vf-athene.intra.dlr.de/project_indikatoren_fuss')

def routedWay(start, end):
    slon, slat = start.split(',')
    elon, elat=end.split(',')
    url="http://localhost:8989/route?point=%s,%s&point=%s,%s&profile=all_tracks&format=json&details=osm_way_id" % (slat, slon, elat, elon)

    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        paths=data.get('paths')[0]
        pathdetails=paths['details']['osm_way_id']
        instructiondetails = paths['instructions']
        overalldetails = paths['paths']
        print(url)
        details = [pathdetails, instructiondetails, overalldetails]
        #print(details)
    else:
        #print(f"Failed to get data: {response.status_code}"+"\n Check if the server is running or the points are close enough to tracks.")
        details=[]
    return details


def odMatrixBuilderGTFS(gtfs_source_trips, gtfs_source_routes, gtfs_source_stop_times, gtfs_source_stops):

    routes = pd.read_csv(gtfs_source_routes, delimiter=',', low_memory=False)
    filtered_routes = routes[routes['route_type'].isin([2, 101, 102, 103, 109])].copy()

    trips = pd.read_csv(gtfs_source_trips, delimiter=',', low_memory=False)
    #TODO: for now, single iteration/trip. CHANGE LATER!!!!!!
    distinct_trips = trips.drop_duplicates(subset='route_id', keep='first')
    trips=distinct_trips.copy()

    stop_times = pd.read_csv(gtfs_source_stop_times, delimiter=',', low_memory=False)
    stops = pd.read_csv(gtfs_source_stops, delimiter=',', low_memory=False)

    route_trips = filtered_routes.merge(trips, on='route_id', how='left')
    route_trips_stoptimes = route_trips.merge(stop_times, on='trip_id', how='left')
    route_trips_stoptimes_stops = route_trips_stoptimes.merge(stops, on='stop_id', how='left')
    start = route_trips_stoptimes_stops.copy()
    start.index = range(1, 1 + len(start))
    start = start.iloc[:-1]
    end = route_trips_stoptimes_stops.copy()
    end.index = range(0, 0 + len(end))
    end = end.iloc[1:]
    end = end.drop(['shape_id', 'route_id', 'agency_id', 'route_short_name', 'route_long_name', 'route_type', 'route_color', 'route_text_color', 'route_desc', 'service_id', 'trip_headsign', 'trip_short_name', 'direction_id', 'block_id', 'wheelchair_accessible', 'bikes_allowed'], axis=1)
    end = end.drop(['stop_headsign', 'pickup_type', 'drop_off_type'], axis=1)

    combined = pd.merge(start, end, left_index=True, right_index=True, suffixes=('_start', '_end'))
    combined_filter = combined[combined['trip_id_start'] == combined['trip_id_end']]
    combined_filter = combined_filter.reset_index(drop=True)
    combined = combined_filter.copy()


    print(filtered_routes)
    print(combined)
    print(combined.keys())

    return combined

def routeToArray(route):
    routeIdOnly = route[0]
    way_ids_list = []
    for i in range(len(routeIdOnly)):

        way_ids_list.append(route[i][2])
    return way_ids_list

def wayTime(route, row):
    departure= row['departure_time_start']
    arrival = row['arrival_time_end']
    routedtime = route[2]['time']
    routeddistance = route[2]['distance']

    way_ids = route[0]
    way_desc = route[1]
    waystarttime = departure
    for way in way_desc:
        interval = way['interval']
        interval_ids=[]
        for i in range(interval[0], interval[1]+1):
            for start, end, id in way_ids:
                if start<=i<=end:
                    if id in interval_ids:
                        pass
                    else:
                        interval_ids.append(id)
                    break
        print(interval_ids)

def tableCreator(cursor, tablename):
    cursor.execute("""DROP TABLE if exists %s;""" % tablename)
    conn.commit()
    cursor.execute("""CREATE TABLE %s (
                        route_id text,
                        way_id text, 
                        id_start bigint,
                        route_type text,
                        trip_headsign text,
                        stationname_start text,
                        latlon_start text,
                        lonlat_start text,
                        id_end bigint,
                        stationname_end text,
                        lonlat_end text,  
                        latlon_end text,
                        way_ids integer[]
                );""" % tablename )


def matrixRouter(tablename, cursor, conn, gtfs_source_trips, gtfs_source_routes, gtfs_source_stop_times, gtfs_source_stops):
    tableCreator(cursor, tablename)
    odmatrix = odMatrixBuilderGTFS(gtfs_source_trips, gtfs_source_routes, gtfs_source_stop_times, gtfs_source_stops)
    print(type(odmatrix))

    for index, row in odmatrix.iterrows():
        try:
            routed_way = row
            route_id=row['route_id']
            way_id = str(row['trip_id_start'])+'_'+str(row['stop_id_start'])
            start = str(row['stop_lon_start'])+','+str(row['stop_lat_start'])
            end = str(row['stop_lon_end']) + ',' + str(row['stop_lat_end'])
            route=routedWay(start, end)
            #print(route)
            #print(row)
            #print(type(route[1]))


            trip_headsign = str(row['trip_headsign'])
            latlon_start = str(row['stop_lat_start'])+','+str(row['stop_lon_start'])
            latlon_end = str(row['stop_lat_end']) + ',' + str(row['stop_lon_end'])
            route_type= str(row['route_type'])



            way_ids = routeToArray(route)

            wayTime(route, row)


            #print(way_ids)
            cursor.execute(
                """INSERT INTO %s(route_id, route_type, trip_headsign, way_id, lonlat_start, latlon_start, lonlat_end, latlon_end, way_ids) 
                VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', ARRAY%s::integer[]);
                """ % (tablename, route_id, route_type, trip_headsign, way_id, start, latlon_start, end, latlon_end, way_ids))
            conn.commit()

        except AttributeError:
            cursor.execute(
                """INSERT INTO %s(route_id, stationname_start, way_id, route_type, trip_headsign, latlon_start, latlon_end) VALUES ('%s', 'ATTRIBUTEERROR', '%s', '%s','%s','%s','%s'); """ % (tablename, route_id, way_id, route_type, trip_headsign, latlon_start, latlon_end))
            conn.commit()
        except IndexError:
            try:
                cursor.execute(
                    """INSERT INTO %s(route_id, stationname_start, way_id, route_type, trip_headsign, latlon_start, latlon_end) VALUES ('%s', 'INDEXERROR', '%s', '%s','%s','%s','%s'); """ % (tablename, route_id, way_id, route_type, trip_headsign, latlon_start, latlon_end))
                conn.commit()
            except psycopg2.errors.SyntaxError:
                cursor.execute(
                    """INSERT INTO %s(route_id, stationname_start, way_id, route_type, trip_headsign, latlon_start, latlon_end) VALUES ('%s', 'INDEXERROR', '%s', '%s','%s','%s'); """ % (
                        tablename, route_id, way_id, route_type, latlon_start, latlon_end))
                conn.commit()
        except psycopg2.errors.SyntaxError:
            cursor.execute(
                """INSERT INTO %s(route_id, stationname_start, way_id, route_type, trip_headsign, latlon_start, latlon_end) VALUES ('%s', 'INDEXERROR', '%s', '%s','%s','%s'); """ % (tablename, route_id, way_id, route_type, latlon_start, latlon_end))
            conn.commit()







login='project_indikatoren_fuss,bras_ml,vf-athene.intra.dlr.de,2kbV296nCa'

gtfs_source_trips = "gtfs\\trips.txt"
gtfs_source_routes = "gtfs\\routes.txt"
gtfs_source_stop_times = "gtfs\\stop_times.txt"
gtfs_source_stops = "gtfs\\stops.txt"

tablename='osmextract.odmatrix_germany_gtfsn_eu'


db, user, host, password = login.split(',')
conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor = conn.cursor()


#matrixRouter(tablename, cursor, conn, gtfs_source_trips, gtfs_source_routes, gtfs_source_stop_times, gtfs_source_stops)
#odMatrixBuilderGTFS(gtfs_source_trips, gtfs_source_routes, gtfs_source_stop_times, gtfs_source_stops)








#http://localhost:8989/route?point=51.312559,9.447116&point=50.969568,9.797788&profile=all_tracks&format=json&details=osm_way_id
kasselbebra = [[[0, 5, 59239159], [5, 6, 132802727], [6, 7, 104511313], [7, 8, 132781617], [8, 9, 135630254], [9, 10, 421670732], [10, 15, 135651557], [15, 19, 368994795], [19, 25, 104360741], [25, 26, 104360744], [26, 29, 69766029], [29, 30, 69766026], [30, 31, 104360757], [31, 32, 1159327115], [32, 33, 277583436], [33, 34, 66023215], [34, 52, 533542664], [52, 116, 11838614], [116, 128, 267625764], [128, 129, 26480046], [129, 130, 26480045], [130, 131, 412754253], [131, 132, 426323815], [132, 142, 104302745], [142, 143, 267625773], [143, 144, 104302741], [144, 159, 104302735], [159, 161, 1044576424], [161, 162, 1044576422], [162, 183, 267625776], [183, 184, 104309495], [184, 189, 104309507], [189, 190, 104309508], [190, 192, 104309502], [192, 193, 104309497], [193, 226, 104309510], [226, 227, 104309492], [227, 254, 104309512], [254, 255, 235057022], [255, 379, 235057020], [379, 380, 104309501], [380, 387, 104309511], [387, 392, 267625779], [392, 393, 1150579805], [393, 397, 104309493], [397, 400, 104309505], [400, 401, 104309488], [401, 409, 104309509], [409, 411, 104309503], [411, 412, 104309506], [412, 430, 104309491], [430, 431, 104311535], [431, 432, 104311520], [432, 433, 104311527], [433, 467, 104311513], [467, 468, 104311518], [468, 470, 104311503], [470, 471, 128823366], [471, 489, 128823364], [489, 490, 104311525], [490, 492, 104311510], [492, 493, 194663503], [493, 511, 194663502], [511, 512, 104288623], [512, 513, 1174075176], [513, 514, 1174075177], [514, 515, 309095689], [515, 516, 26362871], [516, 517, 26362872], [517, 525, 47387074], [525, 529, 47387073], [529, 570, 267625778], [570, 583, 267625765], [583, 585, 309095691], [585, 587, 104255599], [587, 588, 104255615], [588, 640, 309095694], [640, 642, 899151122], [642, 680, 240789809], [680, 681, 25923172], [681, 682, 25923171], [682, 693, 44013306], [693, 694, 44013305], [694, 699, 899151121], [699, 713, 309033496], [713, 716, 104246574], [716, 734, 134358294], [734, 735, 669017621], [735, 736, 451905758], [736, 738, 134497181], [738, 739, 310575601], [739, 742, 137372561]], [{'distance': 499.772, 'heading': 158.49, 'sign': 0, 'interval': [0, 5], 'text': 'Continue', 'time': 25703, 'street_name': ''}, {'street_ref': '3900', 'distance': 1624.028, 'sign': 0, 'interval': [5, 15], 'text': 'Continue onto Main-Weser-Bahn', 'time': 54570, 'street_name': 'Main-Weser-Bahn'}, {'street_ref': '3900', 'distance': 7710.925, 'sign': 7, 'interval': [15, 132], 'text': 'Keep right onto Main-Weser-Bahn', 'time': 243013, 'street_name': 'Main-Weser-Bahn'}, {'street_ref': '6340', 'distance': 46.08, 'sign': -7, 'interval': [132, 133], 'text': 'Keep left onto Friedrich-Wilhelms-Nordbahn', 'time': 2552, 'street_name': 'Friedrich-Wilhelms-Nordbahn'}, {'street_ref': '6340', 'distance': 16550.895, 'sign': -7, 'interval': [133, 414], 'text': 'Keep left onto Friedrich-Wilhelms-Nordbahn', 'time': 602363, 'street_name': 'Friedrich-Wilhelms-Nordbahn'}, {'street_ref': '6340', 'distance': 5429.763, 'sign': -7, 'interval': [414, 512], 'text': 'Keep left onto Friedrich-Wilhelms-Nordbahn', 'time': 195472, 'street_name': 'Friedrich-Wilhelms-Nordbahn'}, {'distance': 72.957, 'sign': -7, 'interval': [512, 514], 'text': 'Keep left', 'time': 4378, 'street_name': ''}, {'street_ref': '6340', 'distance': 19232.519, 'sign': 0, 'interval': [514, 713], 'text': 'Continue onto Friedrich-Wilhelms-Nordbahn', 'time': 635940, 'street_name': 'Friedrich-Wilhelms-Nordbahn'}, {'distance': 92.615, 'sign': 7, 'interval': [713, 716], 'text': 'Keep right', 'time': 5557, 'street_name': ''}, {'street_ref': '6340', 'distance': 14.987, 'sign': 0, 'interval': [716, 717], 'text': 'Continue onto Friedrich-Wilhelms-Nordbahn', 'time': 540, 'street_name': 'Friedrich-Wilhelms-Nordbahn'}, {'street_ref': '6340', 'distance': 2448.161, 'sign': -7, 'interval': [717, 736], 'text': 'Keep left onto Friedrich-Wilhelms-Nordbahn', 'time': 87563, 'street_name': 'Friedrich-Wilhelms-Nordbahn'}, {'street_ref': '6340', 'distance': 93.821, 'sign': -7, 'interval': [736, 737], 'text': 'Keep left onto 6340', 'time': 2937, 'street_name': ''}, {'street_ref': '6340', 'distance': 67.536, 'sign': 7, 'interval': [737, 738], 'text': 'Keep right onto 6340', 'time': 2114, 'street_name': ''}, {'distance': 332.582, 'sign': 7, 'interval': [738, 742], 'text': 'Keep right', 'time': 13457, 'street_name': ''}, {'distance': 0.0, 'sign': 4, 'last_heading': 150.87697619179968, 'interval': [742, 742], 'text': 'Arrive at destination', 'time': 0, 'street_name': ''}]]





#pd.set_option('display.max_columns', None)
#result = odMatrixBuilder(source)
#print(result.head())