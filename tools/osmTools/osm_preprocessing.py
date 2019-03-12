'''

#===================================================
osm_preprocessing.py
@author Simon Nieland
@date 08.09.2017
@copyright Institut fuer Verkehrsforschung, 
            Deutsches Zentrum fuer Luft- und Raumfahrt
@brief preprocessing tool for OpenStreetMap indicators
# =========================================================
'''


import argparse
from sqlalchemy import engine
from osmDb import osm_facilities
from osmDb import osm_streets
from osmDb import osm_polygons
from dataimport import osm2db
#import osmDb.osm_net_indicators as netindicators
import sys




if __name__ == '__main__':
    
    
    parser = argparse.ArgumentParser(description='Tool for OSM pre-processing')
    parser.add_argument("-db", "--dbname",
                  help="Name of PostGIS database " , required=True)
    
    parser.add_argument("-host", "--host", dest="host",
                  help="PostGIS database host", required=True)
    
    parser.add_argument("-u", "--user",
                  help="PostGIS database Username", required=True)
    
    parser.add_argument("-psw", "--password",
                  help="PostGIS database Password", required=True)
    
    parser.add_argument("-l", "--location",
                  help="Location of osm input. E.g. 'berlin'", required=True)
    
    parser.add_argument("-out-schema", "--out_schema",
                  help="Schema of ouput osm tables", required=True)
    
    parser.add_argument("-srid", 
                  help="Schema aggregation unit. E.g. statistical units" , required=True)
    
    parser.add_argument("-osm", "--osm_file", 
                  help="OSM input xml file")
    
    parser.add_argument("-b", "--buildings", action='store_true', default=False,
                  help="create OSM buildings")
    
    parser.add_argument("-net-ind", "--network_indicators", action='store_true', default=False,
                  help="create OSM buildings")
    
    parser.add_argument("-facility", "--facility", action='store_true', default=False,
                  help="Calculate osm facilities and density per aggregation unit")
    
    parser.add_argument("-crossroad-density", "--crossroad_density", action='store_true', default=False,
                  help="calculate crossroad density per aggregation unit")
    
    parser.add_argument("-net", "--network",action='store_true', default=False,
                  help="create OSM road network")
    
    parser.add_argument("-agg-schema", "--agg_schema",
                  help="schema aggregation unit")
    
    parser.add_argument("-agg-table", "--agg_table",
                  help="table aggregation unit")
    
    parser.add_argument("-agg-geom", "--agg_geom",
                  help="geometry field of aggregation unit. E.g. 'geom'")
    
    parser.add_argument("-agg-id", "--agg_id",
                  help="id field of aggregation unit. E.g. sg_id")
    
    
    parser.add_argument("-if-exist",  default='fail',
                  help="""  fail: If table exists, do nothing.
                            replace: If table exists, drop it, recreate it, and insert data.
                            append: If table exists, insert data. Create if does not exist.""")
    
    parser.add_argument("-pop-col", "--population-column",#, dest="srid",
                  help="Schema aggregation unit. E.g. statistical units", default=None)
    
    parser.add_argument("-v", "--verbose", default=0,#, dest="verbose", default=0,
                  help="print status messages to stdout")
    
    args = parser.parse_args()


    db_table_name = args.location+'_facilities'  
    network_table = args.location+"_network"
    network_table_street_column = "street_type"
    geom_field_network_table = "the_geom" 
    
    print "Connecting to the db..."

    
    try:
        engine = engine.create_engine('postgresql://%s:%s@%s:5432/%s'%(args.user, args.password, args.host,args.dbname))
        conn = engine.connect()
    except Exception, e:
        print e
        sys.exit()
    
    if args.osm_file:
        print 'importing osm data...'
        osm2db.import_osm_raw(args.dbname, args.user, args.host, args.password, args.osm_file, args.location)
    
    if args.network:
        print 'writing osm road network...\n'
        osm_streets.write_street_network(args.dbname, args.user, args.host, args.password, args.location, int(args.verbose))
    
    if args.buildings:
        print 'writing osm buildings...\n'
        osm_polygons.write_polygons(args.dbname, args.user, args.host, args.password, args.location, "building", int(args.verbose))
    
    if args.crossroad_density:
        print 'calculating crossroad density...'
        netindicators.write_intersections(conn, args.out_schema, args.out_schema, network_table, "%s_intersections"%network_table, int(args.srid))

        netindicators.calculate_density(conn, args.location,
                      args.agg_schema, 
                    args.agg_table,
                    args.agg_geom, 
                    args.agg_id, 
                    args.out_schema, 
                    'geom',
                    '%s_network_intersections_clustered'%(args.location), 
                    'crossroads',
                    int(args.srid),
                    verbose=int(args.verbose))
        conn.execute('DROP TABLE IF EXISTS %s.%s_network_intersections'%(args.out_schema, args.location))

        
    if args.network_indicators:
        print 'calculation network indicators\n'
        netindicators.calculate_network_indicators(conn, 
                                            args.out_schema, 
                                            "%s_%s_net_ind"%(args.location,args.agg_table), 
                                           args.agg_schema+"."+ args.agg_table, 
                                            args.agg_id, 
                                            args.agg_geom,
                                            network_table, 
                                            network_table_street_column , 
                                            geom_field_network_table,
                                            args.srid) 
    
    
    if args.facility:
        print 'calculation facilities and density per aggregation unit\n'
        osm_facilities.build_facilities_calc_dens(args.dbname, args.host, args.user, args.password, args.location, 
                               db_table_name, 
                               args.out_schema, 
                               args.agg_schema, 
                               args.agg_table,
                               args.agg_id,
                               args.agg_geom,
                               args.srid,
                               pop_column=None,
                               drop_if_table_exists=True)
        
    
