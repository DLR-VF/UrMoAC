#!/usr/bin/env python
# =========================================================
# osmdb.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Helper methods for dealing with our OSM-database representation
# =========================================================
import os, string, sys
import datetime
from optparse import OptionParser
from xml.sax import saxutils, make_parser, handler
import psycopg2



class OSMDB:
  def __init__(self, schema, dbprefix, conn, cursor):
    self.schema = schema
    self.dbprefix = dbprefix
    self.conn = conn
    self.cursor = cursor
    
  def fetchAllFromQuery(self, query):
    self.cursor.execute(query)
    return self.cursor.fetchall()      
    
  def getWay(self, wID):
    query = "SELECT * from %s.%s_way WHERE id='%s';" % (self.schema, self.dbprefix, wID)
    return self.fetchAllFromQuery(query);
  
  def getWayKV_forID(self, sid):
    query = "SELECT * from %s.%s_wtag WHERE id='%s';" % (self.schema, self.dbprefix, sid)
    return self.fetchAllFromQuery(query);
  
  def getWayKV_withMatchingKey(self, key):
    query = "SELECT * from %s.%s_wtag WHERE k='%s';" % (self.schema, self.dbprefix, key)
    return self.fetchAllFromQuery(query);
  
  def getWayKV_withMatchingKeyValue(self, key, value):
    query = "SELECT * from %s.%s_wtag WHERE k='%s' AND v='%s';" % (self.schema, self.dbprefix, key, value)
    return self.fetchAllFromQuery(query);
  
  def getNode(self, nID):
    query = "SELECT * from %s.%s_node WHERE id='%s';" % (self.schema, self.dbprefix, nID)
    return self.fetchAllFromQuery(query);
  
  def getNodeKV_forID(self, nid):
    query = "SELECT * from %s.%s_ntag WHERE id='%s';" % (self.schema, self.dbprefix, nid)
    return self.fetchAllFromQuery(query);
  
  def getNodeKV_withMatchingKey(self, key):
    query = "SELECT * from %s.%s_ntag WHERE k='%s';" % (self.schema, self.dbprefix, key)
    return self.fetchAllFromQuery(query);
  
  def getNodeKV_withMatchingKeyValue(self, key, value):
    query = "SELECT * from %s.%s_ntag WHERE (k='%s' AND v='%s');" % (self.schema, self.dbprefix, key, value)
    return self.fetchAllFromQuery(query);

  def getNodesWGeom(self, nIDs):
    query = "SELECT id, ST_AsText(pos) from %s.%s_node WHERE id in ("+','.join([str(x) for x in nIDs])+");" % (self.schema, self.dbprefix)
    return self.fetchAllFromQuery(query);
  
  def getNodes_preserveOrder(self, nIDs):
    rep = ','.join([str(x) for x in nIDs])
    #orderRep = ','.join(["id="+str(x)+" DESC" for x in nIDs])
    # http://stackoverflow.com/questions/866465/sql-order-by-the-in-value-list
    query = "SELECT id, ST_AsText(pos) from %s.%s_node WHERE id in (%s);" % (self.schema, self.dbprefix, rep)
    nodes = self.fetchAllFromQuery(query);
    n2pos = {}
    for n in nodes:
      n2pos[n[0]] = n[1]
    ret = []
    for n in nIDs:
      if n not in n2pos:
        print "Warning: node %s is not known" % n
        continue
      ret.append([n, n2pos[n]])
    return ret  

  def getRelationKV_forID(self, sid):
    query = "SELECT * from %s.%s_rtag WHERE id='%s';" % (self.schema, self.dbprefix, sid)
    return self.fetchAllFromQuery(query);
  
  def getRelationKV_withMatchingKey(self, key):
    query = "SELECT * from %s.%s_rtag WHERE k='%s';" % (self.schema, self.dbprefix, key)
    return self.fetchAllFromQuery(query);
  
  def getRelationKV_withMatchingKeyValue(self, key, value):
    query = "SELECT * from %s.%s_rtag WHERE k='%s' AND v='%s';" % (self.schema, self.dbprefix, key, value)
    return self.fetchAllFromQuery(query);
  
  def getMembers_forID(self, sid):
    query = "SELECT * from %s.%s_member WHERE rid=%s ORDER BY idx;" % (self.schema, self.dbprefix, sid)
    return self.fetchAllFromQuery(query);
  

  

         
  def addPolygon(self, tableName, rID, rtype, rNodes, rGeom):
    self.cursor.execute("INSERT INTO %s.%s(id, type, nodes, shape) VALUES (%s, '%s', '{{%s}}', ST_GeomFromText('POLYGON((%s))', 4326));" % (self.schema, tableName, rID, rtype, ','.join([str(x) for x in rNodes]), rGeom))
    self.conn.commit()       
    
  def addLineString(self, tableName, rID, rtype, rNodes, rGeom):
    self.cursor.execute("INSERT INTO %s.%s(id, type, nodes, shape) VALUES (%s, '%s', '{{%s}}', ST_GeomFromText('LINESTRING(%s)', 4326));" % (self.schema, tableName, rID, rtype, ','.join([str(x) for x in rNodes]), rGeom))
    self.conn.commit()   
    
  def addPoI(self, tableName, rID, rtype, rNode, rGeom):
    self.cursor.execute("INSERT INTO %s.%s(id, type, node, shape) VALUES (%s, '%s', %s, ST_GeomFromText('POINT(%s)', 4326));" % (self.schema, tableName, rID, rtype, rNode, rGeom))
    self.conn.commit()   
    
  def tableExists(self, schema, name):
    # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
    self.cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name ='%s');""" % (schema, name))
    self.conn.commit()   
    ret = self.cursor.fetchall()
    return ret[0][0]
    
    
    
