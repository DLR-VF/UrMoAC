'''
Created on 23.01.2017

@author: niel_sm
'''
# This script calculates density values from osm facilities. Please use osm2db to import osm data into the data base.
# currently 'restaurant', 'health', 'daily', 'shop', 'sport', 'school', "bar", "fun" and "culture" are supported

from sqlalchemy import *
import sys




def build_facility_table(connection, out_schema, out_table, osm_schema, in_table, in_table_node, in_table_node_geom, sqlStatement):
    connection.execute("""Drop table if exists  %s.%s;"""% (out_schema, out_table)) 
    connection.execute("""create table %s.%s AS SELECT %s.id, %s.v, 
                    %s.k, osm.%s.%s from osm.%s, 
                    osm.%s Where (%s) and 
                    %s.id= %s.id"""% (out_schema, out_table, in_table,  in_table, in_table, 
                                                                   in_table_node, in_table_node_geom, in_table, in_table_node, 
                                                                  sqlStatement, in_table, in_table_node))



def calculate_density(connection,location="hamburg",
                      schema_aggr_unit= "urmo_hh", 
                    table_aggr_unit="st_aufbereitet_pop",
                    geom_field_aggr_unit="geom", 
                    join_field_aggr_unit="st" , 
                    schema_table_points="osm", 
                    table_points='hamburg_facilities_restaurants', 
                    content= 'restaurant',
                    epsg = "25833",
                    pop_column= None
                    ):
    if pop_column:
        
        sql_string =  """with counts AS(
            SELECT s.%s, COUNT(p.*) fun 
            FROM %s.%s p, %s.tmp s
            WHERE ST_Within(ST_Transform(p.pos, %s), s.%s) 
            GROUP BY s.%s
            ORDER BY s.%s)
            SELECT x.*, s.fun/(ST_Area(x.%s) / 1000000) %s_dens_osm,
            s.fun / (x.%s::double precision / 1000) %s_dens_pop_osm 
            INTO %s.%s_dens
            FROM %s.tmp x
            LEFT JOIN counts s ON s.%s = x.%s;
            Drop table if exists %s.tmp;
            create table %s.tmp as Select * from %s.%s_dens;""" %(join_field_aggr_unit, 
             schema_table_points, 
             table_points, 
             schema_table_points,
             epsg,
             geom_field_aggr_unit,
             join_field_aggr_unit,
             join_field_aggr_unit,
            geom_field_aggr_unit,
            content,  
            pop_column,   
            content,
            schema_table_points, 
            location,
            schema_table_points, 
            join_field_aggr_unit,
            join_field_aggr_unit,
            schema_table_points,
            schema_table_points,
            schema_table_points,
            location
            )
        print sql_string                   
        connection.execute(sql_string)    
        
    else:
        connection.execute(
            """with counts AS(
            SELECT s.%s, COUNT(p.*) fun 
            FROM %s.%s p, %s.tmp s
            WHERE ST_Within(ST_Transform(p.pos, %s), s.%s) 
            GROUP BY s.%s
            ORDER BY s.%s)
            SELECT x.*, s.fun/(ST_Area(x.%s) / 1000000) %s_dens_osm
            INTO %s.%s_dens
            FROM %s.tmp x
            LEFT JOIN counts s ON s.%s = x.%s;
            Drop table if exists %s.tmp;
            create table %s.tmp as Select * from %s.%s_dens;""" %
            (join_field_aggr_unit, 
             schema_table_points, 
             table_points, 
             schema_table_points,
             epsg,
             geom_field_aggr_unit,
             join_field_aggr_unit,
             join_field_aggr_unit,
            geom_field_aggr_unit,
            content,
            schema_table_points, 
            location,
            schema_table_points, 
            join_field_aggr_unit,
            join_field_aggr_unit,
            schema_table_points,
            schema_table_points,
            schema_table_points,
            location
            )
        )

