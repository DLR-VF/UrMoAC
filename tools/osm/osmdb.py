#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""OSM database representation."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2016-2025, Institute of Transport Research, German Aerospace Center (DLR)"
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
class DB:
    """A connection to a database representation of an OSM area."""
  
    def __init__(self, dbdef):
        """Constructor
        
        Initialises the connection
        """
        (host, db, schema_prefix, user, password) = dbdef.split(",")
        self._schema, self._prefix = schema_prefix.split(".")
        self._conn = psycopg2.connect(f"dbname='{db}' user='{user}' host='{host}' password='{password}'")
        self._cursor = self._conn.cursor()
    
    def table_exists(self, name=None):
        """Returns whether the given table already exists
        :param name: The name of the table
        :type name: str
        :return: Whether the table already exists
        :rtype: bool
        """
        if name is None:
            name = self._prefix
        # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
        self._cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name ='%s');""" % (self._schema, name))
        self._conn.commit()   
        ret = self._cursor.fetchall()
        return ret[0][0]


    def get_table_path(self):
        return self._schema + "." + self._prefix

    def get_table_name(self):
        return self._prefix

    def get_table_schema(self):
        return self._schema

    def execute(self, what):
        self._cursor.execute(what)

    def commit(self):
        self._conn.commit()


class OSMDB(DB):
    """A connection to a database representation of an OSM area."""
  
    def __init__(self, dbdef):
        """Constructor
        
        """
        DB.__init__(self, dbdef)
    

    
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


  

  
    # --- node i/o helper ---------------------------------
    def get_node(self, nID):
        """Returns the node defined by the given id
        :param nID: The ID of the node to retrieve
        :type nID: int
        :return: The node, given as ID and position
        :rtype: 
        """
        query = "SELECT * from %s.%s_node WHERE id='%s';" % (self._schema, self._prefix, nID)
        return self._execute_fetch_all(query)
  
  
    def getNodeKV_forID(self, nid):
        """Returns the parameter of a node
        :param nID: The ID of the node to retrieve
        :type nID: int
        :return: The parameter of the node as ID/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_ntag WHERE id='%s';" % (self._schema, self._prefix, nid)
        return self._execute_fetch_all(query)
  
  
    def getNodeKV_withMatchingKey(self, key):
        """Returns the ids and parameter of all ways with the given key
        :param key: The key to get the ids and parameter for
        :type key: str
        :return: The ids and parameter with the given key as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_ntag WHERE k='%s';" % (self._schema, self._prefix, key)
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
        query = "SELECT * from %s.%s_ntag WHERE (k='%s' AND v='%s');" % (self._schema, self._prefix, key, value)
        return self._execute_fetch_all(query)


    def getNodesWGeom(self, nIDs):
        """Returns the IDs and positions of the given nodes
        
        The positions are returned as given in the database (usually WGS84).
                
        :param nIDs: A list of node IDs
        :type nIDs: list[int]
        :return: The IDs and positions of the named nodes
        :rtype: 
        """
        query = "SELECT id,ST_AsText(pos) from %s.%s_node WHERE id in ("+','.join([str(x) for x in nIDs])+");" % (self._schema, self._prefix)
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
        query = "SELECT id, ST_AsText(pos) from %s.%s_node WHERE id in (%s);" % (self._schema, self._prefix, rep)
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
        query = "SELECT id,refs from %s.%s_way WHERE id='%s';" % (self._schema, self._prefix, wID)
        return self._execute_fetch_all(query)
  

    def getWayKV_forID(self, wID):
        """Returns the parameter of a way
        :param wID: The ID of the way to retrieve
        :type wID: int
        :return: The parameter of the way as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_wtag WHERE id='%s';" % (self._schema, self._prefix, wID)
        return self._execute_fetch_all(query)

  
    def getWayKV_withMatchingKey(self, key):
        """Returns the IDs and parameter of all ways with the given key
        :param key: The key to get the IDs and parameter for
        :type key: str
        :return: The IDs and parameter with the given key as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_wtag WHERE k='%s';" % (self._schema, self._prefix, key)
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
        query = "SELECT * from %s.%s_wtag WHERE k='%s' AND v='%s';" % (self._schema, self._prefix, key, value)
        return self._execute_fetch_all(query)


  
    # --- releation i/o helper ----------------------------
    def getRelationKV_forID(self, wID):
        """Returns the parameter of a relation
        :param wID: The ID of the relation to retrieve
        :type wID: int
        :return: The parameter of the node as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_rtag WHERE id='%s';" % (self._schema, self._prefix, wID)
        return self._execute_fetch_all(query)
  
  
    def getRelationKV_withMatchingKey(self, key):
        """Returns the ids and parameter of all relations with the given key
        :param key: The key to get the ids and parameter for
        :type key: str
        :return: The IDs and parameter with the given key as id/key/value tuples
        :rtype: 
        """
        query = "SELECT * from %s.%s_rtag WHERE k='%s';" % (self._schema, self._prefix, key)
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
        query = "SELECT * from %s.%s_rtag WHERE k='%s' AND v='%s';" % (self._schema, self._prefix, key, value)
        return self._execute_fetch_all(query)
  

    def getMembers_forID(self, rID):
        """Returns the members of the relation with the given id
        :param rID: The ID of the relation to get the members for
        :type rID: int
        :return: The members of the given relation
        :rtype: 
        """
        query = "SELECT * from %s.%s_member WHERE rid=%s ORDER BY idx;" % (self._schema, self._prefix, rID)
        return self._execute_fetch_all(query)

  

    
    
    
