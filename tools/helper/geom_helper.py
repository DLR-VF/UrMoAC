#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# =============================================================================
# geom_helper.py
#
# Author: Daniel Krajzewicz
# Date:   12.08.2022
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2022-2024 Institute of Transport Research,
#                         German Aerospace Center
# All rights reserved.
# =============================================================================
"""Some geometry helper."""
# =============================================================================

# --- imported modules --------------------------------------------------------
import math


# --- meta --------------------------------------------------------------------
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright (c) 2022-2024 Institute of Transport Research, German Aerospace Center"
__credits__    = [ "Daniel Krajzewicz" ]
__license__    = "EPL2.0"
__version__    = "0.8"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Development"



# --- function definitions ----------------------------------------------------
def signed_area(poly):
    """! brief Returns the orientation of the polygon
    
    Taken from https://gis.stackexchange.com/questions/298290/checking-if-vertices-of-polygon-are-in-clockwise-or-anti-clockwise-direction-in
    with adaptations.
    
    @param poly The polygon to evaluate
    """
    return sum(poly[i][0]*(poly[i+1][1]-poly[i-1][1]) for i in range(1, len(poly)-1))/2.0    


def lineLineIntersection(Ax1, Ay1, Ax2, Ay2, Bx1, By1, Bx2, By2):
    """ @brief Returns the intersection position of two lines
    
    @return A (x, y) tuple or None if there is no intersection
    """
    d = (By2 - By1) * (Ax2 - Ax1) - (Bx2 - Bx1) * (Ay2 - Ay1)
    if d:
        uA = ((Bx2 - Bx1) * (Ay1 - By1) - (By2 - By1) * (Ax1 - Bx1)) / d
        uB = ((Ax2 - Ax1) * (Ay1 - By1) - (Ay2 - Ay1) * (Ax1 - Bx1)) / d
    else:
        return None
    if not(0 <= uA <= 1 and 0 <= uB <= 1):
        return None
    x = Ax1 + uA * (Ax2 - Ax1)
    y = Ay1 + uA * (Ay2 - Ay1)
    return x, y


def distance(p1, p2):
    """ @brief Returns the distance between two points
    @param p1 First point
    @param p2 Second point
    """
    dx = p1[0] - p2[0]
    dy = p1[1] - p2[1]
    return math.sqrt(dx*dx + dy*dy)


def point_in_polygon(point, polygon):
    """
    https://www.algorithms-and-technologies.com/de/punkt_in_polygon/Python
    Raycasting Algorithm to find out whether a point is in a given polygon.
    Performs the even-odd-rule Algorithm to find out whether a point is in a given polygon.
    This runs in O(n) where n is the number of edges of the polygon.
    @param polygon: an array representation of the polygon where polygon[i][0] is the x Value of the i-th point and polygon[i][1] is the y Value.
    @param point:   an array representation of the point where point[0] is its x Value and point[1] is its y Value
    @return: whether the point is in the polygon (not on the edge, just turn < into <= and > into >= for that)
    """
    # A point is in a polygon if a line from the point to infinity crosses the polygon an odd number of times
    odd = False
    # For each edge (In this case for each point of the polygon and the previous one)
    i = 0
    j = len(polygon) - 1
    while i < len(polygon) - 1:
        i = i + 1
        # If a line from the point into infinity crosses this edge
        # One point needs to be above, one below our y coordinate
        # ...and the edge doesn't cross our Y corrdinate before our x coordinate (but between our x coordinate and infinity)
        if (((polygon[i][1] > point[1]) != (polygon[j][1] > point[1])) 
            and (point[0] < ( (polygon[j][0] - polygon[i][0]) * (point[1] - polygon[i][1]) / (polygon[j][1] - polygon[i][1])) + polygon[i][0])):
            # Invert odd
            odd = not odd
        j = i
    # If the number of crossings was odd, the point is in the polygon
    return odd


def polygon_in_polygon(small, big):
    """ @brief Returns whether the first polygon is completely within the second polygon
    @param small The polygon to test whether it is completely within the other one
    @param big The polygon within which the first one should be
    @return Whether the first polygon is completely within the second one
    """
    for p in small:
        if not point_in_polygon(p, big):
            return False
    return True
