import psycopg2
import json
from shapely.geometry import shape, Point
from geopy.distance import geodesic
import osmium
import json
import numpy as np
import folium
#import folium.plugins as plugins
from folium.plugins import HeatMap

def GeoJSONTest():

    conn = psycopg2.connect(" dbname=project_indikatoren_fuss user=bras_ml host=vf-athene.intra.dlr.de password=2kbV296nCa")
    cur = conn.cursor()

    #cur.execute("""DROP TABLE osmextract.geojsontest;""" )
    #conn.commit()
    cur.execute("""CREATE TABLE osmextract.geojsontest3 (
        id bigint,
        v text,
        sub_stops text[] 
    );""" )
    cur.execute(
        """SELECT AddGeometryColumn('osmextract', 'geojsontest3', 'geom', 4326, 'POINT', 2);""" )
    conn.commit()



    cur.execute("""
                SELECT json_build_object(
            'type',     'FeatureCollection',
            'features', json_agg(features.feature)
        )
        FROM (
            SELECT json_build_object(
                'type',       'Feature',
                'geometry',   ST_AsGeoJSON(geom)::json,
                'properties', to_jsonb(row) - 'geom'
            ) AS feature
            FROM 
                
    
        (SELECT DISTINCT loc.id as id, loc.k as k, loc.v as v, loc.geom as geom
                        FROM
                            (Select tags.id as id, tags.k as k, tags.v as v, node.pos as geom
                            FROM
                                (SELECT id, k, v
                                FROM osm.germany20250516_ntag
                                where k = 'railway' and (v = 'station' or v = 'halt' or v = 'stop')) as tags
                            LEFT JOIN osm.germany20250516_node as node
                            ON tags.id = node.id) as loc
                        INNER JOIN osmextract.test_berlin_railway_network as network
                        on ST_DWithin(loc.geom, network.geom, 0.0002))
    
                        
                        row
        ) features;
                        """)

    geojson_result = cur.fetchone()[0]

    #print(json.dumps(geojson_result, indent=2))

    features = geojson_result['features']

    points = []
    for feature in features:
        geom = shape(feature['geometry'])
        properties = feature['properties']
        points.append((properties['id'], properties['k'], properties['v'], geom))

    radius_m = 200
    seen = []
    for st in ["station", "stop", "halt"]:
        for id1, k1, v1, point1 in points:
            if id1 in seen:
                    continue

            elif v1 == st:
                sub_stops = []
                for id2, k2, v2, point2 in points:
                    if id1 == id2:
                        continue
                    else:
                        dist = geodesic(
                            (point1.y, point1.x),
                            (point2.y, point2.x)
                        ).meters
                        if dist <= radius_m:
                            sub_stops.append(id2)
                            if v1 != "station":
                                seen.append(id2)
                cur.execute(
                    "INSERT INTO osmextract.geojsontest(id, v, sub_stops, geom) VALUES(%s, '%s', ARRAY%s::bigint[], ST_GeomFromText('%s', 4326));" % (
                        id1, v1, sub_stops, point1))
            else:
                continue
        conn.commit()
    cur.close()
    conn.close()
    #osmextract.toast_hamburg_railway_stops

#GeoJSONTest()

def dictTest():
    kvpairs = {
        "amenity_clinic": {},
        "amenity_doctors": {},
        "amenity_hospital": {},
        "amenity_dentist": {},
        "amenity_pharmacy": {}
    }

    kvpairlist=list(kvpairs.keys())
    for i in range(len(kvpairlist)):
        kvpairlist[i] = kvpairlist[i].split("_")
    print(kvpairlist[1])
#dictTest()

def writer(filename):
    with open(filename, "w") as fd:
        for i in [1,2,3]:
            fd.write(str(i)+"\n")

#writer(r"C:\git\UrMoAC\tools\osm\blibla.txt")

