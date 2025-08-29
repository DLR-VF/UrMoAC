


import osmium
import psycopg2
import pandas as pd
import geopandas as gpd


def directionAdder(cursor, oid):
    query = """SELECT pseudodir 
            FROM osmextract.test_berlin_railway_network
            WHERE oid LIKE '%%%s%%'""" % oid
    cursor.execute(query)
    prefdir = cursor.fetchall()
    if prefdir:
        prefdir = prefdir[0][0]
    else:
        prefdir=""

    print(prefdir)
    print(type(prefdir))
    return prefdir

class OSMHandler(osmium.SimpleHandler):
    def __init__(self):
        super().__init__()
        self.modified_ways =[]
        # implement methods to handle nodes, ways, relations




    def node(self, n):
        # Handle node here
        #print(f"Node id: {n.id}")
        pass

    def way(self, w):
        # Handle way here
        #print(f"Way id: {w.id}")
        if 'railway' in w.tags:
            # Convert way to dict to modify tags (pyosmium objects are immutable)
            tags = {tag.k: tag.v for tag in w.tags}

            # Add or update your tag
              # Example value

            oid = w.id

            tags['preferred_direction'] = directionAdder(cursor, oid)


            # Store way info (id, nodes, updated tags) for later
            way_info = {
                'id': w.id,
                'nodes': [node.ref for node in w.nodes],
                'tags': tags
            }
            print(way_info)
            self.modified_ways.append(way_info)

    def relation(self, r):
        # Handle relation here
        #print(f"Relation id: {r.id}")
        pass





login='project_indikatoren_fuss,bras_ml,vf-athene.intra.dlr.de,2kbV296nCa'
source='osmextract.test_berlin_railway'
tablename='osmextract.odmatrix_sachsen'


db, user, host, password = login.split(',')
conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
cursor = conn.cursor()

#handler = OSMHandler()
#handler.apply_file("C:\\git\\berlin-latest.osm.pbf")