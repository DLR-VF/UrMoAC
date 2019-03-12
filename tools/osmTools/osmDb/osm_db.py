#!/usr/bin/env python

import os, string, sys
import datetime

from optparse import OptionParser
from xml.sax import saxutils, make_parser, handler

import psycopg2

from pylab import *
import matplotlib.patches as patches
from matplotlib.collections import PatchCollection

def parsePOINT2XY(which, scale=1.):
  which = which[6:-1]
  xy = which.split(" ")
  return [ float(xy[0])/scale, float(xy[1])/scale ]
  
  
  


class OSMDB:
  def __init__(self, dbprefix, conn, cursor):
    self.dbprefix = dbprefix
    self.conn = conn
    self.cursor = cursor
    
  def fetchAllFromQuery(self, query):
    self.cursor.execute(query)
    return self.cursor.fetchall()      
    
  def getWay(self, wID):
    query = "SELECT * from osm."+self.dbprefix+"_way WHERE id='%s';" % wID;
    return self.fetchAllFromQuery(query);
  
  def getWayKV_forID(self, sid):
    query = "SELECT * from osm."+self.dbprefix+"_wtag WHERE id='%s';" % sid;
    return self.fetchAllFromQuery(query);
  
  def getWayKV_withMatchingKey(self, key):
    query = "SELECT * from osm."+self.dbprefix+"_wtag WHERE k='%s';" % key;
    return self.fetchAllFromQuery(query);
  
  def getWayKV_withMatchingKeyValue(self, key, value):
    query = "SELECT * from osm."+self.dbprefix+"_wtag WHERE k='%s' AND v='%s';" % (key, value);
    return self.fetchAllFromQuery(query);
  
  def getNode(self, nID):
    query = "SELECT * from osm."+self.dbprefix+"_node WHERE id='%s';" % nID;
    return self.fetchAllFromQuery(query);
  
  def getNodeKV_forID(self, nid):
    query = "SELECT * from osm."+self.dbprefix+"_ntag WHERE id='%s';" % nid;
    return self.fetchAllFromQuery(query);
  
  def getNodeKV_withMatchingKey(self, key):
    query = "SELECT * from osm."+self.dbprefix+"_ntag WHERE k='%s';" % key;
    return self.fetchAllFromQuery(query);
  
  def getNodeKV_withMatchingKeyValue(self, key, value):
    query = "SELECT * from osm."+self.dbprefix+"_ntag WHERE (k='%s' AND v='%s');" % (key, value);
    return self.fetchAllFromQuery(query);

  def getNodesWGeom(self, nIDs):
    query = "SELECT id, ST_AsText(pos) from osm."+self.dbprefix+"_node WHERE id in ("+','.join([str(x) for x in nIDs])+");";
    return self.fetchAllFromQuery(query);
  
  def getNodes_preserveOrder(self, nIDs, verbose=0):
    rep = ','.join([str(x) for x in nIDs])
    #orderRep = ','.join(["id="+str(x)+" DESC" for x in nIDs])
    # http://stackoverflow.com/questions/866465/sql-order-by-the-in-value-list
    query = "SELECT id, ST_AsText(pos) from osm."+self.dbprefix+"_node WHERE id in ("+rep+");"#" ORDER BY "+orderRep+";";
    nodes = self.fetchAllFromQuery(query);
    n2pos = {}
    for n in nodes:
      n2pos[n[0]] = n[1]
    ret = []
    for n in nIDs:
      if n not in n2pos:
        if verbose>0:
            print "Warning: node %s is not known" % n
        continue
      ret.append([n, n2pos[n]])
    return ret  

  def getRelationKV_forID(self, sid):
    query = "SELECT * from osm."+self.dbprefix+"_rtag WHERE id='%s';" % sid;
    return self.fetchAllFromQuery(query);
  
  def getRelationKV_withMatchingKey(self, key):
    query = "SELECT * from osm."+self.dbprefix+"_rtag WHERE k='%s';" % key;
    return self.fetchAllFromQuery(query);
  
  def getRelationKV_withMatchingKeyValue(self, key, value):
    query = "SELECT * from osm."+self.dbprefix+"_rtag WHERE k='%s' AND v='%s';" % (key, value);
    return self.fetchAllFromQuery(query);
  
  def getMembers_forID(self, sid):
    query = "SELECT * from osm."+self.dbprefix+"_member WHERE rid=%s ORDER BY idx;" % (sid);
    return self.fetchAllFromQuery(query);
  
  def addPolygon_with_level(self, tableName, rID, rtype, level, rNodes, rGeom):
      self.cursor.execute("INSERT INTO osm."+tableName+"(id, type, level, nodes,  shape) VALUES (%s, '%s', '%s', '{{%s}}', ST_GeomFromText('POLYGON((%s))', 4326));" % (rID, rtype, level, ','.join([str(x) for x in rNodes]), rGeom))
      self.conn.commit()  
  

         
  def addPolygon(self, tableName, rID, rtype, rNodes, rGeom):
      self.cursor.execute("INSERT INTO osm."+tableName+"(id, type, nodes, shape) VALUES (%s, '%s', '{{%s}}', ST_GeomFromText('POLYGON((%s))', 4326));" % (rID, rtype, ','.join([str(x) for x in rNodes]), rGeom))
      self.conn.commit()  

  def addLineString(self, tableName, rID, rtype, rNodes, rGeom):
    self.cursor.execute("INSERT INTO osm."+tableName+"(id, type, nodes, shape) VALUES (%s, '%s', '{{%s}}', ST_GeomFromText('LINESTRING(%s)', 4326));" % (rID, rtype, ','.join([str(x) for x in rNodes]), rGeom))
    self.conn.commit()   
    
  def addPoI(self, tableName, rID, rtype, rNode, rGeom):
    self.cursor.execute("INSERT INTO osm."+tableName+"(id, type, node, shape) VALUES (%s, '%s', %s, ST_GeomFromText('POINT(%s)', 4326));" % (rID, rtype, rNode, rGeom))
    self.conn.commit()   
    
  def tableExists(self, schema, name):
    # http://stackoverflow.com/questions/20582500/how-to-check-if-a-table-exists-in-a-given-schema
    self.cursor.execute("""SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE  table_schema = '%s' AND table_name = '%s');""" % (schema, name))
    self.conn.commit()   
    ret = self.cursor.fetchall()
    return ret[0][0]
    
    
    
