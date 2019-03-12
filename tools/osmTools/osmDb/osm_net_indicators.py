'''
Created on 24.05.2017
# =========================================================
 netIndicators.py
@author Simon Nieland
@date 24.05.2017
@copyright Institut fuer Verkehrsforschung, 
            Deutsches Zentrum fuer Luft- und Raumfahrt
@brief calculates indicators of OpenStreetMap data
# =========================================================

'''


    



from geoalchemy2 import Geometry, WKTElement
import pandas as pd
from sklearn.cluster import DBSCAN
from geopy.distance import great_circle
from shapely.geometry import MultiPoint
from shapely.geometry import Point
from geopandas import GeoDataFrame


    
def createIntersecTable(connection, 
                        agg_area, 
                        agg_id,
                        agg_geom,
                        epsg,
                        network_table,
                        network_column,
                        network_geom): 
  
    
    connection.execute("DROP TABLE IF EXISTS intersec;")
    
    sqlString=  """SELECT * INTO public.intersec FROM
      (SELECT 
        row_number() over (order by 1) as key,
        agg_area.%s AS agg_id,
        network_table.%s AS street_type,
        network_table.vmax AS vmax,    
        ST_Multi(ST_Intersection(agg_area.%s, ST_Transform(network_table.%s, %s)))::geometry(multiLineString, %s) as geom
          FROM
            %s AS agg_area
            LEFT JOIN osm.%s AS network_table
              ON (ST_INTERSECTS(agg_area.%s, ST_Transform(network_table.%s, %s)))
                
                WHERE 
                
                --network_table.street_type LIKE ('%ssecondary') OR
                --network_table.street_type LIKE ('%stertiary') OR
                --network_table.street_type LIKE ('%sprimary') OR
                --network_table.street_type LIKE ('%sresidential') OR
                --network_table.street_type LIKE ('%sunclassified') OR
                --network_table.street_type LIKE ('%slivingstreet') 
                
                --AND network_table.%s NOT LIKE '%s' AND
                ST_isValid(agg_area.%s) = TRUE AND ST_isValid(ST_Transform(network_table.%s, %s)) = TRUE 
                
      ) as foo;
      
      ALTER TABLE intersec ADD PRIMARY KEY (key);"""% (agg_id,                    
                                                         network_column,                                             
                                                         agg_geom, 
                                                         network_geom, 
                                                         epsg,
                                                         epsg,                                                        
                                                         agg_area,                    
                                                         network_table,                     
                                                         agg_geom, 
                                                         network_geom, 
                                                         epsg,           
                                                         "%%", "%%", "%%", "%%", "%%", "%%", 
                                                         network_column, "track;%%",            
                                                         agg_geom, 
                                                         network_geom,
                                                         epsg )     

    connection.execute(sqlString)
    

    
    
  
  






  
def createResultTable(connection,
                        out_schema,
                        result_table,
                        agg_id,
                        agg_geom,
                        agg_area):
    #####################
    #creates output table
    #####################
    sqlString = """DROP TABLE IF EXISTS %s.%s;
    
    SELECT 
      row_number() over (order by 1) as key,
      %s AS agg_id,
      %s AS geom
        INTO %s.%s
        FROM %s AS agg_area
        WHERE ST_isValid(agg_area.%s) = TRUE AND ST_isSimple(agg_area.%s) = TRUE
    ;
    
    ALTER TABLE %s.%s ADD PRIMARY KEY (key);"""% (
        out_schema,
        result_table,    
        agg_id,       
        agg_geom,
        out_schema,     
        result_table,
        agg_area,    
        agg_geom, 
        agg_geom,
        out_schema,
        result_table     
    )

    
    connection.execute(sqlString)  
  

def get_street_types(connection, network_table):
    ##############
    #returns list of distinct street types from osm street network
    ##############
    sqlString = """SELECT DISTINCT street_type
                    FROM osm.%s
                    ;""" % (network_table)   
    street_types = connection.execute(sqlString)    
    return street_types
  
  
def calculate_length(connection, result_schema, result_table, street_type):
    ################
    #calculates length of a certain street type 
    ################  
    sqlString=  """ALTER TABLE %s.%s DROP COLUMN IF EXISTS sum_%s;
      ALTER TABLE %s.%s ADD COLUMN sum_%s FLOAT;
      UPDATE %s.%s 
        SET sum_%s = foo.sum_%s
          FROM (
            SELECT 
              agg_id,
              SUM(ST_Length(geom))/1000 AS sum_%s
                FROM intersec
                WHERE (street_type = 'highway_%s' OR street_type = 'railway_%s')
                GROUP BY agg_id
                ORDER BY agg_id
            ) as foo
      WHERE %s.Agg_ID = foo.Agg_ID;"""% (result_schema,
                                         result_table,
                                         street_type,
                                         result_schema,
                                         result_table,
                                         street_type,
                                         result_schema,
                                         result_table,
                                         street_type,
                                         street_type,
                                         street_type,
                                         street_type,
                                         street_type,
                                         result_table)

    connection.execute(sqlString)
      

