#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Tests for wkt.py"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2025, Institute of Transport Research, German Aerospace Center (DLR)"
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
    assert type(ret)==type(wkt.Point(None))
    assert ret._shape == [0, 1]

def test_wkt2geometry_point2(capsys):
    ret = wkt.wkt2geometry("Point(0 1)")
    assert type(ret)==type(wkt.Point(None))
    assert ret._shape == [0, 1]

def test_wkt2geometry_point3(capsys):
    ret = wkt.wkt2geometry("POINT(EMPTY)")
    assert type(ret)==type(wkt.Point(None))
    assert ret._shape is None


def test_wkt2geometry_linestring1(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1,2 3)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape == [[0, 1], [2, 3]]

def test_wkt2geometry_linestring2(capsys):
    ret = wkt.wkt2geometry("LineString(0 1,2 3)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape == [[0, 1], [2, 3]]

def test_wkt2geometry_linestring3(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1, 2 3)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape == [[0, 1], [2, 3]]

def test_wkt2geometry_linestring4(capsys):
    ret = wkt.wkt2geometry("LINESTRING(0 1, 2 3, 4 5)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape == [[0, 1], [2, 3], [4, 5]]

def test_wkt2geometry_linestring5(capsys):
    ret = wkt.wkt2geometry("LINESTRING(EMPTY)")
    assert type(ret)==type(wkt.LineString(None))
    assert ret._shape is None


def test_wkt2geometry_multilinestring1(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1,2 3))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3]]]

def test_wkt2geometry_multilinestring2(capsys):
    ret = wkt.wkt2geometry("MultiLineString((0 1,2 3))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3]]]

def test_wkt2geometry_multilinestring3(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3]]]

def test_wkt2geometry_multilinestring4(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3, 4 5))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3], [4, 5]]]

def test_wkt2geometry_multilinestring6(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3), (4 5, 6 7))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2geometry_multilinestring7(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING((0 1, 2 3),(4 5, 6 7))")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2geometry_multilinestring8(capsys):
    ret = wkt.wkt2geometry("MULTILINESTRING(EMPTY)")
    assert type(ret)==type(wkt.MultiLineString(None))
    assert ret._shape is None


def test_wkt2geometry_polygon1(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1,2 3))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3]]]

def test_wkt2geometry_polygon2(capsys):
    ret = wkt.wkt2geometry("Polygon((0 1, 2 3))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3]]]

def test_wkt2geometry_polygon3(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3, 4 5))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3], [4, 5]]]

def test_wkt2geometry_polygon4(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3, 4 5))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3], [4, 5]]]

def test_wkt2geometry_polygon5(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3), (4 5, 6 7))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2geometry_polygon6(capsys):
    ret = wkt.wkt2geometry("POLYGON((0 1, 2 3),(4 5, 6 7))")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2geometry_polygon7(capsys):
    ret = wkt.wkt2geometry("POLYGON(EMPTY)")
    assert type(ret)==type(wkt.Polygon(None))
    assert ret._shape is None


def test_wkt2geometry_multipolygon1(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1,2 3)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]]]]

def test_wkt2geometry_multipolygon2(capsys):
    ret = wkt.wkt2geometry("MultiPolygon(((0 1, 2 3)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]]]]

def test_wkt2geometry_multipolygon3(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3, 4 5)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3], [4, 5]]]]

def test_wkt2geometry_multipolygon4(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3, 4 5)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3], [4, 5]]]]

def test_wkt2geometry_multipolygon5(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3), (4 5, 6 7)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]], [[4, 5], [6, 7]]]]

def test_wkt2geometry_multipolygon6(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3),(4 5, 6 7)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]], [[4, 5], [6, 7]]]]

def test_wkt2geometry_multipolygon7(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3)),((4 5, 6 7)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]]], [[[4, 5], [6, 7]]]]

def test_wkt2geometry_multipolygon8(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3)),((4 5, 6 7)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]]], [[[4, 5], [6, 7]]]]

