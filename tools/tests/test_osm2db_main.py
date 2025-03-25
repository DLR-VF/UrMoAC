#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Tests for osmdb_buildStructures.py"""
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


# --- imports -----------------------------------------------------------------
import sys
import os
import unittest
sys.path.append(os.path.join(os.path.split(__file__)[0], "..", "osm"))
import osm2db


# --- test functions ----------------------------------------------------------
def test_main_empty(capsys):
    """Test behaviour if no arguments are given"""
    try:
        osm2db.main([])
        assert False # pragma: no cover
    except SystemExit as e:
        assert type(e)==type(SystemExit())
        assert e.code==2
    captured = capsys.readouterr()
    assert captured.out == ""
    assert captured.err.replace("__main__.py", "osm2db.py") == """usage: osm2db [-h] [-c FILE] [--version] [-R] [-A] [-v] OSM-database OSM-file
osm2db: error: the following arguments are required: OSM-database, OSM-file
"""


def test_main_help(capsys):
    """Test behaviour if no arguments are given"""
    try:
        osm2db.main(["--help"])
        assert False # pragma: no cover
    except SystemExit as e:
        assert type(e)==type(SystemExit())
        assert e.code==0
    captured = capsys.readouterr()
    assert captured.err == ""
    assert captured.out.replace("__main__.py", "osm2db.py") == """usage: osm2db [-h] [-c FILE] [--version] [-R] [-A] [-v] OSM-database OSM-file

Imports an OSM file into a PostGIS database

positional arguments:
  OSM-database          The definition of the database to write the data into;
                        should be a string of the form
                        <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>
  OSM-file              The OSM-file to read

options:
  -h, --help            show this help message and exit
  -c FILE, --config FILE
                        Reads the named configuration file
  --version             show program's version number and exit
  -R, --dropprevious    Delete destination tables if already existing
  -A, --append          Append read data to existing tables
  -v, --verbose         Print what is being done

(c) Copyright 2016-2025, German Aerospace Center (DLR)
"""



