#!/usr/bin/env python
# =========================================================
# osmdb.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Helper methods for dealing with our OSM-database representation
# =========================================================


# --- imports ---------------------------------------------
import psycopg2


# --- class definitions -----------------------------------
class OSMDB:
    """ @class OSMDB
    @brief A connection to a database representation of an OSM area
    """
  
    # --- constructors ------------------------------------
    def __init__(self, schema, dbprefix, conn, cursor):
        """ @brief Initialises the connection
        @param self The class instance
        @param schema The schema the OSM data is stored in
        @param dbprefix The prefix of the tables the OSM data is stored in
        @param conn The connection to the database
        @param cursor The connection cursor
        """
        self.schema = schema
        self.dbprefix = dbprefix
        self.conn = conn
        self.cursor = cursor
    

    
    # --- db helper ---------------------------------------
    def fetchAllFromQuery(self, query):
        """ @brief Executes the query and fetches all results
        @param self The class instance
        @param query The query to execute
        @return The results of the query
        """
        self.cursor.execute(query)
        return self.cursor.fetchall()      


    def tableExists(self, schema, name):
        """ @brief Returns whether the given table already exists
        @param self The class instance
        @param schema The schema the table is located in
        @param name The name of the table
        @return Whether the table already exists
        """
        # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
        self.cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name ='%s');""" % (schema, name))
        self.conn.commit()   
        ret = self.cursor.fetchall()
        return ret[0][0]
  

  
    # --- node i/o helper ---------------------------------
    def getNode(self, nID):
        """ @brief Returns the node defined by the given id
        @param self The class instance
        @param nID The ID of the node to retrieve
        @return The node, given as id and position
        """
        query = "SELECT * from %s.%s_node WHERE id='%s';" % (self.schema, self.dbprefix, nID)
        return self.fetchAllFromQuery(query)
  
  
    def getNodeKV_forID(self, nid):
        """ @brief Returns the parameter of a node
        @param self The class instance
        @param wID The ID of the node to retrieve
        @return The parameter of the node as id/key/value tuples
        """
        query = "SELECT * from %s.%s_ntag WHERE id='%s';" % (self.schema, self.dbprefix, nid)
        return self.fetchAllFromQuery(query)
  
  
    def getNodeKV_withMatchingKey(self, key):
        """ @brief Returns the ids and parameter of all ways with the given key
        @param self The class instance
        @param key The key to get the ids and parameter for
        @return The ids and parameter with the given key as id/key/value tuples
        """
        query = "SELECT * from %s.%s_ntag WHERE k='%s';" % (self.schema, self.dbprefix, key)
        return self.fetchAllFromQuery(query)
  
    
    def getNodeKV_withMatchingKeyValue(self, key, value):
        """ @brief Returns the ids and parameter of all nodes with the given key and the given value
        @param self The class instance
        @param key The key to get the ids and parameter for
        @param value The value to get the ids and parameter for
        @return The ids and parameter with the given key and value as id/key/value tuples
        """
        query = "SELECT * from %s.%s_ntag WHERE (k='%s' AND v='%s');" % (self.schema, self.dbprefix, key, value)
        return self.fetchAllFromQuery(query)


    def getNodesWGeom(self, nIDs):
        """ @brief Returns the ids and positions of the given nodes
        
        The positions are returned as given in the database (usually WGS84).
                
        @param self The class instance
        @param nIDs a list of node ids (usually numeric)
        @return The ids and positions of the named nodes
        """
        query = "SELECT id,ST_AsText(pos) from %s.%s_node WHERE id in ("+','.join([str(x) for x in nIDs])+");" % (self.schema, self.dbprefix)
        return self.fetchAllFromQuery(query)
  
  
    def getNodes_preserveOrder(self, nIDs):
        """ @brief Returns the ids and positions of the given nodes preserving the given order
        
        The positions are returned as given in the database (usually WGS84).
                
        @param self The class instance
        @param nIDs a list of node ids (usually numeric)
        @return The ids and positions of the named nodes
        """
        rep = ','.join([str(x) for x in nIDs])
        # http://stackoverflow.com/questions/866465/sql-order-by-the-in-value-list
        query = "SELECT id, ST_AsText(pos) from %s.%s_node WHERE id in (%s);" % (self.schema, self.dbprefix, rep)
        nodes = self.fetchAllFromQuery(query)
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
    def getWay(self, wID):
        """ @brief Returns the way defined by the given id
        @param self The class instance
        @param wID The ID of the way to retrieve
        @return The way, given as id and referenced items
        """
        query = "SELECT id,refs from %s.%s_way WHERE id='%s';" % (self.schema, self.dbprefix, wID)
        return self.fetchAllFromQuery(query)
  

    def getWayKV_forID(self, wID):
        """ @brief Returns the parameter of a way
        @param self The class instance
        @param wID The ID of the way to retrieve
        @return The parameter of the way as id/key/value tuples
        """
        query = "SELECT * from %s.%s_wtag WHERE id='%s';" % (self.schema, self.dbprefix, wID)
        return self.fetchAllFromQuery(query)

  
    def getWayKV_withMatchingKey(self, key):
        """ @brief Returns the ids and parameter of all ways with the given key
        @param self The class instance
        @param key The key to get the ids and parameter for
        @return The ids and parameter with the given key as id/key/value tuples
        """
        query = "SELECT * from %s.%s_wtag WHERE k='%s';" % (self.schema, self.dbprefix, key)
        return self.fetchAllFromQuery(query)
  
  
    def getWayKV_withMatchingKeyValue(self, key, value):
        """ @brief Returns the ids and parameter of all ways with the given key and the given value
        @param self The class instance
        @param key The key to get the ids and parameter for
        @param value The value to get the ids and parameter for
        @return The ids and parameter with the given key and value as id/key/value tuples
        """
        query = "SELECT * from %s.%s_wtag WHERE k='%s' AND v='%s';" % (self.schema, self.dbprefix, key, value)
        return self.fetchAllFromQuery(query)


  
    # --- releation i/o helper ----------------------------
    def getRelationKV_forID(self, wID):
        """ @brief Returns the parameter of a relation
        @param self The class instance
        @param wID The ID of the relation to retrieve
        @return The parameter of the node as id/key/value tuples
        """
        query = "SELECT * from %s.%s_rtag WHERE id='%s';" % (self.schema, self.dbprefix, wID)
        return self.fetchAllFromQuery(query)
  
  
    def getRelationKV_withMatchingKey(self, key):
        """ @brief Returns the ids and parameter of all relations with the given key
        @param self The class instance
        @param key The key to get the ids and parameter for
        @return The ids and parameter with the given key as id/key/value tuples
        """
        query = "SELECT * from %s.%s_rtag WHERE k='%s';" % (self.schema, self.dbprefix, key)
        return self.fetchAllFromQuery(query)
  

    def getRelationKV_withMatchingKeyValue(self, key, value):
        """ @brief Returns the ids and parameter of all relations with the given key and the given value
        @param self The class instance
        @param key The key to get the ids and parameter for
        @param value The value to get the ids and parameter for
        @return The ids and parameter with the given key and value as id/key/value tuples
        """
        query = "SELECT * from %s.%s_rtag WHERE k='%s' AND v='%s';" % (self.schema, self.dbprefix, key, value)
        return self.fetchAllFromQuery(query)
  

    def getMembers_forID(self, rID):
        """ @brief Returns the members of the relation with the given id
        @param self The class instance
        @param rID The id of the relation to get the members for
        @return The members of the given relation
        """
        query = "SELECT * from %s.%s_member WHERE rid=%s ORDER BY idx;" % (self.schema, self.dbprefix, rID)
        return self.fetchAllFromQuery(query)

  

    # --- geometry i/o helper -----------------------------
    def addPolygon(self, tableName, rID, rtype, rNodes, rGeom):
        """ @brief Adds a polygon with the given parameters to the given table
        @param self The class instance
        @param tableName The name of the table to add the polygon to
        @param rID The name of the polygon
        @param rtype The MML-type of the polygon
        @param rNodes The OSM nodes the polygon consists of
        @param rGeom The list of vertices the polygon consists of
        @todo Where is this needed?
        """
        self.cursor.execute("INSERT INTO %s.%s(id, type, nodes, shape) VALUES (%s, '%s', '{{%s}}', ST_GeomFromText('POLYGON((%s))', 4326));" % (self.schema, tableName, rID, rtype, ','.join([str(x) for x in rNodes]), rGeom))
        self.conn.commit()       
    

    def addLineString(self, tableName, rID, rtype, rNodes, rGeom):
        """ @brief Adds a linestring with the given parameters to the given table
        @param self The class instance
        @param tableName The name of the table to add the polygon to
        @param rID The name of the linestring
        @param rtype The MML-type of the linestring
        @param rNodes The OSM nodes the linestring consists of
        @param rGeom The list of vertices the linestring consists of
        @todo Where is this needed?
        """
        self.cursor.execute("INSERT INTO %s.%s(id, type, nodes, shape) VALUES (%s, '%s', '{{%s}}', ST_GeomFromText('LINESTRING(%s)', 4326));" % (self.schema, tableName, rID, rtype, ','.join([str(x) for x in rNodes]), rGeom))
        self.conn.commit()   
    
    def addPoI(self, tableName, rID, rtype, rNode, rGeom):
        """ @brief Adds a PoI with the given parameters to the given table
        @param self The class instance
        @param tableName The name of the table to add the polygon to
        @param rID The name of the PoI
        @param rtype The MML-type of the PoI
        @param rNodes The OSM node the PoI represents
        @param rGeom The vertex of this PoI
        @todo Where is this needed?
        """
        self.cursor.execute("INSERT INTO %s.%s(id, type, node, shape) VALUES (%s, '%s', %s, ST_GeomFromText('POINT(%s)', 4326));" % (self.schema, tableName, rID, rtype, rNode, rGeom))
        self.conn.commit()   
    
    
    
    
