#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Plots measures on a map."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2025, Institute of Transport Research, German Aerospace Center (DLR)"
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
import matplotlib
import matplotlib.pyplot
import matplotlib.patches
import matplotlib.collections
import numpy
import scipy
import scipy.interpolate
import sys
import os
import argparse
import psycopg2
import datetime

script_dir = os.path.dirname( __file__ )
mymodule_dir = os.path.join( script_dir, '..', 'helper' )
sys.path.append( mymodule_dir )

import wkt
import net
import spatialhelper


# --- function definitions --------------------------------------------------
def cmap_discretize(cmap, N):
    """Return a discrete colormap from the continuous colormap cmap.

        cmap: colormap instance, eg. cm.jet. 
        N: number of colors.

    Example
        x = resize(arange(100), (5,100))
        djet = cmap_discretize(cm.jet, 5)
        imshow(x, cmap=djet)
    """
    if type(cmap) == str:
        cmap = plt.get_cmap(cmap)
    colors_i = numpy.concatenate((numpy.linspace(0, 1., N), (0.,0.,0.,0.)))
    colors_rgba = cmap(colors_i)
    indices = numpy.linspace(0, 1., N+1)
    cdict = {}
    for ki,key in enumerate(('red','green','blue')):
        cdict[key] = [ (indices[i], colors_rgba[i-1,ki], colors_rgba[i,ki]) for i in range(0, N+1) ]
    # Return colormap object.
    return matplotlib.colors.LinearSegmentedColormap(cmap.name + "_%d"%N, cdict, 1024)
    
        
def colorbar_index(ax, ncolors, cmap, ticklabels):
    cmap = cmap_discretize(cmap, ncolors)
    mappable = matplotlib.cm.ScalarMappable(cmap=cmap)
    mappable.set_array([])
    mappable.set_clim(-0.5, ncolors+0.5)
    colorbar = matplotlib.pyplot.colorbar(mappable, ax=ax)
    colorbar.set_ticks(numpy.linspace(0, ncolors, ncolors))
    colorbar.set_ticklabels(ticklabels)
    return colorbar    
  

def plot_area_contours(fig, ax, obj2pos, obj2val, colmap, norm, levels, report_all_missing_values=False, invalidColor="azure"):
    # compute points
    xs = []
    ys = []
    zs = []
    had_missing = False
    for g in obj2pos:
        if g not in obj2val:
            if report_all_missing_values or not had_missing:
                print ("Missing values for object %s" % g)
                had_missing = True
                if not report_all_missing_values:
                    print (" Subsequent missing values will not be reported.\n Use --report-all-missing-values to list all.")
            continue
        xs.append(obj2pos[g]._shape[0])
        ys.append(obj2pos[g]._shape[1])
        zs.append(obj2val[g])
    xi = numpy.linspace(min(xs), max(xs), 1000) # !!!
    yi = numpy.linspace(min(ys), max(ys), 1000) # !!!
    xi, yi = numpy.meshgrid(xi, yi)
    zi = scipy.interpolate.griddata((xs, ys), zs, (xi, yi), method='linear')
    #
    cs1 = matplotlib.pyplot.contourf(xi, yi, zi, levels=levels, cmap=colmap, zorder=20, norm=norm)#, alpha=.5)



def plot_area_objects(fig, ax, obj2pos, obj2val, colmap, sm, from_borderwidth=.5, report_all_missing_values=False, invalidColor="azure", preset_colors=None):
    patches = []
    had_missing = False
    for o in obj2pos:
        c = invalidColor
        if preset_colors is not None and o in preset_colors:
            c = preset_colors[o]
        elif o not in obj2val:
            if report_all_missing_values or not had_missing:
                print ("Missing values for object %s" % o)
                had_missing = True
                if not report_all_missing_values:
                    print (" Subsequent missing values will not be reported.\n Use --report-all-missing-values to list all.")
        else:
            c = sm.to_rgba(obj2val[o])
        p = obj2pos[o].artist(fc=c, ec="black", lw=from_borderwidth, zorder=800)
        if p is None:
            print ("Missing geometry for object %s" % o)
            continue
        ax.add_artist(p)


