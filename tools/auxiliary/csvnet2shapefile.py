#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Converts a csv net file into a shapefile.

Call with 
   csvnet2shapefile.py <INPUT_CSV_NET> <SHAPEFILE_OUTPUT_PREFIX>"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2025, Institute of Transport Research, German Aerospace Center (DLR)"
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


# --- imports ---------------------------------------------------------------
import sys
import shapefile


# --- function definitions --------------------------------------------------
def csvnet2shapefile(input_file, output_prefix):
    """Parses the contents from the csv net file with the given name and 
    writes them into the set of files with the given name that make up a 
    shapefile.
    
    :param input_file: The name of the input file
    :type input_file: str
    :param output_prefix: The output prefix
    :type output_prefix: str
    """
    fdi = open(input_file)
    w = shapefile.Writer(output_prefix, shapefile.POLYLINE)
    w.field('oid', 'C', 40, 40)
    w.field('nodefrom', 'N', 10, 0)
    w.field('nodeto', 'N', 10, 0)
    w.field('mode_walk', 'L')
    w.field('mode_bike', 'L')
    w.field('mode_mit', 'L')
    w.field('vmax','N', 5, 4)
    w.field('length','N', 5, 4)
    nodes = set()
    first = True
    for l in fdi:
        if first:
            first = False
            continue
        vals = l.strip().split(";")
        l = [vals[0], int(vals[1]), int(vals[2]), 1 if vals[3]=="true" else 0, 1 if vals[4]=="true" else 0, 1 if vals[5]=="true" else 0, float(vals[6]), float(vals[7])]
        p = 8
        shape = []
        while p<len(vals):
            pos = [float(vals[p]), float(vals[p+1])]
            shape.append(pos)
            p += 2
        w.line([shape])
        w.record(*l)
    fdi.close()
    w.close()


# -- main check
if __name__ == '__main__':
    if len(sys.argv)<3:
        print ("Error: Parameter is missing\nPlease run with:\n  csvnet2shapefile.py <INPUT_CSV_NET> <SHAPEFILE_OUTPUT_PREFIX>")
        sys.exit(1)
    csvnet2shapefile(sys.argv[1], sys.argv[2])
    sys.exit(0)
