#!/usr/bin/env python
# =========================================================
# colormap.py
# @author Daniel Krajzewicz
# @date 28.11.2020
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief A simple method for defining own colormaps
# =========================================================
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