def test_wkt2geometry_multipolygon9(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(((0 1, 2 3), (4 5, 6 7)),((8 9, 10 11)))")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape == [[[[0, 1], [2, 3]], [[4, 5], [6, 7]]], [[[8, 9], [10, 11]]]]

def test_wkt2geometry_multipolygon10(capsys):
    ret = wkt.wkt2geometry("MULTIPOLYGON(EMPTY)")
    assert type(ret)==type(wkt.MultiPolygon(None))
    assert ret._shape is None


# plain parsing to lists
def test_wkt2lists_point1(capsys):
    ret = wkt.wkt2lists("POINT(0 1)")
    assert type(ret)==type(list())
    assert ret == [0, 1]

def test_wkt2lists_point2(capsys):
    ret = wkt.wkt2lists("Point(0 1)")
    assert type(ret)==type(list())
    assert ret == [0, 1]

def test_wkt2lists_point3(capsys):
    ret = wkt.wkt2lists("POINT(EMPTY)")
    assert ret==None


def test_wkt2lists_linestring1(capsys):
    ret = wkt.wkt2lists("LINESTRING(0 1,2 3)")
    assert type(ret)==type(list())
    assert ret == [[0, 1], [2, 3]]

def test_wkt2lists_linestring2(capsys):
    ret = wkt.wkt2lists("LineString(0 1,2 3)")
    assert type(ret)==type(list())
    assert ret == [[0, 1], [2, 3]]

def test_wkt2lists_linestring3(capsys):
    ret = wkt.wkt2lists("LINESTRING(0 1, 2 3)")
    assert type(ret)==type(list())
    assert ret == [[0, 1], [2, 3]]

def test_wkt2lists_linestring4(capsys):
    ret = wkt.wkt2lists("LINESTRING(0 1, 2 3, 4 5)")
    assert type(ret)==type(list())
    assert ret == [[0, 1], [2, 3], [4, 5]]

def test_wkt2lists_linestring5(capsys):
    ret = wkt.wkt2lists("LINESTRING(EMPTY)")
    assert ret==None


def test_wkt2lists_multilinestring1(capsys):
    ret = wkt.wkt2lists("MULTILINESTRING((0 1,2 3))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]]]

def test_wkt2lists_multilinestring2(capsys):
    ret = wkt.wkt2lists("MultiLineString((0 1,2 3))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]]]

def test_wkt2lists_multilinestring3(capsys):
    ret = wkt.wkt2lists("MULTILINESTRING((0 1, 2 3))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]]]

def test_wkt2lists_multilinestring4(capsys):
    ret = wkt.wkt2lists("MULTILINESTRING((0 1, 2 3, 4 5))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3], [4, 5]]]

def test_wkt2lists_multilinestring6(capsys):
    ret = wkt.wkt2lists("MULTILINESTRING((0 1, 2 3), (4 5, 6 7))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2lists_multilinestring7(capsys):
    ret = wkt.wkt2lists("MULTILINESTRING((0 1, 2 3),(4 5, 6 7))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2lists_multilinestring8(capsys):
    ret = wkt.wkt2lists("MULTILINESTRING(EMPTY)")
    assert ret==None


def test_wkt2lists_polygon1(capsys):
    ret = wkt.wkt2lists("POLYGON((0 1,2 3))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]]]

def test_wkt2lists_polygon2(capsys):
    ret = wkt.wkt2lists("Polygon((0 1, 2 3))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]]]

def test_wkt2lists_polygon3(capsys):
    ret = wkt.wkt2lists("POLYGON((0 1, 2 3, 4 5))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3], [4, 5]]]

def test_wkt2lists_polygon4(capsys):
    ret = wkt.wkt2lists("POLYGON((0 1, 2 3, 4 5))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3], [4, 5]]]

def test_wkt2lists_polygon5(capsys):
    ret = wkt.wkt2lists("POLYGON((0 1, 2 3), (4 5, 6 7))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2lists_polygon6(capsys):
    ret = wkt.wkt2lists("POLYGON((0 1, 2 3),(4 5, 6 7))")
    assert type(ret)==type(list())
    assert ret == [[[0, 1], [2, 3]], [[4, 5], [6, 7]]]

def test_wkt2lists_polygon7(capsys):
    ret = wkt.wkt2lists("POLYGON(EMPTY)")
    assert ret==None


def test_wkt2lists_multipolygon1(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1,2 3)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]]]]

def test_wkt2lists_multipolygon2(capsys):
    ret = wkt.wkt2lists("MultiPolygon(((0 1, 2 3)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]]]]

def test_wkt2lists_multipolygon3(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3, 4 5)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3], [4, 5]]]]

def test_wkt2lists_multipolygon4(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3, 4 5)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3], [4, 5]]]]

def test_wkt2lists_multipolygon5(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3), (4 5, 6 7)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]], [[4, 5], [6, 7]]]]

def test_wkt2lists_multipolygon6(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3),(4 5, 6 7)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]], [[4, 5], [6, 7]]]]

def test_wkt2lists_multipolygon7(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3)),((4 5, 6 7)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]]], [[[4, 5], [6, 7]]]]

def test_wkt2lists_multipolygon8(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3)),((4 5, 6 7)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]]], [[[4, 5], [6, 7]]]]

def test_wkt2lists_multipolygon9(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(((0 1, 2 3), (4 5, 6 7)),((8 9, 10 11)))")
    assert type(ret)==type(list())
    assert ret == [[[[0, 1], [2, 3]], [[4, 5], [6, 7]]], [[[8, 9], [10, 11]]]]

def test_wkt2lists_multipolygon10(capsys):
    ret = wkt.wkt2lists("MULTIPOLYGON(EMPTY)")
    assert ret==None


