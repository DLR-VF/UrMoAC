#!/usr/bin/env python
# =========================================================
# osm2db.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief OSM data model
# =========================================================


# --- imported modules ------------------------------------
import os, sys, math, copy

script_dir = os.path.dirname( __file__ )
mymodule_dir = os.path.join(script_dir, '..', 'helper')
sys.path.append(mymodule_dir)
from wkt import *
from geom_helper import *



# --- class definitions -----------------------------------
class OSMElement:
    """ @class OSMElement
    @brief The base class for nodes, ways, and relations with an ID an tags
    """
    
    def __init__(self, id):
        """ @brief Initialises the element
        @param self The class instance
        @param id The ID of the element
        """
        self.id = id
        self.tags = {}


    def addTag(self, k, v):
        """ @brief Adds an attribute to the node
        @param self The class instance
        @param k The attribute's key
        @param v The attribute's value
        """
        self.tags[k] = v



# --- OSMNode
class OSMNode(OSMElement):
    """ @class OSMNode
    @brief The representation of an OSM node
    """
    
    def __init__(self, id, pos):
        """ @brief Initialises the node
        @param self The class instance
        @param id The ID of the node
        @param pos The position (latitude, longitude) of the node
        """
        OSMElement.__init__(self, id)
        self.refNum = 0
        self.pos = pos


    def getDescriptionWithPolygons(self):
        """ @brief Returns a description for inserting the object as a polygon
        @param self The class instance
        """
        pxm = self.pos[0]-.0001
        pym = self.pos[1]-.0001
        pxp = self.pos[0]+.0001
        pyp = self.pos[1]+.0001
        poly = []
        poly.append("%s %s" % (pxm, pym))
        poly.append("%s %s" % (pxp, pym))
        poly.append("%s %s" % (pxp, pyp))
        poly.append("%s %s" % (pxm, pyp))
        poly.append("%s %s" % (pxm, pym))
        return [self.id, "node", [poly]]



# --- OSMWay
class OSMWay(OSMElement):
    """ @class OSMWay
    @brief The representation of an OSM way
    """
    
    def __init__(self, id, refs=[], geom=None):
        """ @brief Initialises the node
        @param self The class instance
        @param id The ID of the node
        """
        OSMElement.__init__(self, id)
        self.refs = list(refs)
        self.geom = geom


    def addNodeID(self, nID):
        """ @brief Adds a node (a vertex) to the way
        @param self The class instance
        @param nID The ID of the node to add
        """
        self.refs.append(nID)
        
        
    def buildGeometry(self, nodesMap):
        """ @brief Builds the geometry
        @param self The class instance
        @param nodesMap The nodes storage
        """
        self.geom = list([])
        ok = True
        for r in self.refs:
            if r not in nodesMap:
                print ("Missing node %s while building way %s." % (r, self.id))
                ok = False
                continue
            self.geom.append(nodesMap[r].pos)
        if not ok: self.geom.clear()
        return ok
        

    def getDescriptionWithPolygons(self):
        """ @brief Returns a description for inserting the object as a polygon
        @param self The class instance
        """
        poly = []
        poly = ["%s %s" % (p[0], p[1]) for p in self.geom]
        if poly[0]!=poly[-1]: poly.append(poly[0])
        return [self.id, "way", [poly]]

    

# --- OSMRelation
class PolygonPart():
    """ @class PolygonPart
    @brief A helper class used when computing linestring combinations
    """
    
    def __init__(self, type, geom):
        """ @brief Constructor
        @param type The geometry type
        @param geom The geometry
        """
        self.type = type
        self.geom = list(geom)
        self.possibleFollowers = list([])
    
    
    def __repr__(self):
        """ @brief Returns the string representation
        @return The string representation of this instance
        """
        return "<PolygonPart %s %s %s>" % (self.type, self.geom, self.possibleFollowers)
    

    
