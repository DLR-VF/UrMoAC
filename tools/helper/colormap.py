#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Colormap helper."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2015-2024, Institute of Transport Research, German Aerospace Center (DLR)"
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
def toHex(val):
    """Converts the given value (0-255) into its hexadecimal representation"""
    hex = "0123456789abcdef"
    return hex[int(val / 16)] + hex[int(val - int(val / 16) * 16)]


def toFloat(val):
    """Converts the given value (0-255) into its hexadecimal representation"""
    hex = "0123456789abcdef"
    return float(hex.find(val[0]) * 16 + hex.find(val[1]))


def toColor(val, colormap):
    """Converts the given value (0-1) into a color definition parseable by matplotlib"""
    for i in range(0, len(colormap) - 1):
        if colormap[i + 1][0] > val:
            scale = (val - colormap[i][0]) / \
                (colormap[i + 1][0] - colormap[i][0])
            r = colormap[i][1][0] + \
                (colormap[i + 1][1][0] - colormap[i][1][0]) * scale
            g = colormap[i][1][1] + \
                (colormap[i + 1][1][1] - colormap[i][1][1]) * scale
            b = colormap[i][1][2] + \
                (colormap[i + 1][1][2] - colormap[i][1][2]) * scale
            return "#" + toHex(r) + toHex(g) + toHex(b)
    return "#" + toHex(colormap[-1][1][0]) + toHex(colormap[-1][1][1]) + toHex(colormap[-1][1][2])


def parseColorMap(mapDef):
    ret = []
    defs = mapDef.split(",")
    for d in defs:
        (value, color) = d.split(":")
        r = color[1:3]
        g = color[3:5]
        b = color[5:7]
        ret.append((float(value), (toFloat(r), toFloat(g), toFloat(b))))
    return ret