def add_colorbar(fig, ax, colmap, sm, levels, logarithmic, measurelabel):
    # fake up the array of the scalar mappable. Urgh..." (pelson, http://stackoverflow.com/questions/8342549/matplotlib-add-colorbar-to-a-sequence-of-line-plots)
    sm._A = []
    labels = []
    if levels is not None and type(levels) is not int:
        for il,l in enumerate(levels):
            if il==0: continue 
            if il==len(levels)-1: 
                l = ">= " + str(levels[il-1])
                if measurelabel!="":
                    l = l + " " + measurelabel
            else: 
                if measurelabel!="":
                    l = str(levels[il-1]) + " " + measurelabel + " - " + str(levels[il]) + " " + measurelabel
                else:
                    l = str(levels[il-1]) + " - " + str(levels[il])
            labels.append(l)
        cbar = colorbar_index(ax, len(levels[1:]), colmap, labels)
    else:
        if logarithmic:
            from matplotlib.ticker import LogLocator
            cbar = matplotlib.pyplot.colorbar(sm, ticks=LogLocator(numticks=10), ax=ax)
        else:
            from matplotlib.ticker import LinearLocator
            cbar = matplotlib.pyplot.colorbar(sm,  ax=ax)
        # https://stackoverflow.com/questions/29053132/manipulate-tick-labels-on-a-colorbar
        labels = cbar.ax.get_yticklabels()
        if measurelabel!="":
            labels = map(lambda x: x.get_text() + " " + measurelabel, labels)
        cbar.ax.set_yticklabels(labels)
    cbar.ax.tick_params(labelsize=12)



def parse_arguments(arguments):
    """parse arguments"""
    # parse options
    if arguments is None:
        arguments = sys.argv[1:]
    # https://stackoverflow.com/questions/3609852/which-is-the-best-way-to-allow-configuration-options-be-overridden-at-the-comman
    defaults = {}
    conf_parser = argparse.ArgumentParser(prog='plot_area', add_help=False)
    conf_parser.add_argument("-c", "--config", metavar="FILE", help="Reads the named configuration file")
    args, remaining_args = conf_parser.parse_known_args(arguments)
    if args.config is not None:
        if not os.path.exists(args.config):
            print ("plot_area: error: configuration file '%s' does not exist" % str(args.config), file=sys.stderr)
            raise SystemExit(2)
        config = configparser.ConfigParser()
        config.read([args.config])
        defaults.update(dict(config.items("DEFAULT")))
    parser = argparse.ArgumentParser(prog='plot_area', parents=[conf_parser], 
        description='Plots accessibility measures on a map', 
        epilog='(c) Copyright 2023-2025, German Aerospace Center (DLR)')
    parser.add_argument('--version', action='version', version='%(prog)s 0.8.2')
    parser.add_argument("-f", "--from", dest="objects", default=None, help="Defines the objects (origins) to load")
    parser.add_argument("--from.id", dest="objectsID", default="id", help="Defines the name of the field to read the object ids from")
    parser.add_argument("--from.geom", dest="objectsGeom", default="geom", help="Defines the name of the field to read the object geometries from")
    parser.add_argument("--from.filter", dest="objectsFilter",default=None, help="Defines a SQL WHERE-clause parameter to filter the origins to read")
    parser.add_argument("-m", "--measures", dest="measures", default=None, help="Defines the measures' table to load")
    parser.add_argument("-i", "--value", dest="measuresValue", default='avg_tt', help="Defines the name of the value to load from the measures")
    parser.add_argument("-N", "--norm", dest="norm", type=float, default=1, help="Defines the norm factor (divider)")
    parser.add_argument("-p", "--projection", dest="projection", type=int, default=25833, help="Sets the projection EPSG number")
    #
    parser.add_argument("-b", "--border", dest="mainBorder", default=None, help="Defines the border geometry to load")
    parser.add_argument("--border.geom", dest="mainBorder_geom", default="geom", help="Defines the column name of the border's geometry")
    parser.add_argument("--inner", dest="innerBorders", default=None, help="Defines the optional inner boundaries to load")
    parser.add_argument("--bounds", dest="bounds", default=None, help="Defines the bounding box to show")
    #
    parser.add_argument("-n", "--net", dest="net", default=None, help="Defines the optional road network source")
    parser.add_argument("--water", dest="water", default=None, help="Defines the optional water source")
    #
    parser.add_argument("--minV", dest="minV", type=float, default=None, help="Sets the lower value bound")
    parser.add_argument("--maxV", dest="maxV", type=float, default=None, help="Sets the upper value bound")
    parser.add_argument("--levels", dest="levels", default=None, help="Sets the discrete levels")
    parser.add_argument("--measure-label", dest="measurelabel", default="", help="Sets the colorbar measure label")
    #
    parser.add_argument("-F", "--figsize", dest="figsize", default="8,5", help="Defines figure size")
    parser.add_argument("-C", "--colormap", dest="colmap", default="RdYlGn_r", help="Defines the color map to use")
    parser.add_argument("-I", "--invalid", dest="invalidColor", default="azure", help="Defines the color to use when data is missing")
    parser.add_argument("-L", "--logarithmic", dest="logarithmic", action="store_true", default=False, help="Whether logarithmic scaling shall be used")
    parser.add_argument("--contour", dest="contour", action="store_true", default=False, help="Triggers contour rendering")
    parser.add_argument("--isochrone", dest="isochrone", action="store_true", default=False, help="Triggers isochrone rendering")
    parser.add_argument("--no-legend", dest="no_legend", default=False, action="store_true", help="If set, no legend will be drawn")
    parser.add_argument("-t", "--title", dest="title", default=None, help="Sets the figure title")
    parser.add_argument("--from.borderwidth", dest="from_borderwidth", type=float, default=1., help="Sets the width of the border of the loaded objects")
    parser.add_argument("--net.width", dest="net_width", type=float, default=1., help="Sets the width scale of the network")
    #
    parser.add_argument("-o", "--output", dest="output", default=None, help="Defines the name of the graphic to generate")
    parser.add_argument("-v", "--verbose", dest="verbose", action="store_true", default=False, help="Triggers verbose output")
    parser.add_argument("--report-all-missing-values", dest="report_all_missing_values", action="store_true", default=False, help="Triggers reporting all missing values")
    parser.add_argument("-S", "--no-show", dest="no_show", action="store_true", default=False, help="Does not show the figure if set")
    parser.set_defaults(**defaults)
    args, remaining_args = parser.parse_known_args(remaining_args)
    # check and parse command line parameter and input files
    errors = []
    # - output db
    if args.objects==None:
        errors.append("You have to define the objects to load using '--from / -f")
    if args.measures==None:
        errors.append("You have to define the measures to load using '--measures / -m")
    # - report
    if len(errors)!=0:
        parser.print_usage(sys.stderr)
        for e in errors:
            print ("plot_area: error: %s" % e, file=sys.stderr)
        print ("plot_area: quitting on error.", file=sys.stderr)
        return None, None
    #
    return args, remaining_args


