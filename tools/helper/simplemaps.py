#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Some simple maps for data categorization."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2020-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.2"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Production"
# ===========================================================================
# - https://github.com/DLR-VF/UrMoAC
# - https://www.dlr.de/vf
# ===========================================================================


# --- function definitions --------------------------------------------------
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
  
  
  

  