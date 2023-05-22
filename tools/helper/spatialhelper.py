#!/usr/bin/env python
# =========================================================
# spatialhelper.py
#
# @author Daniel Krajzewicz
# @date 19.01.2022
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Helper methods for dealing with spatial data
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2016-2023 DLR Institute of Transport Research
# All rights reserved.
# =========================================================


# --- imported modules ------------------------------------
def getBounds(positions):
    if positions==None or len(positions)==0:
        return None
    p = positions[0]
    bounds = [p[0], p[1], p[0], p[1]]
    for p in positions:
        bounds[0] = min(p[0], bounds[0])
        bounds[1] = min(p[1], bounds[1])
        bounds[2] = max(p[0], bounds[2])
        bounds[3] = max(p[1], bounds[3])
    return bounds

def getCenter(positions):
    bounds = getBounds(positions)
    if bounds==None:
        return None
    return ((bounds[0]+bounds[2])/2., (bounds[1]+bounds[3])/2.)

