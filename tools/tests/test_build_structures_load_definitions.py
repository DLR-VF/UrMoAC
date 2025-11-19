#!/usr/bin/env python
# -*- coding: utf-8 -*-
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
import tempfile
import shutil
sys.path.append(os.path.join(os.path.split(__file__)[0], "..", "osm"))
import osmdb_buildStructures


# --- test functions ----------------------------------------------------------
class TestUrMoAC_osmdb_buildStructures_LoadDefinition(unittest.TestCase):
    """Testing the _mark_html method"""

    def setUp(self):
        self._extractor = osmdb_buildStructures.OSMExtractor()
        self._testdir = tempfile.mkdtemp()


    def tearDown(self):
        # Remove the directory after the test
        shutil.rmtree(self._testdir)
        
        
        
    def test_read_single_definition_node(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[node]
building=*    
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == ["building=*"]
        assert self._extractor._defs["way"] == []
        assert self._extractor._defs["rel"] == []
        self._extractor._roles == set()


    def test_read_single_definition_way(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[way]
building=*    
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == []
        assert self._extractor._defs["way"] == ["building=*"]
        assert self._extractor._defs["rel"] == []
        self._extractor._roles == set()


    def test_read_single_definition_rel(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[rel]
building=*    
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == []
        assert self._extractor._defs["way"] == []
        assert self._extractor._defs["rel"] == ["building=*"]
        self._extractor._roles == set()


    def test_read_single_definition_all(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[*]
building=*    
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == ["building=*"]
        assert self._extractor._defs["way"] == ["building=*"]
        assert self._extractor._defs["rel"] == ["building=*"]
        self._extractor._roles == set()

        
        
    def test_read_multiple_definition_node(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[node]
building=*
amenity=school
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == ["building=*", "amenity=school"]
        assert self._extractor._defs["way"] == []
        assert self._extractor._defs["rel"] == []
        self._extractor._roles == set()


    def test_read_multiple_definition_way(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[way]
building=*
amenity=school
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == []
        assert self._extractor._defs["way"] == ["building=*", "amenity=school"]
        assert self._extractor._defs["rel"] == []
        self._extractor._roles == set()


    def test_read_multiple_definition_rel(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[rel]
building=*
amenity=school
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == []
        assert self._extractor._defs["way"] == []
        assert self._extractor._defs["rel"] == ["building=*", "amenity=school"]
        self._extractor._roles == set()


    def test_read_multiple_definition_all(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[*]
building=*
amenity=school
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == ["building=*", "amenity=school"]
        assert self._extractor._defs["way"] == ["building=*", "amenity=school"]
        assert self._extractor._defs["rel"] == ["building=*", "amenity=school"]
        self._extractor._roles == set()




    def test_read_multiple_definition_all_roles(self):
        """ """
        filename = os.path.join(self._testdir, "test1.txt")
        with open(filename, "w") as fdo:
            fdo.write("""[*]
<outline>
<outer>
<inner>
building=*
amenity=school
""")
        self._extractor.load_definitions(filename)
        assert self._extractor._defs["node"] == ["building=*", "amenity=school"]
        assert self._extractor._defs["way"] == ["building=*", "amenity=school"]
        assert self._extractor._defs["rel"] == ["building=*", "amenity=school"]
        assert self._extractor._roles == set(["outline", "outer", "inner"])


    