def load_shapes(source, projection, asCentroid=False, idField="id", geomField="polygon", geomFilter=None):
    (host, db, tableFull, user, password) = source.split(",")
    (schema, table) = tableFull.split(".")
    where = ""
    if geomFilter is not None:
        where = " WHERE %s" % geomFilter
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    if asCentroid: 
        cursor.execute("SELECT %s,ST_AsText(ST_Centroid(ST_Transform(%s, %s))) FROM %s.%s%s" % (idField, geomField, projection, schema, table, where))
    else: 
        cursor.execute("SELECT %s,ST_AsText(ST_Transform(%s, %s)) FROM %s.%s%s" % (idField, geomField, projection, schema, table, where))
    obj2geom = {}
    for r in cursor.fetchall():
        geom = wkt.wkt2geometry(r[1])
        if geom is None or geom._shape is None or len(geom._shape)==0:
            # skipping empty geometries
            print ("The geometry of %s is empty. Skipping" % int(r[0]))
            continue
        obj2geom[int(r[0])] = geom
    return obj2geom



def load_measures(source, measure, minV, maxV, isochrone):
    (host, db, tableFull, user, password) = source.split(",")
    (schema, table) = tableFull.split(".")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    id_field = "fid" if not isochrone else "sid"
    cursor.execute("SELECT %s,%s FROM %s.%s" % (id_field, measure, schema, table))
    obj2value = {}
    for r in cursor.fetchall():
        obj2value[int(r[0])] = float(r[1])
    if minV is not None: 
        for o in obj2value: obj2value[o] = max(minV, obj2value[o])
    if maxV is not None:
        for o in obj2value: obj2value[o] = min(maxV, obj2value[o])
    return obj2value


