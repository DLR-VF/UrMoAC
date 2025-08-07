#!/usr/bin/env python
# -*- coding: utf-8 -*-
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
# plain parsing to geometry
def test_wkt2geometry_point1(capsys):
    ret = wkt.wkt2geometry("POINT(0 1)")
    assert ret.wkt() == "POINT(0.0 1.0)"

def test_wkt2geometry_point2(capsys):
    ret = wkt.wkt2geometry("Point(0 1)")
    assert ret.wkt() == "POINT(0.0 1.0)"



def test_wkt2geometry_linestring1(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1,2 3)")
    assert ret.wkt() == "LINESTRING(0.0 1.0, 2.0 3.0)"

def test_wkt2geometry_linestring2(capsys):
    ret = wkt.wkt2geometry("LineString(0 1,2 3)")
    assert ret.wkt() == "LINESTRING(0.0 1.0, 2.0 3.0)"

def test_wkt2geometry_linestring3(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1, 2 3)")
    assert ret.wkt() == "LINESTRING(0.0 1.0, 2.0 3.0)"

def test_wkt2geometry_linestring4(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1, 2 3, 4 5)")
    assert ret.wkt() == "LINESTRING(0.0 1.0, 2.0 3.0, 4.0 5.0)"



def test_wkt2geometry_multilinestring1(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1,2 3))")
    assert ret.wkt() == "MULTILINESTRING((0.0 1.0, 2.0 3.0))"

def test_wkt2geometry_multilinestring2(capsys):
    ret = wkt.wkt2geometry("MultiLineString((0 1,2 3))")
    assert ret.wkt() == "MULTILINESTRING((0.0 1.0, 2.0 3.0))"

def test_wkt2geometry_multilinestring3(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3))")
    assert ret.wkt() == "MULTILINESTRING((0.0 1.0, 2.0 3.0))"

def test_wkt2geometry_multilinestring4(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3, 4 5))")
    assert ret.wkt() == "MULTILINESTRING((0.0 1.0, 2.0 3.0, 4.0 5.0))"

def test_wkt2geometry_multilinestring6(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3), (4 5, 6 7))")
    assert ret.wkt() == "MULTILINESTRING((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0))"

def test_wkt2geometry_multilinestring7(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3),(4 5, 6 7))")
    assert ret.wkt() == "MULTILINESTRING((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0))"



def test_wkt2geometry_polygon1(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1,2 3))")
    assert ret.wkt() == "POLYGON((0.0 1.0, 2.0 3.0))"

def test_wkt2geometry_polygon2(capsys):
    ret = wkt.wkt2geometry("Polygon((0 1, 2 3))")
    assert ret.wkt() == "POLYGON((0.0 1.0, 2.0 3.0))"

def test_wkt2geometry_polygon3(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3, 4 5))")
    assert ret.wkt() == "POLYGON((0.0 1.0, 2.0 3.0, 4.0 5.0))"

def test_wkt2geometry_polygon4(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3, 4 5))")
    assert ret.wkt() == "POLYGON((0.0 1.0, 2.0 3.0, 4.0 5.0))"

def test_wkt2geometry_polygon5(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3), (4 5, 6 7))")
    assert ret.wkt() == "POLYGON((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0))"

def test_wkt2geometry_polygon6(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3),(4 5, 6 7))")
    assert ret.wkt() == "POLYGON((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0))"



def test_wkt2geometry_multipolygon1(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1,2 3)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0)))"

def test_wkt2geometry_multipolygon2(capsys):
    ret = wkt.wkt2geometry("MultiPolygon(((0 1, 2 3)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0)))"

def test_wkt2geometry_multipolygon3(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3, 4 5)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0, 4.0 5.0)))"

def test_wkt2geometry_multipolygon4(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3, 4 5)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0, 4.0 5.0)))"

def test_wkt2geometry_multipolygon5(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3), (4 5, 6 7)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0)))"

def test_wkt2geometry_multipolygon6(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3),(4 5, 6 7)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0)))"

def test_wkt2geometry_multipolygon7(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3)),((4 5, 6 7)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0)), ((4.0 5.0, 6.0 7.0)))"

def test_wkt2geometry_multipolygon8(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3)),((4 5, 6 7)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0)), ((4.0 5.0, 6.0 7.0)))"

def test_wkt2geometry_multipolygon9(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3), (4 5, 6 7)),((8 9, 10 11)))")
    assert ret.wkt() == "MULTIPOLYGON(((0.0 1.0, 2.0 3.0), (4.0 5.0, 6.0 7.0)), ((8.0 9.0, 10.0 11.0)))"



