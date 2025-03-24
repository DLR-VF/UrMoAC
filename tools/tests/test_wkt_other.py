#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Tests for wkt.py"""
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


# --- imports -----------------------------------------------------------------
import sys
import os
import unittest
sys.path.append(os.path.join(os.path.split(__file__)[0], "..", "helper"))
import wkt

# add error cases (false format)
# add None as a parameter to get an empty wkt


# --- test functions ----------------------------------------------------------
# boundary
def test_point_boundary1(capsys):
    ret = wkt.wkt2geometry("POINT(0 1)")
    assert type(ret)==type(wkt.Point(None))
    assert ret._shape == [0, 1]
    bounds = ret.bounds()
    assert type(bounds)==type(list())
    assert bounds == [0, 1, 0, 1]

def test_linestring_bounds1(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1,2 3)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape == [[0, 1], [2, 3]]
    bounds = ret.bounds()
    assert type(bounds)==type(list())
    assert bounds == [0, 1, 2, 3]

def test_multilinestring_bounds1(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1,2 3))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3]]]
    bounds = ret.bounds()
    assert type(bounds)==type(list())
    assert bounds == [0, 1, 2, 3]

def test_polygon_bounds1(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1,2 3))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3]]]
    bounds = ret.bounds()
    assert type(bounds)==type(list())
    assert bounds == [0, 1, 2, 3]

def test_multipolygon_bounds1(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1,2 3)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]]]]
    bounds = ret.bounds()
    assert type(bounds)==type(list())
    assert bounds == [0, 1, 2, 3]


# shape
def test_point_shape1(capsys):
    ret = wkt.wkt2geometry("POINT(0 1)")
    assert type(ret)==type(wkt.Point(None))
    assert ret._shape == ret.shape()

def test_linestring_shape1(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1,2 3)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape == ret.shape()

def test_multilinestring_shape1(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1,2 3))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == ret.shape()

def test_polygon_shape1(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1,2 3))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == ret.shape()

def test_multipolygon_shape1(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1,2 3)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == ret.shape()


