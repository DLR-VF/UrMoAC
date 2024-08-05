#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""OSM data model."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2016-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.0"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Production"
# ===========================================================================
# - https://github.com/DLR-VF/UrMoAC
# - https://www.dlr.de/vf
# ===========================================================================


# --- imports ---------------------------------------------------------------
import os
import sys
import math
import copy

script_dir = os.path.dirname( __file__ )
mymodule_dir = os.path.join(script_dir, '..', 'helper')
sys.path.append(mymodule_dir)
from wkt import *
from geom_helper import *


# --- class definitions -----------------------------------------------------
# --- OSMElement
class OSMElement:
    """The base class for nodes, ways, and relations with an ID an tags."""
    
    def __init__(self, id):
        """Initialises the element
        :param id: The ID of the element
        :type id: int
        """
        self.id = id
        self.tags = {}
        self._ok = True


    def add_tag(self, k, v):
        """Adds an attribute to the node
        :param k: The key name
        :type k: str
        :param v: The value
        :type v: str
        """
        self.tags[k] = v




# --- OSMNode
class OSMNode(OSMElement):
    """The representation of an OSM node."""
    
    def __init__(self, id, pos):
        """Initialises the node
        :param id: The ID of the node
        :type id: int
        :param pos: The key name
        :type pos: tuple[float, float]
        """
        OSMElement.__init__(self, id)
        self.refNum = 0
        self.pos = pos


    def get_description_with_polygons(self):
        """Returns a description for inserting the object as a multipolygon and geomtrycollection
        :return: A tuple of id, type, polygons, and WKT representation
        :rtype: tuple[int, str, list[str], str]
        """
        return [self.id, "node", [], "POINT(%s %s)" % (self.pos[0], self.pos[1])]


    def get_geometry_type(self):
        """Returns the type of this element (here: always GeometryType.POINT)
        :return: The geometry type (GeometryType.POINT)
        :rtype: GeometryType
        """
        return GeometryType.POINT



# --- OSMWay
class OSMWay(OSMElement):
    """The representation of an OSM way."""
    
    def __init__(self, id, refs=[], geom=None):
        """Initialises the way
        :param id: The ID of the way
        :type id: int
        :param refs: The list of referenced nodes
        :type refs: list[int]
        :param geom: The geometry
        :type geom: list[tuple[float, float]]
        """
        OSMElement.__init__(self, id)
        self.refs = list(refs)
        self.geom = geom


    def add_node_id(self, nID):
        """Adds a node (a vertex) to the way
        :param id: The ID of the referenced node
        :type id: int
        """
        self.refs.append(nID)
        
        
    def build_geometry(self, area):
        """Builds the geometry
        :param area: The area to get information from
        :type area: OSMArea
        """
        if self._ok and self.geom!=None:
            return True
        self.geom = []
        for r in self.refs:
            if r not in area._nodes:
                print ("Missing node %s while building way %s." % (r, self.id))
                self._ok = False
                continue
            self.geom.append(area._nodes[r].pos)
        if not self._ok:
            self.geom = None
        return self._ok
        

    def get_description_with_polygons(self):
        """Returns a description for inserting the object as a multipolygon and geomtrycollection
        :return: A tuple of id, type, polygons, and WKT representation
        :rtype: tuple[int, str, list[str], str]
        """
        if self.geom[0]==self.geom[-1]:
            p = ",".join(["%s %s" % (p[0], p[1]) for p in self.geom])
            return [self.id, "way", [[self.geom]], "POLYGON((" + p + "))"]
        return [self.id, "way", None, "LINESTRING(" + ",".join(["%s %s" % (p[0], p[1]) for p in self.geom]) + ")"]


    def get_geometry_type(self):
        """Returns the type of this element
        :return: GeometryType.POLYGON if the way is closed, otherwise GeometryType.LINESTRING
        :rtype: GeometryType
        """
        if self.geom[0]==self.geom[-1]:
            return GeometryType.POLYGON
        return GeometryType.LINESTRING
    
    

    

    
