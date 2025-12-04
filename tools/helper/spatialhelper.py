#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Methods for computing boundaries and centers."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2022-2025, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.10.0"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Production"
# ===========================================================================
# - https://github.com/DLR-VF/UrMoAC
# - https://www.dlr.de/vf
# ===========================================================================


# --- function definitions --------------------------------------------------
def getBounds(positions):
    if positions==None or len(positions)==0:
        return None
    p = positions[0]
    if isinstance(p[0], float):
        bounds = [p[0], p[1], p[0], p[1]]
        for p in positions:
            bounds[0] = min(p[0], bounds[0])
            bounds[1] = min(p[1], bounds[1])
            bounds[2] = max(p[0], bounds[2])
            bounds[3] = max(p[1], bounds[3])
    else:
        bounds = getBounds(positions[0])
        for p in positions:
            tbounds = getBounds(p)
            if tbounds is None:
                continue
            bounds[0] = min(tbounds[0], bounds[0])
            bounds[1] = min(tbounds[1], bounds[1])
            bounds[2] = max(tbounds[2], bounds[2])
            bounds[3] = max(tbounds[3], bounds[3])
    return bounds

def getCenter(positions):
    bounds = getBounds(positions)
    if bounds==None:
        return None
    return ((bounds[0]+bounds[2])/2., (bounds[1]+bounds[3])/2.)


def geometries_bounds(geoms):
    if geoms==None or len(geoms)==0:
        return None
    print (geoms[0]._type)
    bounds = geoms[0].bounds()
    for g in geoms:
        tbounds = g.bounds()
        if tbounds==None:
            continue
        bounds[0] = min(tbounds[0], bounds[0])
        bounds[1] = min(tbounds[1], bounds[1])
        bounds[2] = max(tbounds[2], bounds[2])
        bounds[3] = max(tbounds[3], bounds[3])
    return bounds
    


