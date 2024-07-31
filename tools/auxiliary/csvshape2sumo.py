#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Converts a csv shape file into a SUMO shapes file.

Call with 
   csvshape2sumo.py <INPUT_CSV_SHAPES> <SUMO_SHAPE_OUTPUT>"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.0"
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
def csvshape2sumo(input_file, output_file):
    """Parses the contents from the csv shape file with the given name and 
    writes them into a SUMO additional (shapes) file.
    
    :param input_file: The name of the input file
    :type input_file: str
    :param output_file: The name of the output file
    :type output_file: str
    """
    fdi = open(input_file)
    fdo = open(output_file, "w")
    fdo.write("<shapes>\n")
    first = True
    for l in fdi:
        if first or len(l.strip())==0:
            first = False
            continue
        vals = l.strip().split(";")
        p = 1
        shape = []
        while p<len(vals):
            pos = [float(vals[p]), float(vals[p+1])]
            shape.append(pos)
            p += 2
        if len(shape)==1:
            fdo.write('    <poi id="%s" color="1,1,0" x="%s" y="%s"/>\n' % (vals[0], shape[0][0], shape[0][1]))
        else:
            for i,p in enumerate(shape):
                shape[i] = "%s,%s" % (p[0], p[1])
            shape = " ".join(shape)
            fdo.write('    <poly id="%s" color="1,1,0" shape="%s"/>\n' % (vals[0], shape))
    fdo.write("</shapes>\n")
    fdo.close()
    fdi.close()


# -- main check
if __name__ == '__main__':
    if len(sys.argv)<3:
        print ("Error: Parameter is missing\nPlease run with:\n  csvshape2sumo.py <INPUT_CSV_SHAPES> <SUMO_SHAPE_OUTPUT>")
        sys.exit(1)
    csvshape2sumo(sys.argv[1], sys.argv[2])