# --- OSMRelation
class OSMRelation(OSMElement):
    """The representation of an OSM relation."""

    def __init__(self, id, geom=None):
        """Initialises the relation
        :param id: The ID of the relation
        :type id: int
        :param geom: The geometry
        :type geom: list[list[list[tuple[float, float]]]

        @param self The class instance
        @param id The ID of the relation
        """
        OSMElement.__init__(self, id)
        self.members = []
        self.geom = geom
        
        
    def add_member(self, mID, mType, mRole):
        """Adds amember to the relation
        :param mID: The ID of the member
        :type mID: int
        :param mType: The type (node / way / relation) of the member
        :type mType: str
        :param mRole: The role of the member
        :type mRole: str
        """
        self.members.append([mID, mType, mRole])
        
        
    # --- geometry computation
    def _compute_possible_consecutions(self, roleItems):
        """Computes possible follower items
        
        The possible followers are stored in the item's "possibleFollowers"
        variable
        :param roleItems: 
        :type roleItems: 
        """
        # initialise consecution list
        for i in roleItems:
            i.possibleFollowers = []
        # fill consecution list
        for i,w1 in enumerate(roleItems):
            if w1.get_geometry_type()!=GeometryType.LINESTRING:
                continue
            for j,w2 in enumerate(roleItems):
                if i==j: continue
                if w2.get_geometry_type()!=GeometryType.LINESTRING:
                    continue
                if w1.geom[-1]==w2.geom[0]: # ok, plain continuation
                    w1.possibleFollowers.append([j, 0])
                if w1.geom[-1]==w2.geom[-1]: # second would have to be mirrored
                    w1.possibleFollowers.append([j, 1])
                if w1.geom[0]==w2.geom[0]: # this would have to be mirrored
                    w1.possibleFollowers.append([j, 2])
                if w1.geom[0]==w2.geom[-1]: # both have to be mirrored
                    w1.possibleFollowers.append([j, 3])        


    def _join_unambiguous(self, roleItems):
        """Joins subparts with no alternative consecution
        :param roleItems: 
        :type roleItems: 
        """
        changed = True
        while changed:
            changed = False
            # initialise list of referenced items
            # those referenced by one only are unambiguous
            referencedBy = [[] for i in range(0, len(roleItems))]
            for i,w1 in enumerate(roleItems):
                for r in w1.possibleFollowers:
                    referencedBy[r[0]].append([i, r[1]])
            newRoleItems = list(roleItems)
            for i,w1 in enumerate(newRoleItems):
                if len(referencedBy[i])<1 or len(referencedBy[i])>2:
                    continue
                if len(referencedBy[i])==2:
                    if referencedBy[i][0][0]!=referencedBy[i][1][0] or ((referencedBy[i][0][1]!=0 or referencedBy[i][1][1]!=3) and (referencedBy[i][0][1]!=3 and referencedBy[i][1][1]!=0)):
                        continue
                referrer = referencedBy[i][0][0]
                # ok, I assume this should not happen - that an item is an 
                # unambiguous follower of the referrer but can be the follower
                # of two other ones...
                if len(roleItems[referrer].possibleFollowers)!=1 and (len(roleItems[referrer].possibleFollowers)!=2 or roleItems[referrer].possibleFollowers[0][0]!=roleItems[referrer].possibleFollowers[1][0]):
                    continue
                # umpf, we skip some mirroring possibilities; they have to be solved later
                mirror = referencedBy[i][0][1]
                if mirror==2 or mirror==3:
                    continue
                # extend the referrer by this item
                geom = w1.geom
                if mirror==1:
                    geom = list(reversed(geom))
                newRoleItems[referrer].geom.extend(geom[1:])
                del newRoleItems[i]
                changed = True
                break
            if changed:
                self._compute_possible_consecutions(newRoleItems)
                roleItems = newRoleItems
        return newRoleItems
                

    def _extendCombinations(self, roleItems, combinations, seen):
        """Extends the given combinations by next possible items
        :param roleItems: The list of the parts of a relation
        :type roleItems: 
        :param combinations: The list of combinations to extend
        :type combinations: 
        :param seen: The list seen objects per combination
        :type seen: 
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

                
    def _compute_combinations(self, roleItems):
        """Computes possible combinations of the given geometry items
        :param roleItems: The list of the parts of a relation
        :type roleItems: 
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
            if len(combinations)>100000:
                print ("Maximum number of combinations reached when processing relation %s!" % self.id)
                break
            combinations, seen = self._extendCombinations(roleItems, combinations, seen)
            if len(combinations)==0:
                break # invalid (no combinations were found)
            if len(seen[0])==len(roleItems):
                break # all visited
        return combinations
        
    
    def _get_polygon_validities(self, id, roleItems, combinations, closeIfNeeded):
        """Builds polygons and checks their validities
        :param roleItems: The list of the parts of a relation
        :type roleItems: 
        :param combinations: The list of combinations to extend
        :type combinations: 
        :param closeIfNeeded: If true, an unclosed polygon will be closed
        :type closeIfNeeded: 
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

    
    def _compute_cliques(self, items):
        """Divides the given list of elements into cliques
        
        A clique contains only items which reference other
        :param items: The list of items
        :type items: 
        """
        cliques = []
        for i,item in enumerate(items):
            item.clique = i
            cliques.append(set())
            cliques[-1].add(item)
        changed = True
        while changed:
            changed = False
            for ic,c in enumerate(cliques):
                for i in c:
                    for f in i.possibleFollowers:
                        ic2 = items[f[0]].clique
                        if ic2==ic:
                            continue
                        for i2 in cliques[ic2]:
                            c.add(i2)
                            i2.clique = ic
                        cliques[ic2] = set()
                        items[f[0]].clique = ic
                        changed = True
                    if changed: break
        return cliques
    
        
    def build_geometry(self, area):
        """Builds the geometry
        :param area: The objects storage
        :type area: OSMArea
        """
        if self._ok and self.geom!=None:
            return True
        # build roles
        roles = {} # !!! could be a direct member
        #print (self.members)
        for m in self.members:
            if m[2] not in roles:
                roles[m[2]] = []
        #
        closeIfNeeded = False
        for m in self.members:
            if m[1]=="node":
                n = area.get_node(m[0])
                if not n:
                    print ("Missing node %s in relation %s" % (m[0], self.id))
                    self._ok = False
                    continue
                roles[m[2]].append(n)
            elif m[1]=="way":
                w = area.get_way(m[0])
                if not w:
                    print ("Missing way %s in relation %s" % (m[0], self.id))
                    self._ok = False
                    closeIfNeeded = True
                    continue
                self._ok &= w.build_geometry(area)
                if self._ok:
                    roles[m[2]].append(w) # well, we append it even if it's not ok!?
            elif m[1]=="relation" or m[1]=="rel":
                if self.id==m[0]:
                    print ("Skipping self-referencing relation in %s" % self.id)
                    continue
                r = area.get_relation(m[0])
                if not r:
                    print ("Missing relation %s in relation %s" % (m[0], self.id))
                    self._ok = False
                    continue
                self._ok &= r.build_geometry(area)
                if not self._ok:
                    print ("Broken geometry of relation %s in relation %s" % (m[0], self.id))
                    self._ok = False
                    continue
                roles[m[2]].append(r)
        if not self._ok:
            return False
        # build roles geometries
        self.geom = {}
        for role in roles:
            if len(roles[role])==0: # may occur if an element is missing
                continue
            self.geom[role] = []
            # add all elements that do not need any continuations (are a closed polygon or point)
            leftItems = []
            for r in roles[role]:
                if r.get_geometry_type()==GeometryType.POINT:
                    self.geom[role].append(r)
                elif r.get_geometry_type()==GeometryType.POLYGON:
                    asign = signed_area(r.geom)
                    if (asign>0 and role=="inner") or (asign<0 and role=="outer"):
                        r.geom.reverse()
                    self.geom[role].append(r)
                elif r.get_geometry_type()==GeometryType.GEOMETRYCOLLECTION:
                    self.geom[role].append(r)
                else:
                    leftItems.append(r)
            # check continuations for complex items
            if len(leftItems)>0:
                self._compute_possible_consecutions(leftItems)
                leftItems = self._join_unambiguous(leftItems)
                self._compute_possible_consecutions(leftItems)
                cliques = self._compute_cliques(leftItems)
                for c in cliques:
                    if len(c)==0:
                        continue
                    entries = list(c)
                    self._compute_possible_consecutions(entries)
                    combinations = self._compute_combinations(entries)
                    ngeomss, valids = self._get_polygon_validities(self.id, entries, combinations, closeIfNeeded)
                    # check whether we could successfully combine items
                    if True not in valids:
                        # ok, we could not determine meaningful element combinations
                        # simply add the items
                        for item in leftItems:
                            self.geom[role].append(item)
                        continue
                    # build a new item
                    polys = ngeomss[valids.index(True)]
                    for poly in polys:
                        asign = signed_area(poly)
                        if asign<0:
                            poly.reverse()
                        self.geom[role].append(OSMWay(-1, [], poly))
        return self._ok
        

    def get_geometry_type(self):
        """Returns the type of this element (here: always GeometryType.GEOMETRYCOLLECTION)
        :return: The geometry type (GeometryType.GEOMETRYCOLLECTION)
        :rtype: GeometryType
        """
        return GeometryType.GEOMETRYCOLLECTION


    def get_description_with_polygons(self):
        """Returns a description for inserting the object as a multipolygon and geomtrycollection
        :return: A tuple of id, type, polygons, and WKT representation
        :rtype: tuple[int, str, list[str], str]
        """
        ret = []
        polys = []
        seen = set()
        if "outer" in self.geom:
            for o in self.geom["outer"]:
                id, type, outerpolys, outer = o.get_description_with_polygons()
                if not outer.startswith("POLYGON"):
                    continue
                if signed_area(outerpolys[0][0])<0: outerpolys[0][0].reverse()
                seen.add(o)
                if "inner" in self.geom:
                    inners = []
                    for i in self.geom["inner"]:
                        if i in seen:
                            continue
                        if i.get_geometry_type()!=GeometryType.LINESTRING and i.get_geometry_type()!=GeometryType.POLYGON:
                            continue
                        id, type, innerpolys, inner = i.get_description_with_polygons()
                        if not inner.startswith("POLYGON"):
                            continue
                        if polygon_in_polygon(innerpolys[0][0], outerpolys[0][0]):
                            if signed_area(innerpolys[0][0])>0: innerpolys[0][0].reverse()
                            inners.append(innerpolys[0][0])
                            seen.add(i)
                    if len(inners)!=0:
                        outerpolys[0].extend(inners)
                    polys.append(outerpolys[0])
                else:
                    polys.append(outerpolys[0])
                ret.append(outer)
        for r in self.geom:
            for g in self.geom[r]:
                if g in seen:
                    continue
                id, type, npolys, geom2 = g.get_description_with_polygons()
                if npolys!=None: polys.extend(npolys)
                if geom2.startswith("GEOMETRYCOLLECTION"):
                    geom2 = geom2[geom2.find("(")+1:-1]
                ret.append(geom2)
        ret = ",".join(ret)
        return [self.id, "rel", polys, ret]


    
# --- OSMArea
class OSMArea:
    """A complete area consisting of nodes, way, and elements."""
    
    def __init__(self):
        """Initialises the area"""
        self._nodes = {}
        self._ways = {}
        self._relations = {}
    
    
    # --- getter
    def get_node(self, nID):
        """Returns the named node of None if it's not known
        :param nID: The ID of the node
        :type nID: int
        :return: The named node or None if the node is not known
        :rtype: OSMNode
        """
        if nID not in self._nodes:
            return None
        return self._nodes[nID]
        
        
    def get_way(self, wID):
        """Returns the named way of None if it's not known
        :param wID: The ID of the way
        :type wID: int
        :return: The named way or None if the node is not known
        :rtype: OSMWay
        """
        if wID not in self._ways:
            return None
        return self._ways[wID]
        
        
    def get_relation(self, rID):
        """Returns the named relation of None if it's not known
        :param rID: The ID of the relation
        :type rID: int
        :return: The named relation or None if the node is not known
        :rtype: OSMRelation
        """
        if rID not in self._relations:
            return None
        return self._relations[rID]
        
        
    # --- adder
    def add_node(self, node):
        """Adds a node
        :param node: The node to add
        :type node: OSMNode
        """
        self._nodes[node.id] = node
        
    
    def add_way(self, way):
        """Adds a way
        :param node: The way to add
        :type node: OSMWay
        """
        self._ways[way.id] = way
    
    
    def add_relation(self, rel):
        """Adds a relation
        :param rel: The relaltion to add
        :type rel: OSMRelation
        """
        self._relations[rel.id] = rel
    
    
    def build_geometries(self):
        """Builds geometries for ways and releations"""
        fw = 0
        for w in self._ways:
            if not self._ways[w].build_geometry(self):
                fw += 1
        fr = 0
        for r in self._relations:
            if not self._relations[r].build_geometry(self):
                fr += 1
        return fw,fr