def stationLineAdder(stationID):
    conn = psycopg2.connect(" dbname=project_indikatoren_fuss user=bras_ml host=vf-athene.intra.dlr.de password=2kbV296nCa")
    cur = conn.cursor()
    linelist=[]
    cur.execute(
        """SELECT sub_stops from osmextract.test_hamburg_railway_stations where id ='%s';""" % stationID)
    stoplist=cur.fetchall()[0][0]
    for substops in stoplist:
        cur.execute(
        """Select rid from osm.germany20250516_member where elemid='%s';""" % substops)
        rels = cur.fetchall()
        for rel in rels:
            cur.execute("""
            SELECT v FROM
            osm.germany20250516_rtag
            where id = '%s' and k = 'ref';""" % rel)
            lineslist = cur.fetchall()
            if lineslist:
                if lineslist[0][0] not in linelist:
                    linelist.append(lineslist[0][0])
    print(linelist)
    return linelist


#stationLineAdder('747855975')




class GeoJsonWriter(osmium.SimpleHandler):
    def __init__(self, filename):
        super().__init__()
        self.file = open(filename, 'w')
        self.file.write('{"type": "FeatureCollection", "features": [\n')
        self.first = True

    def write_feature(self, feature):
        if not self.first:
            self.file.write(',\n')
        else:
            self.first = False
        self.file.write(json.dumps(feature))

    def node(self, n):
        # Convert OSM node to GeoJSON Point
        feature = {
            "type": "Feature",
            "id": f"node/{n.id}",
            "geometry": {
                "type": "Point",
                "coordinates": [n.location.lon, n.location.lat]
            },
            "properties": dict(n.tags)
        }
        self.write_feature(feature)

    def way(self, w):
        # Convert OSM way to GeoJSON LineString or Polygon
        coords = []
        for node in w.nodes:
            if not node.location.valid():
                return  # skip invalid ways
            coords.append([node.location.lon, node.location.lat])

        if len(coords) < 2:
            return  # not enough points for geometry

        geom_type = "LineString"
        if len(coords) > 3 and coords[0] == coords[-1]:
            geom_type = "Polygon"
            coords = [coords]  # Polygon requires list of linear rings

        feature = {
            "type": "Feature",
            "id": f"way/{w.id}",
            "geometry": {
                "type": geom_type,
                "coordinates": coords
            },
            "properties": dict(w.tags)
        }
        self.write_feature(feature)

    def close(self):
        self.file.write('\n]}\n')
        self.file.close()


#GeoJSONhandler = GeoJsonWriter(r"C:\git\berlin-latest.geojson")
#GeoJSONhandler.apply_file(r"C:\git\berlin-latest.osm.pbf")
#GeoJSONhandler.close()


def storeAreaCalculator(conn, cursor, schema, prefix, dschema, name, region):
    #cursor.execute("""ALTER TABLE %s.%s_%s
     #                   ADD COLUMN buildingcat text;
     #                   ALTER TABLE %s.%s_%s
    #                    ADD COLUMN share decimal;
    #
    #              """  % (schema, name, region, schema, name, region))
   # conn.commit()
    query = """SELECT * FROM %s.%s_%s""" % (dschema, name, region)

    cursor.execute(query)
    shoplist=cursor.fetchall()
    if shoplist:
        #print(shoplist)
        for shop in shoplist:
            #print(shop)
            if shop[4] != '0106000020E610000000000000':
                #print(shop[4])
                continue
            else:
                id=shop[0]
                query = """SELECT *
                            FROM
                            %s.buildings_%s
                            WHERE
                            ST_Contains(polygon, (
                                SELECT centroid
                            FROM %s.%s_%s
                            WHERE id = '%s'));""" % (dschema, region, schema, name, region, id)
                #print(query)
                cursor.execute(query)
                building = cursor.fetchone()
                if building:
                    print(id)
                    print(building[0])
                    cursor.execute("""SELECT * FROM %s.buildings_%s_types
                                        where id = '%s'""" %(dschema, region, building[0]))
                    buildingtags = cursor.fetchone()


                    cursor.execute("""
                                    SELECT COUNT (contain.id)
                                    FROM (SELECT node.id as id
                                            FROM (	SELECT *
                                                    FROM %s.buildings_%s
                                                    where id = '%s')as building
                                            INNER JOIN %s.%s_node as node
                                            ON ST_WITHIN(node.pos, building.polygon)) as contain
                                    LEFT JOIN %s.%s_ntag as tags
                                    ON contain.id = tags.id
                                    where tags.k = 'shop'
                    """ % (dschema, region, building[0], schema, prefix, schema, prefix))
                    shopcount=cursor.fetchone()
                    print(buildingtags[2])
                    print(shopcount)
                    print(building[4])
                    """if shopcount[0] <2:
                        shopshare=0.8
                    elif shopcount[0] <6:
                        shopshare = 0.5
                    else:
                        shopshare = 0.2
                    print(shopshare)"""
                    cursor.execute( """ UPDATE %s.%s_%s
                                        SET  buildingcat = '%s', share = '%s', polygon = ST_GeomFromEWKB(decode('%s', 'hex'))
                                        WHERE id = '%s'""" % (dschema, name, region, buildingtags[2], shopcount[0], building[4], id ))
                    conn.commit()
        #TODO Marlon lieber würde ich hier committen, aber für testing lieber oben
        #conn.commit()

