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
sys.path.append(os.path.join(os.path.split(__file__)[0], "..", "gtfs"))
import importGTFS


# --- test functions ----------------------------------------------------------
def test_main_empty(capsys):
    """Test behaviour if no arguments are given"""
    try:
        importGTFS.main([])
        assert False # pragma: no cover
    except SystemExit as e:
        assert type(e)==type(SystemExit())
        assert e.code==2
    captured = capsys.readouterr()
    assert captured.out == ""
    assert captured.err.replace("__main__.py", "importGTFS.py") == """usage: importGTFS [-h] [-c FILE] [--version] [-R] [-A] [-C] [-I] [-v]
                  GTFS-database GTFS-folder
importGTFS: error: the following arguments are required: GTFS-database, GTFS-folder
"""


def test_main_help(capsys):
    """Test behaviour if no arguments are given"""
    try:
        importGTFS.main(["--help"])
        assert False # pragma: no cover
    except SystemExit as e:
        assert type(e)==type(SystemExit())
        assert e.code==0
    captured = capsys.readouterr()
    assert captured.err == ""
    assert captured.out.replace("__main__.py", "importGTFS.py") == """usage: importGTFS [-h] [-c FILE] [--version] [-R] [-A] [-C] [-I] [-v]
                  GTFS-database GTFS-folder

Imports a GTFS file set into a PostGIS database

positional arguments:
  GTFS-database         The definition of the database to write the data into;
                        should be a string of the form
                        <HOST>,<DB>,<SCHEMA>.<TABLE_PREFIX>,<USER>,<PASSWD>
  GTFS-folder           The folder the GTFS files are located in

options:
  -h, --help            show this help message and exit
  -c FILE, --config FILE
                        Reads the named configuration file
  --version             show program's version number and exit
  -R, --dropprevious    Delete destination tables if already existing
  -A, --append          Append read data to existing tables
  -C, --keep-open       If not set, line information is not added to stops
  -I, --add-ids         Whan set, adds integer ids to stops
  -v, --verbose         Print what is being done

(c) Copyright 2016-2025, German Aerospace Center (DLR)
"""



