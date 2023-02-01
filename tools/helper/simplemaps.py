#!/usr/bin/env python
# =========================================================
# simplemaps.py
#
# @author Daniel Krajzewicz
# @date 28.11.2020
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Methods for generating maps and histograms
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2020-2023 DLR Institute of Transport Research
# All rights reserved.
# =========================================================
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
  
  
  

  