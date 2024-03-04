#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# =============================================================================
# osmdb.py
#
# Author: Daniel Krajzewicz
# Date:   01.04.2016
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2016-2024 Institute of Transport Research,
#                         German Aerospace Center
# All rights reserved.
# =============================================================================
"""OSM database representation."""
# =============================================================================

# --- imported modules --------------------------------------------------------
import psycopg2


# --- meta --------------------------------------------------------------------
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright (c) 2016-2024 Institute of Transport Research, German Aerospace Center"
__credits__    = [ "Daniel Krajzewicz" ]
__license__    = "EPL2.0"
__version__    = "0.8"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Development"


# --- class definitions -------------------------------------------------------
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
    def get_node(self, nID):
        """Returns the node defined by the given id
        :param nID: The ID of the node to retrieve
        :type nID: int
        :return: The node, given as ID and position
        :rtype: 
        """
        query = "SELECT * from %s.%s_node WHERE id='%s';" % (self._schema, self._dbprefix, nID)
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

  
    def getWayKV_withMatchingKey(self, key):
        """Returns the IDs and parameter of all ways with the given key
        :param key: The key to get the IDs and parameter for
        :type key: str
        :return: The IDs and parameter with the given key as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_wtag WHERE k='%s';" % (self._schema, self._dbprefix, key)
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

  

    
    
    
