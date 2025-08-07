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
sys.path.append(os.path.join(os.path.split(__file__)[0], "..", "visualisation"))
import plot_area


# --- test functions ----------------------------------------------------------
def test_main_empty(capsys):
    """Test behaviour if no arguments are given"""
    assert plot_area.main([])==2
    captured = capsys.readouterr()
    assert captured.out == ""
    assert captured.err.replace("__main__.py", "plot_area.py") == """usage: plot_area [-h] [-c FILE] [--version] [-f OBJECTS] [--from.id OBJECTSID]
                 [--from.geom OBJECTSGEOM] [--from.filter OBJECTSFILTER]
                 [-m MEASURES] [-i MEASURESVALUE] [-N NORM] [-p PROJECTION]
                 [-b MAINBORDER] [--border.geom MAINBORDER_GEOM]
                 [--inner INNERBORDERS] [--bounds BOUNDS] [-n NET]
                 [--water WATER] [--minV MINV] [--maxV MAXV] [--levels LEVELS]
                 [--measure-label MEASURELABEL] [-F FIGSIZE] [-C COLMAP]
                 [-I INVALIDCOLOR] [-L] [--contour] [--isochrone]
                 [--no-legend] [-t TITLE]
                 [--from.borderwidth FROM_BORDERWIDTH] [--net.width NET_WIDTH]
                 [-o OUTPUT] [-v] [--report-all-missing-values] [-S]
plot_area: error: You have to define the objects to load using '--from / -f
plot_area: error: You have to define the measures to load using '--measures / -m
plot_area: quitting on error.
"""


def test_main_help(capsys):
    """Test behaviour if no arguments are given"""
    try:
        plot_area.main(["--help"])
        assert False # pragma: no cover
    except SystemExit as e:
        assert type(e)==type(SystemExit())
        assert e.code==0
    captured = capsys.readouterr()
    assert captured.err == ""
    assert captured.out.replace("__main__.py", "plot_area.py") == """usage: plot_area [-h] [-c FILE] [--version] [-f OBJECTS] [--from.id OBJECTSID]
                 [--from.geom OBJECTSGEOM] [--from.filter OBJECTSFILTER]
                 [-m MEASURES] [-i MEASURESVALUE] [-N NORM] [-p PROJECTION]
                 [-b MAINBORDER] [--border.geom MAINBORDER_GEOM]
                 [--inner INNERBORDERS] [--bounds BOUNDS] [-n NET]
                 [--water WATER] [--minV MINV] [--maxV MAXV] [--levels LEVELS]
                 [--measure-label MEASURELABEL] [-F FIGSIZE] [-C COLMAP]
                 [-I INVALIDCOLOR] [-L] [--contour] [--isochrone]
                 [--no-legend] [-t TITLE]
                 [--from.borderwidth FROM_BORDERWIDTH] [--net.width NET_WIDTH]
                 [-o OUTPUT] [-v] [--report-all-missing-values] [-S]

Plots accessibility measures on a map

options:
  -h, --help            show this help message and exit
  -c FILE, --config FILE
                        Reads the named configuration file
  --version             show program's version number and exit
  -f OBJECTS, --from OBJECTS
                        Defines the objects (origins) to load
  --from.id OBJECTSID   Defines the name of the field to read the object ids
                        from
  --from.geom OBJECTSGEOM
                        Defines the name of the field to read the object
                        geometries from
  --from.filter OBJECTSFILTER
                        Defines a SQL WHERE-clause parameter to filter the
                        origins to read
  -m MEASURES, --measures MEASURES
                        Defines the measures' table to load
  -i MEASURESVALUE, --value MEASURESVALUE
                        Defines the name of the value to load from the
                        measures
  -N NORM, --norm NORM  Defines the norm factor (divider)
  -p PROJECTION, --projection PROJECTION
                        Sets the projection EPSG number
  -b MAINBORDER, --border MAINBORDER
                        Defines the border geometry to load
  --border.geom MAINBORDER_GEOM
                        Defines the column name of the border's geometry
  --inner INNERBORDERS  Defines the optional inner boundaries to load
  --bounds BOUNDS       Defines the bounding box to show
  -n NET, --net NET     Defines the optional road network source
  --water WATER         Defines the optional water source
  --minV MINV           Sets the lower value bound
  --maxV MAXV           Sets the upper value bound
  --levels LEVELS       Sets the discrete levels
  --measure-label MEASURELABEL
                        Sets the colorbar measure label
  -F FIGSIZE, --figsize FIGSIZE
                        Defines figure size
  -C COLMAP, --colormap COLMAP
                        Defines the color map to use
  -I INVALIDCOLOR, --invalid INVALIDCOLOR
                        Defines the color to use when data is missing
  -L, --logarithmic     Whether logarithmic scaling shall be used
  --contour             Triggers contour rendering
  --isochrone           Triggers isochrone rendering
  --no-legend           If set, no legend will be drawn
  -t TITLE, --title TITLE
                        Sets the figure title
  --from.borderwidth FROM_BORDERWIDTH
                        Sets the width of the border of the loaded objects
  --net.width NET_WIDTH
                        Sets the width scale of the network
  -o OUTPUT, --output OUTPUT
                        Defines the name of the graphic to generate
  -v, --verbose         Triggers verbose output
  --report-all-missing-values
                        Triggers reporting all missing values
  -S, --no-show         Does not show the figure if set

(c) Copyright 2023-2025, German Aerospace Center (DLR)
"""