def plot(mainBorder, innerBorders, network, water, obj2geom, obj2value, options, remaining_args, preset_colors=None):
    # -- draw
    # open figure
    figsize = options.figsize if options.figsize is not None else "8,5"
    figsize = figsize.split(",")
    fig = matplotlib.pyplot.figure(figsize=(float(figsize[0]), float(figsize[1])))
    ax = fig.add_subplot(111)
    # parse and set colors
    colormap = matplotlib.pyplot.get_cmap(options.colmap)
    f = list(obj2value.keys())[0]
    maxV = obj2value[f]
    minV = obj2value[f]
    for o in obj2value:
        maxV = max(obj2value[o], maxV)
        minV = min(obj2value[o], minV)
    if options.maxV is not None: maxV = options.maxV
    if options.minV is not None: minV = options.minV
    if options.levels is not None and options.levels is not int: options.levels = [float(i) for i in options.levels.split(",")]
    norm = matplotlib.pyplot.Normalize(vmin=minV, vmax=maxV)
    sm = matplotlib.cm.ScalarMappable(cmap=options.colmap, norm=norm)
    # build clip
    clip = mainBorder.artist(lw=2, fc="red", ec="black", transform=ax.transData) if mainBorder is not None else None
    # draw
    if options.contour:
        plot_area_contours(fig, ax, obj2geom, obj2value, colormap, norm, options.levels, options.report_all_missing_values, options.invalidColor)
    else:
        plot_area_objects(fig, ax, obj2geom, obj2value, colormap, sm, options.from_borderwidth, options.report_all_missing_values, options.invalidColor, preset_colors)
    # add a colorbar
    if not options.no_legend:
        add_colorbar(fig, ax, colormap, sm, options.levels, options.logarithmic, options.measurelabel)
    if water is not None:
        for w in water:
            p = water[w].artist(fc="#3cacd5", lw=0, zorder=300)
            ax.add_artist(p)
    if innerBorders is not None:
        patches = [g.artist() for g in innerBorders]
        ax.add_collection(matplotlib.collections.PatchCollection(patches, facecolors='none', linewidths=10., edgecolor="black", zorder=40  ))
    if network is not None:
        net.plotNet(ax, network, color="#000000", alpha=1, width_scale=options.net_width, zorder=10)
    # apply clipping
    for o in ax.get_children():
        o.set_clip_path(clip)
    # add bounds after clipping
    if mainBorder is not None:
        ax.add_patch(mainBorder.artist(lw=2, fc="none", ec="black", zorder=1000))
        ax.add_patch(mainBorder.artist(lw=0, fc=options.invalidColor, ec="black", zorder=-1))
    # decorate
    if options.title is not None: 
        matplotlib.pyplot.title(options.title.replace("\\n", "\n"), size=16)
    
    # -- get/set bounds
    # compute the boundary to show if not given
    bounds = None
    if options.bounds:
        bounds = [float(v) for v in options.bounds.split(",")]
        if len(bounds)!=4:
            print ("The bounds must be a tuple of minx,miny,maxx,maxy.")
            exit()
    else:
        if mainBorder is not None:
            bounds = mainBorder.bounds()
        else:
            bounds = spatialhelper.geometries_bounds(list(obj2geom.values()))
    matplotlib.pyplot.xlim(bounds[0], bounds[2])
    matplotlib.pyplot.ylim(bounds[1], bounds[3])
    
    # -- set scaling and axes
    ax.grid(False)
    ax.set_xticklabels([])
    ax.set_yticklabels([])
    matplotlib.pyplot.tick_params(axis='x', which='both', bottom=False, top=False, labelbottom=False)
    matplotlib.pyplot.tick_params(axis='y', which='both', left=False, right=False, labelleft=False)
    matplotlib.pyplot.gca().xaxis.set_major_locator(matplotlib.pyplot.NullLocator())
    matplotlib.pyplot.gca().yaxis.set_major_locator(matplotlib.pyplot.NullLocator())
    ax.set_aspect('equal', 'datalim')
    matplotlib.pyplot.subplots_adjust(bottom=0.05, right=.96, left=.02, top=0.9, wspace=0)


def main(arguments=None):
    args, remaining_args = parse_arguments(arguments)
    if args is None:
        return 2
    # -- load objects
    # load the main border optionally
    mainBorder = None
    if args.mainBorder!=None:
        if args.verbose: print ("Loading main border...")
        shapes = load_shapes(args.mainBorder, args.projection, False, geomField=args.mainBorder_geom, needs_id=False)
        mainBorder = shapes[list(shapes.keys())[0]] # use only the first geometry
    # load the inner borders optionally
    innerBorders = None
    if args.innerBorders is not None:
        if args.verbose: print ("Loading inner borders...")
        innerBorders = load_shapes(args.innerBorders, args.projection, False)
    # load the network optionally
    network = None
    if args.net is not None:
        if args.verbose: print ("Loading the road network...")
        network = net.loadNet("postgresql:"+args.net, args.projection, ["street_type"])
    # load the water optionally
    water = None
    if args.water is not None:
        if args.verbose: print ("Loading the water layer...")
        water = load_shapes(args.water, args.projection, False)
    # load shapes and measures
    if args.verbose: print ("Loading origin geometries...")
    obj2geom = load_shapes(args.objects, args.projection, args.contour, args.objectsID, args.objectsGeom, args.objectsFilter)
    if args.verbose: print ("Loading measures...")
    obj2value = load_measures(args.measures, args.measuresValue, args.norm, args.minV, args.maxV, args.isochrone)
    # -- draw
    plot(mainBorder, innerBorders, network, water, obj2geom, obj2value, args, remaining_args)
    # show / save
    if args.output is not None:
        matplotlib.pyplot.savefig(args.output)
    if not args.no_show:
        matplotlib.pyplot.show()
    return 0


# -- main check
if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
  
  