# --- OSMRelation
class OSMRelation(OSMElement):
    """ @class OSMWay
    @brief The representation of an OSM relation
    """

    def __init__(self, id, geom=None):
        """ @brief Initialises the relation
        @param self The class instance
        @param id The ID of the relation
        """
        OSMElement.__init__(self, id)
        self.members = []
        self.geom = geom
        
        
    def addMember(self, mID, mType, mRole):
        """ @brief Adds amember to the relation
        @param self The class instance
        @param mID The ID of the member
        @param mType The type (node / way / relation) of the member
        @param mRole The role of the member
        """
        self.members.append([mID, mType, mRole])
        
        
    # --- geometry computation
    def _computePossibleConsecutions(self, roleItems):
        """ @brief Computes possible follower items
        
        The possible followers are stored in the item's "possibleFollowers"
        variable
        
        @param self The class instance
        @param roleItems The list of the parts of a relation
        """
        for i,w1 in enumerate(roleItems):
            for j,w2 in enumerate(roleItems):
                if i==j: continue
                if w1.geom[-1]==w2.geom[0]: # ok, plain continuation
                    w1.possibleFollowers.append([j, 0])
                if w1.geom[-1]==w2.geom[-1]: # second would have to be mirrored
                    w1.possibleFollowers.append([j, 1])
                if w1.geom[0]==w2.geom[0]: # this would have to be mirrored
                    w1.possibleFollowers.append([j, 2])
                if w1.geom[0]==w2.geom[-1]: # both have to be mirrored
                    w1.possibleFollowers.append([j, 3])        
        

    def _extendCombinations(self, roleItems, combinations, seen):
        """ @brief Extends the given combinations by next possible items
        @param self The class instance
        @param roleItems The list of the parts of a relation
        @param combinations The list of combinations to extend
        @param seen The list seen objects per combination
        """
        newCombinations = []
        newSeen = []
        for ic,c in enumerate(combinations):
            lastElement = c[-1][-1]
            added = 0
            if len(lastElement)!=0:
                for next in roleItems[lastElement[0]].possibleFollowers:
                    if next[0] in seen[ic]:
                        continue
                    # could be a valid continuation
                    combi = copy.deepcopy(c)
                    combi[-1].append(next)
                    newCombinations.append(combi)
                    newSeen.append(set(seen[ic]))
                    newSeen[-1].add(next[0])
                    added += 1
            if added!=0:
                continue
            # we could not find a continuation - maybe a new polygon should be started
            for ie,e in enumerate(roleItems):
                if ie in seen[ic]:
                    # already used
                    continue 
                combi = copy.deepcopy(c)
                combi.append([])
                combi[-1].append([ie, 0])
                newCombinations.append(combi)
                newSeen.append(set(seen[ic]))
                newSeen[-1].add(ie)
                added += 1
                break
        return newCombinations, newSeen

                
    def _computeCombinations(self, roleItems):
        """ @brief Computes possible combinations of the given geometry items
        @param self The class instance
        @param roleItems The list of the parts of a relation
        """
        combinations = []
        seen = []
        # initialise combinations 
        for e in roleItems[0].possibleFollowers:
            combinations.append([[e]])
            seen.append(set())
            seen[-1].add(e[0])
        if len(combinations)==0:
            combinations.append([[[0, 0]]])
            seen.append(set())
            seen[-1].add(0)
        while True:
            combinations, seen = self._extendCombinations(roleItems, combinations, seen)
            if len(combinations)==0:
                break # invalid (no combinations were found)
            if len(seen[0])==len(roleItems):
                break # all visited
        return combinations
        
    
    def _checkPolygonValidities(self, id, roleItems, combinations, closeIfNeeded):
        """ @brief Builds polygons and checks their validities
        @param self The class instance
        @param roleItems The list of the parts of a relation
        @param combinations The list of combinations to extend
        @param closeIfNeeded If true, an unclosed polygon will be closed
        """
        valids = []
        ngeomss = []
        for q,combi in enumerate(combinations):
            valid = True
            seen = set()
            ngeoms = []
            for poly in combi:
                mirrorNext = False
                lastGeom = None
                ngeom = []
                # build the polygon and check whether the items are valid continuations
                for i,w1 in enumerate(poly):
                    if w1[0] in seen:
                        valid = False
                        break
                    seen.add(w1[0])
                    geom = roleItems[w1[0]].geom
                    if len(ngeom)>0 and geom[-1]==ngeom[-1]: # !!!
                        geom = list(reversed(list(geom)))
                    mirrorNext = (w1[1]&1)
                    if len(ngeom)>0:
                        if lastGeom and lastGeom[-1]!=geom[0]:
                            valid = False
                            break
                        ngeom.extend(geom[1:])
                    else:
                        ngeom.extend(geom)
                    lastGeom = geom

                # check whether the polygon is closed
                if ngeom[0]!=ngeom[-1]:
                    if closeIfNeeded:
                        ngeom.append(ngeom[0])
                    else:
                        valid = False
                
                # check whether the polygon does not contain crossing boundaries
                for q1 in range(1, len(ngeom)):
                    p11 = ngeom[q1-1]
                    p12 = ngeom[q1]
                    for q2 in range(q1+1, len(ngeom)):
                        p21 = ngeom[q2-1]
                        p22 = ngeom[q2]
                        intersection = lineLineIntersection(p11[0], p11[1], p12[0], p12[1], p21[0], p21[1], p22[0], p22[1])
                        if intersection and (distance(intersection, p11)<.00001 or distance(intersection, p12)<.00001):
                            intersection = None
                        if intersection:
                            valid = False
                            break
                # add geom
                ngeoms.append(ngeom)
            valids.append(valid)
            ngeomss.append(ngeoms)
        return ngeomss, valids

        
        
    def buildGeometry(self, area):
        """ @brief Builds the geometry
        @param self The class instance
        @param area The objects storage
        """
        ok = True
        # build roles
        roles = {}
        for m in self.members:
            if m[2] not in roles:
                roles[m[2]] = []
        #
        closeIfNeeded = False
        for m in self.members:
            if m[1]=="node":
                n = area.getNode(m[0])
                if not n:
                    print ("Missing node %s in relation %s" % (m[0], self.id))
                    ok = False
                    continue
                roles[m[2]].append(PolygonPart(GeometryType.POINT, n.pos))
            elif m[1]=="way":
                w = area.getWay(m[0])
                if not w:
                    print ("Missing way %s in relation %s" % (m[0], self.id))
                    ok = False
                    closeIfNeeded = True
                    continue
                ok = w.buildGeometry(area._nodes)
                roles[m[2]].append(PolygonPart(GeometryType.LINESTRING, list(w.geom))) # well, we append it even if it's not ok!?
            elif m[1]=="relation" or m[1]=="rel":
                r = area.getRelation(m[0])
                if not r:
                    print ("Missing relation %s in relation %s" % (m[0], self.id))
                    ok = False
                    continue
                ok = r.buildGeometry(area)
                roles[m[2]].append(PolygonPart(GeometryType.MULTIPOLYGON, dict(r.geom)))
        # build roles geometries
        self.geom = {}
        for role in roles:
            self.geom[role] = []
            if len(roles[role])<2:
                combinations = [[[[0, 0]]]]
            else:
                self._computePossibleConsecutions(roles[role])
                combinations = self._computeCombinations(roles[role])
            ngeomss, valids = self._checkPolygonValidities(self.id, roles[role], combinations, closeIfNeeded)
            # check
            if True not in valids:
                print ("Invalid geometry in relation %s" % self.id)
                continue
            polys = ngeomss[valids.index(True)]
            for poly in polys:
                asign = signed_area(poly)
                if (asign>0 and role=="inner") or (asign<0 and role=="outer"):
                    poly.reverse()
                self.geom[role].append(poly)
        return ok
        

    def getDescriptionWithPolygons(self):
        """ @brief Returns a description for inserting the object as a polygon
        @param self The class instance
        """
        polys = []
        if not self.geom:
            print ("Missing geometry in relation %s" % self.id)
            return [self.id, "rel", []]
        if "outer" in self.geom and len(self.geom["outer"])!=0:
            poly = ["%s %s" % (p[0], p[1]) for p in self.geom["outer"][0]]
            if poly[0]!=poly[-1]: poly.append(poly[0])
            polys.append(poly)
        for r in self.geom:
            if r=="outer":
                continue
            for ppoly in self.geom[r]:
                poly = ["%s %s" % (p[0], p[1]) for p in ppoly]
                if poly[0]!=poly[-1]: poly.append(poly[0])
                polys.append(poly)
        return [self.id, "rel", polys]


    