def calculate_sum_length(connection, out_schema,  result_table, street_type):
    #################
    #calculates the sum of all streets in one area into column sum_length
    #################
    connection.execute(
    """
    UPDATE %s.%s
    SET sum_length = COALESCE(sum_length,0)+COALESCE(sum_%s,0)  
    ;""" %(out_schema, 
           result_table, 
           street_type)
    )


def calculate_ratio(connection, out_schema, out_table, street_type): 
    ######################
    # creates column ratio_XXstreet_type and calculates the ratio of the actual street type 
    # and the total street length. Sum length column  (sum_length) has to be in the data set
    ######################
    sqlString = """ALTER TABLE %s.%s DROP COLUMN IF EXISTS ratio_%s;
      ALTER TABLE %s.%s ADD COLUMN ratio_%s FLOAT;
      UPDATE %s.%s 
      SET ratio_%s = sum_%s/sum_length
      ;"""% (out_schema,
      out_table,
      street_type,
      out_schema,
      out_table,         
      street_type, 
      out_schema,
      out_table,        
      street_type, 
      street_type)

    connection.execute(sqlString)
  

def calculate_network_indicators(connection,
                                 out_schema="public",
                        result_table_name= "hamburg_st_network_ind",
                        agg_table= "urmo_hh.st_aufbereitet_pop", 
                        agg_id= "st", 
                        agg_geom= "geom",
                        network_table= "hamburg_test_network", 
                        net_column= "street_type", 
                        net_geom ="the_geom",
                        epsg="25832" 
                        ):
    ######################
    # performs intersection of network and aggregation area
    # calculates street length of all street_types in the area
    # calculates sum of all streets in the area
    # calculates ratios of street_types and all streets in the area
    ######################
    

    
    createIntersecTable(connection, agg_table, agg_id, agg_geom, epsg, network_table, net_column, net_geom)


    createResultTable(connection,out_schema, result_table_name, agg_id, agg_geom, agg_table)


    street_types = get_street_types(connection, network_table)
    for street_type in street_types: 
        calculate_length(connection, out_schema, result_table_name, street_type['street_type'].split("_")[1])
    
    
    connection.execute("""ALTER TABLE %s.%s DROP COLUMN IF EXISTS sum_length;
    ALTER TABLE %s.%s ADD COLUMN sum_length FLOAT;"""% (    out_schema,
                                                            result_table_name,
                                                            out_schema,
                                                            result_table_name))
                                                            
                       
    street_types = get_street_types(connection, network_table)
    for street_type in street_types:
        calculate_sum_length(connection, out_schema, result_table_name, street_type['street_type'].split("_")[1])

    street_types = get_street_types(connection, network_table)    
    for street_type in street_types: 
        calculate_ratio(connection, out_schema, result_table_name, street_type['street_type'].split("_")[1])



def write_intersections(connection, network_schema= "osm", out_schema= "osm",  network_table= "berlin_network", intersection_table= "berlin_network_intersections", epsg=25833, verbose=0):
    #################
    #writes nodes with minimum four adjacent road segments into a new table
    #################
    sql_statement = """ DROP TABLE IF EXISTS %s.%s_streets;
    SELECT * into %s.%s_streets from %s.%s where street_type = 'highway_primary' or street_type ='highway_secondary' or  street_type ='highway_tertiary' or street_type ='highway_residential' or street_type ='highway_living_street';
    DROP TABLE IF EXISTS %s.%s_streets_count_unique;
    CREATE TABLE %s.%s_streets_count_unique (the_geom geometry);
    INSERT INTO %s.%s_streets_count_unique SELECT DISTINCT the_geom from %s.%s_streets;
    DROP TABLE IF EXISTS %s.tmp;
    Select (ST_DumpPoints(the_geom)).* into %s.tmp from %s.%s_streets_count_unique;
    DROP TABLE IF EXISTS %s.%s;
    SELECT  geom, COUNT(*) as num into %s.%s  FROM %s.tmp AS temp GROUP BY geom;
    DELETE from %s.%s WHERE num<3;
    DROP TABLE %s.tmp;
    DROP TABLE %s.%s_streets_count_unique;
    DROP TABLE %s.%s_streets;""" %(out_schema, 
           network_table, 
           network_schema, 
           network_table, 
           network_schema, 
           network_table, 
           network_schema, 
           network_table, 
           network_schema, 
           network_table, 
           network_schema, 
           network_table, 
           network_schema, 
           network_table, 
           out_schema,
           out_schema,
           network_schema, 
           network_table,
           network_schema, 
           intersection_table,
           network_schema, 
           intersection_table,
           out_schema,
           out_schema, 
           intersection_table,
           out_schema,
           network_schema, 
           network_table,
           network_schema, 
           network_table)
    if verbose>0:
        print sql_statement
    connection.execute(sql_statement)
    cluster_intersections_to_crossroad(connection, out_schema, intersection_table, epsg)
    
    
