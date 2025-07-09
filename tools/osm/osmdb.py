#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""OSM database representation."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2016-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.2"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Production"
# ===========================================================================
# - https://github.com/DLR-VF/UrMoAC
# - https://www.dlr.de/vf
# ===========================================================================


# --- imports ---------------------------------------------------------------
import psycopg2


# --- class definitions -----------------------------------------------------
class OSMDB:
    """A connection to a database representation of an OSM area."""
  
    def __init__(self, schema, dbprefix, conn, cursor):
        """Constructor
        
        Initialises the connection
        :param schema: The database schema to write the data to
        :type schema: str
        :param dbprefix: The database prefix to write the data to
        :type dbprefix: str
        :param conn: The connection to the database
        :type conn: psycopg2.extensions.connection
        :param cursor: The cursor used to write to the database
        :type cursor: psycopg2.extensions.cursor
        """
        self._schema = schema
        self._dbprefix = dbprefix
        self._conn = conn
        self._cursor = cursor
    

    
    # --- db helper ---------------------------------------
    def _execute_fetch_all(self, query):
        """Executes the query and fetches all results
        :param query: The query to execute
        :type query: str
        :return: The results of the query
        :rtype: 
        """
        self._cursor.execute(query)
        return self._cursor.fetchall()      


    def table_exists(self, schema, name):
        """Returns whether the given table already exists
        :param schema: The schema the table is located in
        :type schema: str
        :param dbprefix: The name of the table
        :type dbprefix: str
        :return: Whether the table already exists
        :rtype: bool
        """
        # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
        self._cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name ='%s');""" % (schema, name))
        self._conn.commit()   
        ret = self._cursor.fetchall()
        return ret[0][0]
  

  
    # --- node i/o helper ---------------------------------
    def get_node(self, nID, area):
        """Returns the node defined by the given id
        :param nID: The ID of the node to retrieve
        :type nID: int
        :return: The node, given as ID and position
        :rtype: 
        """

        if 'nuts3_' in area:
            areacat = 'bezeichnung'
            areaspec = area.split('_')[1]
        else:
            areacat = 'land'
            areaspec = area

        if area == 'germany':
            query = "SELECT * from %s.%s_node WHERE id='%s';" % (self._schema, self._dbprefix, nID)
        else:
            query = """
            SELECT n.id, n.pos
            FROM %s.%s_node n, osmextract.area l
            WHERE l.%s = '%s'
            AND n.id = '%s'
            AND ST_Within(n.pos, l.geom);
            """ % (self._schema, self._dbprefix, areacat, areaspec, nID)

        return self._execute_fetch_all(query)



    def getNodeKV_forID(self, nid):
        """Returns the parameter of a node
        :param nID: The ID of the node to retrieve
        :type nID: int
        :return: The parameter of the node as ID/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_ntag WHERE id='%s';" % (self._schema, self._dbprefix, nid)
        return self._execute_fetch_all(query)
  
  
    def getNodeKV_withMatchingKey(self, key):
        """Returns the ids and parameter of all ways with the given key
        :param key: The key to get the ids and parameter for
        :type key: str
        :return: The ids and parameter with the given key as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_ntag WHERE k='%s';" % (self._schema, self._dbprefix, key)
        return self._execute_fetch_all(query)
  
    
    def getNodeKV_withMatchingKeyValue(self, key, value):
        """Returns the ids and parameter of all nodes with the given key and the given value
        :param key: The key to get the ids and parameter for
        :type key: str
        :param key: The value to get the ids and parameter for
        :type key: str
        :return: The ids and parameter with the given key and value as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_ntag WHERE (k='%s' AND v='%s');" % (self._schema, self._dbprefix, key, value)
        return self._execute_fetch_all(query)


    def getNodesWGeom(self, nIDs):
        """Returns the IDs and positions of the given nodes
        
        The positions are returned as given in the database (usually WGS84).
                
        :param nIDs: A list of node IDs
        :type nIDs: list[int]
        :return: The IDs and positions of the named nodes
        :rtype: 
        """
        query = "SELECT id,ST_AsText(pos) from %s.%s_node WHERE id in ("+','.join([str(x) for x in nIDs])+");" % (self._schema, self._dbprefix)
        return self._execute_fetch_all(query)
  
  
    def getNodes_preserveOrder(self, nIDs):
        """Returns the IDs and positions of the given nodes preserving the given order
        
        The positions are returned as given in the database (usually WGS84).
                
        :param nIDs: A list of node IDs
        :type nIDs: list[int]
        :return: The IDs and positions of the named nodes
        :rtype: 
        """
        rep = ','.join([str(x) for x in nIDs])
        # http://stackoverflow.com/questions/866465/sql-order-by-the-in-value-list
        query = "SELECT id, ST_AsText(pos) from %s.%s_node WHERE id in (%s);" % (self._schema, self._dbprefix, rep)
        nodes = self._execute_fetch_all(query)
        n2pos = {}
        for n in nodes:
            n2pos[n[0]] = n[1]
        ret = []
        for n in nIDs:
            if n not in n2pos:
                print ("Warning: node %s is not known" % n)
                continue
            ret.append([n, n2pos[n]])
        return ret

    def getNodeValue_withKeyAndID(self, elem, key):
        query = "SELECT DISTINCT v FROM %s.%s_ntag WHERE id = '%s' AND k = '%s';" % (self._schema, self._dbprefix, elem, key)
        return self._execute_fetch_all(query)

    
    # --- way i/o helper ----------------------------------
    def get_way(self, wID):
        """Returns the way defined by the given ID

        :param wID: The ID of the way to retrieve
        :type nID: int
        :return: The way, given as id and referenced items
        :rtype: 
        """
        query = "SELECT id,refs from %s.%s_way WHERE id='%s';" % (self._schema, self._dbprefix, wID)
        return self._execute_fetch_all(query)
  

    def getWayKV_forID(self, wID):
        """Returns the parameter of a way
        :param wID: The ID of the way to retrieve
        :type wID: int
        :return: The parameter of the way as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_wtag WHERE id='%s';" % (self._schema, self._dbprefix, wID)
        return self._execute_fetch_all(query)




    def getWayKV_withMatchingKey(self, key, area):
        """Returns the IDs and parameter of all ways with the given key
        :param key: The key to get the IDs and parameter for
        :type key: str
        :return: The IDs and parameter with the given key as id/key/value tuples
        :rtype:
        """
        if 'nuts3_' in area:
            areacat = 'bezeichnung'
            areaspec = area.split('_')[1]
        else:
            areacat = 'land'
            areaspec = area
        if area == 'germany' :
            query = "SELECT * from %s.%s_wtag WHERE k='%s';" % (self._schema, self._dbprefix, key)
            return self._execute_fetch_all(query)
        else:
            query = """
            SELECT waynodes.id as id, waynodes.k as k, waynodes.v as v
            FROM
                (SELECT tway.id as id, tway.k as k, tway.v as v, node.pos as geom
                FROM
                    (SELECT tag.id as id, k, v, refs
                    FROM
                        (SELECT id, k, v 
                        FROM %s.%s_wtag
                        where k = '%s') as tag
                    LEFT JOIN %s.%s_way as way
                    ON tag.id = way.id) as tway
                LEFT JOIN %s.%s_node as node
                on tway.refs[1] = node.id) as waynodes
            INNER JOIN (SELECT * FROM osmextract.area WHERE %s = '%s') as zelle
            on ST_Within(waynodes.geom, zelle.geom);""" % (self._schema, self._dbprefix, key, self._schema, self._dbprefix, self._schema, self._dbprefix, areacat, areaspec)
            print(query)
            return self._execute_fetch_all(query)
    
  
    def getWayKV_withMatchingKeyValue(self, key, value):
        """Returns the IDs and parameter of all ways with the given key and the given value
        :param key: The key to get the IDs and parameter for
        :type key: str
        :param value: The value to get the IDs and parameter for
        :type value: str
        :return: The IDs and parameter with the given key and value as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_wtag WHERE k='%s' AND v='%s';" % (self._schema, self._dbprefix, key, value)
        return self._execute_fetch_all(query)


  
    # --- releation i/o helper ----------------------------
    def getRelationKV_forID(self, wID):
        """Returns the parameter of a relation
        :param wID: The ID of the relation to retrieve
        :type wID: int
        :return: The parameter of the node as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_rtag WHERE id='%s';" % (self._schema, self._dbprefix, wID)
        return self._execute_fetch_all(query)
  
  
    def getRelationKV_withMatchingKey(self, key):
        """Returns the ids and parameter of all relations with the given key
        :param key: The key to get the ids and parameter for
        :type key: str
        :return: The IDs and parameter with the given key as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_rtag WHERE k='%s';" % (self._schema, self._dbprefix, key)
        return self._execute_fetch_all(query)
  

    def getRelationKV_withMatchingKeyValue(self, key, value):
        """Returns the IDs and parameter of all relations with the given key and the given value
        :param key: The key to get the IDs and parameter for
        :type key: str
        :param value: The value to get the IDs and parameter for
        :type value: str
        :return: The IDs and parameter with the given key and value as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_rtag WHERE k='%s' AND v='%s';" % (self._schema, self._dbprefix, key, value)
        return self._execute_fetch_all(query)
  

    def getMembers_forID(self, rID):
        """Returns the members of the relation with the given id
        :param rID: The ID of the relation to get the members for
        :type rID: int
        :return: The members of the given relation
        :rtype: 
        """
        query = "SELECT * from %s.%s_member WHERE rid=%s ORDER BY idx;" % (self._schema, self._dbprefix, rID)
        return self._execute_fetch_all(query)

    def lightrailsort(self, lrID):
        query = """SELECT ST_Within(waynodes.geom, zelle.geom)
                    FROM
                        (SELECT way.id as id, node.pos as geom
                        FROM
                            (SELECT id, refs
                            FROM %s.%s_way
                            where id = '%s') as way
                        LEFT JOIN %s.%s_node as node
                        on way.refs[1] = node.id) as waynodes
                    INNER JOIN (SELECT * FROM osmextract.area WHERE land = 'hamburg' or land = 'berlin') as zelle
                    on ST_DWithin(waynodes.geom, zelle.geom, 0.05);
                    """ % (self._schema, self._dbprefix, lrID, self._schema, self._dbprefix, )
        if self._execute_fetch_all(query):
            return 'light_rail'
        else:
            return 'stadtbahn'

    def subwaysort(self, subID):
        query = """SELECT ST_Within(waynodes.geom, zelle.geom)
                    FROM
                        (SELECT way.id as id, node.pos as geom
                        FROM
                            (SELECT id, refs
                            FROM %s.%s_way
                            where id = '%s') as way
                        LEFT JOIN %s.%s_node as node
                        on way.refs[1] = node.id) as waynodes
                    INNER JOIN (SELECT * FROM osmextract.area WHERE land = 'hamburg' or 
                                                                    land = 'berlin' or 
                                                                    bezeichnung = 'muenchen' or
                                                                    bezeichnung = 'nuernberg') as zelle
                    on ST_DWithin(waynodes.geom, zelle.geom, 0.05);
                    """ % (self._schema, self._dbprefix, subID, self._schema, self._dbprefix, )
        if self._execute_fetch_all(query):
            return 'subway'
        else:
            return 'stadtbahn'


    def getStops(self, area, mode, dschema, dprefix):
        if mode == 'railway':
            tramstop = ''
            ptstop = ''
        else:
            tramstop = ' or v = \'tram_stop\''
        query = """SELECT DISTINCT loc.id as id, loc.k as k, loc.v as v, loc.geom as geom
                    FROM 
                        (Select tags.id as id, tags.k as k, tags.v as v, node.pos as geom
                        FROM 
                            (SELECT id, k, v
                            FROM %s.%s_ntag
                            where k = 'railway' and (v = 'station' or v = 'halt' or v = 'stop'%s)) as tags
                        LEFT JOIN %s.%s_node as node
                        ON tags.id = node.id) as loc
                    INNER JOIN %s.%s_%s_%s_network as network
                    on ST_DWithin(loc.geom, network.geom, 0.0002)""" % (self._schema, self._dbprefix, tramstop, self._schema, self._dbprefix, dschema, dprefix, area, mode)
        print(query)
        return self._execute_fetch_all(query)

    def fetchCloseStops(self, ID, area, mode, dschema, dprefix):
        query = """SELECT *
                    FROM %s.%s_%s_%s_stops as stops
                    WHERE ST_DWithin((	SELECT DISTINCT geom 
					                    FROM %s.%s_%s_%s_stops
					                    WHERE id = '%s'),
					                    stops.geom, 0.002)
        """ % (dschema, dprefix, area, mode, dschema, dprefix, area, mode, ID)
        return self._execute_fetch_all(query)

    """  
        def getWayKV_withMatchingKey(self, key, area):
            Returns the IDs and parameter of all ways with the given key
            :param key: The key to get the IDs and parameter for
            :type key: str
            :return: The IDs and parameter with the given key as id/key/value tuples
            :rtype: 

            query = "SELECT * from %s.%s_wtag WHERE k='%s';" % (self._schema, self._dbprefix, key)
            return self._execute_fetch_all(query)
     """