def build_facilities_calc_dens(db_name, host, user, psw, location, 
                               db_table_name, 
                               schema_table_points, 
                               schema_aggr_unit, 
                               table_aggr_unit,
                               join_field_aggr_unit,
                               geom_field_aggr_unit,
                               epsg,
                               pop_column=None,
                               drop_if_table_exists=True):
    
    contentList= ['restaurant', 'health', 'daily', 'shop', 'sport', 'school', "bar", "fun", "culture", "btu_stops", "train_stops"]   
    
    
    #################
    #optional:
    # ############
    



    try:
        engine = create_engine('postgresql://%s:%s@%s:5432/%s'%(user, psw, host,db_name))
        conn = engine.connect()
    except Exception, e:
        print e
        sys.exit()
        
    build_facility_table(conn, schema_table_points, db_table_name+'_health', schema_table_points, location+'_ntag', location+'_node', 'pos', "v='clinic' or v='doctors' or v='hospital' or v='dentist'")
    build_facility_table(conn, schema_table_points, db_table_name+'_restaurant', schema_table_points, location+'_ntag', location+'_node', 'pos', "v='restaurant' or v='fast_food' or v='cafe'")
    build_facility_table(conn, schema_table_points, db_table_name+'_bar', schema_table_points, location+'_ntag', location+'_node', 'pos', "v='pub' or v='bar'")
    build_facility_table(conn, schema_table_points, db_table_name+'_school', schema_table_points, location+'_ntag', location+'_node', 'pos', "v='school' or v='kindergarten'")
    build_facility_table(conn, schema_table_points, db_table_name+'_culture', schema_table_points, location+'_ntag', location+'_node', 'pos', "v='arts_centre' or v='cinema' or v='theatre'")
    build_facility_table(conn, schema_table_points, db_table_name+'_fun', schema_table_points, location+'_ntag', location+'_node', 'pos',  "v='brothel' or v='casino' or v='community_centre' or v='gambling' or v='nightclub'")
    build_facility_table(conn, schema_table_points, db_table_name+'_shop', schema_table_points, location+'_ntag', location+'_node', 'pos',"k='shop' and v!='supermarket'and v!='general'and v!='kiosk' and v!= 'bakery'")
    build_facility_table(conn, schema_table_points, db_table_name+'_daily', schema_table_points, location+'_ntag', location+'_node', 'pos', "v='supermarket' or v='general' or v='kiosk' or v= 'bakery'")
    build_facility_table(conn, schema_table_points, db_table_name+'_sport', schema_table_points, location+'_ntag', location+'_node', 'pos', "k='sport' and v!='darts' and v!='table_soccer'")
    
    #public transport

    build_facility_table(conn, schema_table_points, db_table_name+'_btu_stops', schema_table_points, location+'_ntag', location+'_node', 'pos', "k='tram' or k='subway' or k='bus'")
    build_facility_table(conn, schema_table_points, db_table_name+'_train_stops', schema_table_points, location+'_ntag', location+'_node', 'pos', "k='train'")
    
    if drop_if_table_exists:
        conn.execute("""Drop table if exists  %s.%s_dens;"""%(schema_table_points, location))
    
    
    if pop_column:
        conn.execute("""Drop table if exists  %s.tmp;"""%(schema_table_points) ) 
        conn.execute("""Create table %s.tmp as select %s, %s, %s from %s.%s;""" %(schema_table_points, join_field_aggr_unit,geom_field_aggr_unit, pop_column, schema_aggr_unit, table_aggr_unit )) 
        conn.close() 
        print "pop column is: " + pop_column
        for content in contentList:
            table_points = "%s_facilities_%s"%(location,content)
            conn = engine.connect()
            calculate_density(conn,location,
                      schema_aggr_unit, 
                    table_aggr_unit, 
                    geom_field_aggr_unit,
                    join_field_aggr_unit,
                    schema_table_points, 
                    table_points, 
                    content,
                    epsg,
                    pop_column)
            conn.execute("""Drop table if exists  %s.%s_dens;"""%(schema_table_points, location))
            conn.close()
        conn = engine.connect()
        conn.execute("""create table %s.%s_dens as Select * from %s.tmp;"""%(schema_table_points, location,schema_table_points ))
        conn.execute("""Drop table if exists  %s.tmp;"""%(schema_table_points )) 
        conn.close() 
        
    else:
        conn.execute("""Drop table if exists  %s.tmp;"""%(schema_table_points) ) 
        conn.execute("""Create table %s.tmp as select %s, %s from %s.%s;""" %(schema_table_points, join_field_aggr_unit,geom_field_aggr_unit, schema_aggr_unit, table_aggr_unit )) 
        conn.close() 
        
        for content in contentList:
            table_points = "%s_facilities_%s"%(location,content)
            conn = engine.connect()
            calculate_density(conn,location,
                      schema_aggr_unit, 
                    table_aggr_unit, 
                    geom_field_aggr_unit,
                    join_field_aggr_unit,
                    schema_table_points, 
                    table_points, 
                    content,
                    epsg)
            conn.execute("""Drop table if exists  %s.%s_dens;"""%(schema_table_points, location))
            conn.close()
        conn = engine.connect()
        conn.execute("""create table %s.%s_dens as Select * from %s.tmp;"""%(schema_table_points, location,schema_table_points ))
        conn.execute("""Drop table if exists  %s.tmp;"""%(schema_table_points )) 
        conn.close() 


