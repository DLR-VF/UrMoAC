#!/usr/bin/env python
# =========================================================
# colorhelper.py
#
# @author Daniel Krajzewicz
# @date 27.05.2015
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Some helpers for dealing with (matplotlib) colors
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2015-2023 DLR Institute of Transport Research
# All rights reserved.
# =========================================================
from pylab import *


def lighten(c1, c2):
  c = "#"
  for i in range(0, 3):
    cc1 = int(c1[1+i*2:3+i*2], 16)
    cc2 = int(c2[1+i*2:3+i*2], 16)
    c = c + "%0.2x" % min(255, cc1 + cc2)
    #c = c + hex(s)[2:]
  return c

def toFloat(val):
    """Converts the given value (0-255) into its hexadecimal representation"""
    hex = "0123456789abcdef"
    return float(hex.find(val[0])*16 + hex.find(val[1]))


def toColor(val, colormap):
    """Converts the given value (0-1) into a color definition parseable by matplotlib"""
    for i in range(0, len(colormap)-1):
        if colormap[i+1][0]>val:
            scale = (val - colormap[i][0]) / (colormap[i+1][0] - colormap[i][0])
            r = colormap[i][1][0] + (colormap[i+1][1][0] - colormap[i][1][0]) * scale 
            g = colormap[i][1][1] + (colormap[i+1][1][1] - colormap[i][1][1]) * scale 
            b = colormap[i][1][2] + (colormap[i+1][1][2] - colormap[i][1][2]) * scale 
            return "#" + toHex(r) + toHex(g) + toHex(b)
    return "#" + toHex(colormap[-1][1][0]) + toHex(colormap[-1][1][1]) + toHex(colormap[-1][1][2]) 

  
def parseColorMap(mapDef):
    somedict = {}
    ret = { "red": [], "green":[], "blue":[] }
    defs = mapDef.split(",")
    lastValue = 0
    for d in defs:
        (value, color) = d.split(":")
        value = float(value)
        r = color[1:3]
        g = color[3:5]
        b = color[5:7]
        ret["red"].append((value, toFloat(r)/255., toFloat(r)/255.))
        ret["green"].append((value, toFloat(g)/255., toFloat(g)/255.))
        ret["blue"].append((value, toFloat(b)/255., toFloat(b)/255.))
        lastValue = value
    colormap = matplotlib.colors.LinearSegmentedColormap("CUSTOM", ret, 1024)
    return colormap
    
      