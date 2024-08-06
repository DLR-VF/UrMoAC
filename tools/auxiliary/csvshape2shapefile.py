#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Converts a csv shape file into a shapefile.

Call with 
   csvshape2shapefile.py <INPUT_CSV_SHAPES> <SHAPEFILE_OUTPUT_PREFIX>"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2024, Institute of Transport Research, German Aerospace Center (DLR)"
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
def csvshape2shapefile(input_file, output_prefix):
    """Parses the contents from the csv shape file with the given name and 
    writes them into the set of files with the given name that make up a 
    shapefile.
    
    :param input_file: The name of the input file
    :type input_file: str
    :param output_prefix: The output prefix
    :type output_prefix: str
    """
    objects = []
    fdi = open(input_file)
    numPoints = 0
    numPolys = 0
    withVar = 0
    for l in fdi:
        l = l.strip()
        if l=="" or l[0]=='#':
            continue
        vals = l.strip().split(";")
        id = vals[0]
    
        p = 1
        shape = []
        while p+1<len(vals):
            pos = [float(vals[p]), float(vals[p+1])]
            shape.append(pos)
            p += 2
        if shape[0]!=shape[-1]:
            shape.append(shape[0])
        if len(shape)==1:
            numPoints += 1
        else:
            numPolys += 1
        var = None
        if p<len(vals):
            var = float(vals[p-2])
            withVar += 1
        objects.append([id, shape, var])
    fdi.close()

    # check
    if numPoints!=0 and numPolys!=0:
        raise ValueError("Objects must have the same geometry type.")
    if withVar!=0 and len(objects)!=withVar:
        raise ValueError("Use either objects with or without var.")
  
    # write  
    if numPolys!=0:
        type = shapefile.POLYGON
    else:
        type = shapefile.POINT
    w = shapefile.Writer(output_prefix, type)
    w.field('id', 'N', 10, 0)
    if withVar!=0:
        w.field('var', 'N', 10, 0)
    for o in objects:
        l = [o[0]]
        if withVar!=0:
            l.append(o[2])
        if type==shapefile.POLYGON:
            w.poly([o[1]])
        else:
            w.point(o[1][0][0], o[1][0][1])
        w.record(*l)
    w.close()


# -- main check
if __name__ == '__main__':
    if len(sys.argv)<3:
        print ("Error: Parameter is missing\nPlease run with:\n  csvshape2shapefile.py <INPUT_CSV_SHAPES> <SHAPEFILE_OUTPUT_PREFIX>")
        sys.exit(1)
    csvshape2shapefile(sys.argv[1], sys.argv[2])
    