# --- OSMArea
class OSMArea:
    """! @class OSMArea
    @brief A complete area consisting of nodes, way, and elements
    """
    
    def __init__(self):
        """ @brief Initialises the area
        @param self The class instance
        """
        self._nodes = {}
        self._ways = {}
        self._relations = {}
    
    
    # --- getter
    def getNode(self, nodeID):
        """ @brief Returns the named node of None if it's not known
        @param self The class instance
        @param nodeID The id of the node to return
        """
        if nodeID not in self._nodes:
            return None
        return self._nodes[nodeID]
        
        
    def getWay(self, wayID):
        """ @brief Returns the named way of None if it's not known
        @param self The class instance
        @param wayID The id of the way to return
        """
        if wayID not in self._ways:
            return None
        return self._ways[wayID]
        
        
    def getRelation(self, relID):
        """ @brief Returns the named relation of None if it's not known
        @param self The class instance
        @param relID The id of the relation to return
        """
        if relID not in self._relations:
            return None
        return self._relations[relID]
        
        
    # --- adder
    def addNode(self, node):
        """ @brief Adds a node
        @param self The class instance
        @param node The node to add
        """
        self._nodes[node.id] = node
        
    
    def addWay(self, way):
        """ @brief Adds a way
        @param self The class instance
        @param way The way to add
        """
        self._ways[way.id] = way
    
    
    def addRelation(self, rel):
        """ @brief Adds a relation
        @param self The class instance
        @param rel The rel to add
        """
        self._relations[rel.id] = rel
    
    
    def buildGeometries(self):
        """ @brief Builds geometries for ways and releations
        @param self The class instance
        """
        for w in self._ways:
            self._ways[w].buildGeometry(self._nodes)
        for r in self._relations:
            self._relations[r].buildGeometry(self)