#conn = psycopg2.connect(" dbname=project_indikatoren_fuss user=bras_ml host=vf-athene.intra.dlr.de password=2kbV296nCa")
#cursor = conn.cursor()

#storeAreaCalculator(conn, cursor, 'osm', 'germany20250516', 'osmextract', 'supermarkets', 'berlin')
#shoplist = [1,2,3,4,5,6,7]
#i=0
#while i <101:
#    i+=1
#    if i % 20 == 0:
#        print(str(i)+' out of '+ str(len(shoplist)) + ' locations examined.')

#cluster = folium.FeatureGroup(name='cluster')
#cluster.add_child(plugins.MarkerCluster(locations=coords, popups=popups)


data = np.load('data/hzy-901_DEU_Dusseldorf.npz', allow_pickle = True)

"""print(data.files)  # Lists all keys in the npz file

grid_coords = data['grid']
print(type(grid_coords))
print(grid_coords.shape)
print(grid_coords)"""


grid = data['grid'].item()  # assuming saved as dict inside npz
population = data['pop']  # adjust key if different
"""

# Prepare data points for heatmap
heat_data = []
for key, coord in grid.items():
    # coord is [lon, lat]
    lat, lon = coord[1], coord[0]
    pop = population[int(key)]  # if keys are string indexes matching population array index
    heat_data.append([lat, lon, pop])

# Create folium map, centered on average location
center_lat = sum([pt[0] for pt in heat_data]) / len(heat_data)
center_lon = sum([pt[1] for pt in heat_data]) / len(heat_data)
m = folium.Map(location=[center_lat, center_lon], zoom_start=10)

# Add heatmap layer
HeatMap(heat_data).add_to(m)

# Save or show map
m.save('heatmap.html')"""

# Confirm alignment
print("Grid size:", len(grid))
print("Population size:", len(population))

# Safe population mapping (align by index)
"""heat_data = []
for i, key in enumerate(sorted(grid.keys(), key=int)):
    if i >= len(population):
        print(f"Skipping grid cell {key} (no corresponding population value)")
        continue

    coord = grid[key]
    lon, lat = coord
    pop = population[i]

    heat_data.append([lat, lon, pop])



center_lat = sum([pt[0] for pt in heat_data]) / len(heat_data)
center_lon = sum([pt[1] for pt in heat_data]) / len(heat_data)
m = folium.Map(location=[center_lat, center_lon], zoom_start=10)



# Filter out bad entries safely
clean_heat_data = []
for pt in heat_data:
    if (isinstance(pt, (list, tuple)) and
        len(pt) == 3 and
        all(isinstance(x, (int, float)) and not np.isnan(x) for x in pt)):
        clean_heat_data.append(pt)
    else:
        print(f"Skipping bad point: {pt}")


# Add heatmap layer
HeatMap(clean_heat_data).add_to(m)

# Save to file
m.save('population_heatmap.html')"""

print(population.shape)

num_rows, num_cols = population.shape  # 22, 22

heat_data = []
for key, coord in grid.items():
    idx = int(key)
    row = idx // num_cols
    col = idx % num_cols

    pop = population[row, col]

    lat, lon = coord[1], coord[0]
    heat_data.append([lat, lon, pop])

center_lat = sum(pt[0] for pt in heat_data) / len(heat_data)
center_lon = sum(pt[1] for pt in heat_data) / len(heat_data)

m = folium.Map(location=[center_lat, center_lon], zoom_start=12)
HeatMap(heat_data).add_to(m)
m.save('population_heatmap.html')