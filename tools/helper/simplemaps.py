#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# =============================================================================
# simplemaps.py
#
# Author: Daniel Krajzewicz
# Date:   28.11.2020
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2020-2024 Institute of Transport Research,
#                         German Aerospace Center
# All rights reserved.
# =============================================================================
"""Some simple maps for data categorization."""
# =============================================================================

# --- meta --------------------------------------------------------------------
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright (c) 2020-2024 Institute of Transport Research, German Aerospace Center"
__credits__    = [ "Daniel Krajzewicz" ]
__license__    = "EPL2.0"
__version__    = "0.8"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Development"


# --- function definitions ----------------------------------------------------
def addToSimpleMap(tMap, tAttr):
    if tAttr not in tMap:
        tMap[tAttr] = 1
    else:
        tMap[tAttr] = tMap[tAttr] + 1
    
def addToDoubleMap(tMap, tAttr1, tAttr2):
    if tAttr1 not in tMap:
        tMap[tAttr1] = {}
    addToSimpleMap(tMap[tAttr1], tAttr2)
    
def addToTripleMap(tMap, tAttr1, tAttr2, tAttr3):
    if tAttr1 not in tMap:
        tMap[tAttr1] = {}
    addToDoubleMap(tMap[tAttr1], tAttr2, tAttr3)



def addToValueMap(tMap, tAttr, value):
    if tAttr not in tMap:
        tMap[tAttr] = value
    else:
        tMap[tAttr] = tMap[tAttr] + value
 


def addToHistogram(hist, binSize, tAttr):
    i = int(tAttr / binSize)
    while(len(hist)<=i):
        hist.append(0)
    hist[i] = hist[i] + 1


def addToHistogramMap(tMap, tAttr1, binSize, tAttr2):
    if tAttr1 not in tMap:
        tMap[tAttr1] = []
    addToHistogram(tMap[tAttr1], binSize, tAttr2)
  
  
  

  