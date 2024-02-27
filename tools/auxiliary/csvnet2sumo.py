#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# =============================================================================
# csvnet2sumo.py
#
# Author: Daniel Krajzewicz
# Date:   01.05.2023
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2023-2024 Institute of Transport Research,
#                         German Aerospace Center
# All rights reserved.
# =============================================================================
"""Converts a csv net file into SUMO nodes and edges files.

Call with 
   csvnet2sumo.py <INPUT_CSV_NET> <SUMO_OUTPUT_PREFIX>"""
# =============================================================================

# --- imported modules --------------------------------------------------------
import sys


# --- meta --------------------------------------------------------------------
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright (c) 2023-2024 Institute of Transport Research, German Aerospace Center"
__credits__    = [ "Daniel Krajzewicz" ]
__license__    = "EPL2.0"
__version__    = "0.8"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Development"


# --- function definitions ----------------------------------------------------
def csvnet2sumo(input_file, output_prefix):
    """Parses the contents from the csv net file with the given name and writes
    them into an edges and a nodes file which names are built using the given
    prefix."""
    fdi = open(input_file)
    fdoE = open(output_prefix + "_edges.xml", "w")
    fdoN = open(output_prefix + "_nodes.xml", "w")
    fdoE.write("<edges>\n")
    fdoN.write("<nodes>\n")
    nodes = set()
    first = True
    for l in fdi:
        if first:
            first = False
            continue
        vals = l.strip().split(";")
        if vals[1] not in nodes:
            fdoN.write('    <node id="%s" x="%s" y="%s"/>\n' % (vals[1], vals[8], vals[9]))
            nodes.add(vals[1])
        if vals[2] not in nodes:
            fdoN.write('    <node id="%s" x="%s" y="%s"/>\n' % (vals[2], vals[-2], vals[-1]))
            nodes.add(vals[2])
        allowed = []
        if vals[3]=="true": allowed.append("pedestrian")
        if vals[4]=="true": allowed.append("bicycle")
        if vals[5]=="true": allowed.append("passenger")
        fdoE.write('    <edge id="%s" from="%s" to="%s" numlanes="1" speed="%s" length="%s" allowed="%s" spreadType="center"/>\n' % (vals[0], vals[1], vals[2], float(vals[6])/3.6, vals[7], " ".join(allowed)))
    fdoE.write("</edges>\n")
    fdoN.write("</nodes>\n")
    fdi.close()
    fdoE.close()
    fdoN.close()


# -- main check
if __name__ == '__main__':
    csvnet2sumo(sys.argv[1], sys.argv[2])
    
