#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Converts a csv net file into SUMO nodes and edges files.

Call with 
   csvnet2sumo.py <INPUT_CSV_NET> <SUMO_OUTPUT_PREFIX>"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2026, Institute of Transport Research, German Aerospace Center (DLR)"
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


# --- imports ---------------------------------------------------------------
import sys


# --- function definitions --------------------------------------------------
def csvnet2sumo(input_file, output_prefix):
    """Parses the contents from the csv net file with the given name and writes
    them into an edges and a nodes file which names are built using the given
    prefix.
    
    :param input_file: The name of the input file
    :type input_file: str
    :param output_prefix: The output prefix
    :type output_prefix: str
    """
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
            fdoN.write(f'    <node id="{vals[1]}" x="{vals[8]}" y="{vals[9]}"/>\n')
            nodes.add(vals[1])
        if vals[2] not in nodes:
            fdoN.write(f'    <node id="{vals[2]}" x="{vals[-2]}" y="{vals[-1]}"/>\n')
            nodes.add(vals[2])
        allowed = []
        if vals[3]=="true": allowed.append("pedestrian")
        if vals[4]=="true": allowed.append("bicycle")
        if vals[5]=="true": allowed.append("passenger")
        fdoE.write(f'    <edge id="{vals[0]}" from="{vals[1]}" to="{vals[2]}" numlanes="1" speed="{float(vals[6])/3.6}" length="{vals[7]}" allowed="{' '.join(allowed)}" spreadType="center"/>\n')
    fdoE.write("</edges>\n")
    fdoN.write("</nodes>\n")
    fdi.close()
    fdoE.close()
    fdoN.close()


# -- main check
if __name__ == '__main__':
    if len(sys.argv)<3:
        print ("Error: Parameter is missing\nPlease run with:\n  csvnet2sumo.py <INPUT_CSV_NET> <SUMO_OUTPUT_PREFIX>")
        sys.exit(1)
    csvnet2sumo(sys.argv[1], sys.argv[2])
    
