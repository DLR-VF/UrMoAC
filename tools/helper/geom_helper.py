#!/usr/bin/env python
# =========================================================
# geom_helper.py
# @author Daniel Krajzewicz, Simon Nieland
# @date 12.08.2022
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Some geometry helpers
# =========================================================


# --- imported modules ------------------------------------
import math



# --- method definitions ----------------------------------
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