def get_centermost_point(cluster):
    centroid = (MultiPoint(cluster).centroid.x, MultiPoint(cluster).centroid.y)
    centermost_point = min(cluster, key=lambda point: great_circle(point, centroid).m)
    return tuple(centermost_point)

def get_centroids(cluster):
    centroid = (MultiPoint(cluster).centroid.x, MultiPoint(cluster).centroid.y)
    return tuple(centroid)

def cluster_intersections_to_crossroad(connection, intersection_schema= "osm", intersection_table= "hamburg_network_intersections", srid=25832):
    ### Uses DBScan clustering algorithm to produce one crossing from several street intersections. 
    ###Input:
    ###connection: sqlAlchemy db connection
    ###intersection_schema: schema of street intersection table
    ###intersection_schema: street intersection table
    ###srid: srid code of region
    
    
    ##data import
    sql_stmt = "select num, St_X(st_transform(geom,%s)) AS lon, st_y(st_transform(geom,%s)) as lat from %s.%s"%(srid, srid, intersection_schema,intersection_table)
    df=pd.read_sql(sql_stmt, connection)
    df = df.dropna()
    coords = df.as_matrix(columns=['lat','lon'])
    
    ##clustering 
    print '    performing clustering of intersections..'
    db = DBSCAN(eps=40, min_samples=1, n_jobs=-1).fit(coords)
    cluster_labels = db.labels_
    num_clusters = len(set(cluster_labels))
    print('    Number of crossroads: {}\n'.format(num_clusters))
    clusters = pd.Series([coords[cluster_labels == n] for n in range(num_clusters)])
    
    #get cluster centriods
    centermost_points = clusters.map(get_centroids)
    lats, lons = zip(*centermost_points)
    rep_points = pd.DataFrame({'lon':lons, 'lat':lats})
    rs = rep_points
    geometry = [Point(xy) for xy in zip(rs.lon, rs.lat)]
    crs = {'init': 'epsg:%s'% (srid)}
    geo_df = GeoDataFrame(rs, crs=crs, geometry=geometry)
    geo_df['geom'] = geo_df['geometry'].apply(lambda x: WKTElement(x.wkt, srid=srid))
    geo_df.drop('geometry', 1, inplace=True)
    
    
    ##write centriods to db
    geo_df.to_sql(intersection_table+"_clustered", connection, schema='osm', if_exists='replace', index=True, 
                         dtype={'geom': Geometry('POINT', srid= srid)})


    
def calculate_density(connection, location="berlin",
                      schema_aggr_unit= "urmo", 
                    table_aggr_unit="sg",
                    geom_field_aggr_unit="the_geom", 
                    join_field_aggr_unit="gid" , 
                    schema_table_points="osm", 
                    geom_field_table_points="geom",
                    table_points='berlin_network_intersections_clustered', 
                    content= 'crossroads',
                    epsg = "25833",
                    verbose=0,
                    if_exists= 'replace'):
    
    if if_exists=='replace':
        connection.execute('drop table if exists %s.%s_%s_dens'%(schema_table_points,location, content))  
        
    sql_string =  """with counts AS(
            SELECT s.%s, COUNT(p.*) count 
            FROM %s.%s p, %s.%s s
            WHERE ST_Within(ST_Transform(p.%s, %s), s.%s) 
            GROUP BY s.%s
            ORDER BY s.%s)
            SELECT x.%s, s.count/(ST_Area(x.%s) / 1000000) %s_dens_osm
            INTO %s.%s_%s_dens
            FROM %s.%s x
            LEFT JOIN counts s ON s.%s = x.%s;""" %(join_field_aggr_unit, 
            schema_table_points, 
            table_points, 
            schema_aggr_unit,
            table_aggr_unit,
            geom_field_table_points,
            epsg,
            geom_field_aggr_unit,
            join_field_aggr_unit,
            join_field_aggr_unit,
            join_field_aggr_unit,
            geom_field_aggr_unit,
            content,  
            schema_table_points,
            location,
            content,
            schema_aggr_unit, 
            table_aggr_unit,
            join_field_aggr_unit,
            join_field_aggr_unit
            )
    if verbose>0:
        print sql_string  
                         
    connection.execute(sql_string)    
        