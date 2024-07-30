#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Plots measures on a map."""
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
from optparse import OptionParser
import matplotlib
import matplotlib.pyplot
import matplotlib.patches
import matplotlib.collections
import numpy
import scipy
import scipy.interpolate
import sys, os
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



def plot_area_objects(fig, ax, obj2pos, obj2val, colmap, sm, from_borderwidth=1., report_all_missing_values=False, invalidColor="azure", preset_colors=None):
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



def parse_options(args):
    optParser = OptionParser(usage="""usage: %prog [options].""")
    optParser.add_option("-f", "--from", dest="objects",default=None, help="Defines the objects (origins) to load")
    optParser.add_option("--from.id", dest="objectsID", default="id", help="Defines the name of the field to read the object ids from")
    optParser.add_option("--from.geom", dest="objectsGeom",default="polygon", help="Defines the name of the field to read the object geometries from")
    optParser.add_option("--from.filter", dest="objectsFilter",default=None, help="Defines a SQL WHERE-clause parameter to filter the origins to read")
    optParser.add_option("-m", "--measures", dest="measures", default=None, help="Defines the measures' table to load")
    optParser.add_option("-i", "--value", dest="measuresValue", default='avg_tt', help="Defines the name of the value to load from the measures")
    optParser.add_option("-p", "--projection", dest="projection", type=int, default=25833, help="Sets the projection EPSG number")
    #
    optParser.add_option("-b", "--border", dest="mainBorder", default=None, help="Defines the border geometry to load")
    optParser.add_option("--border.geom", dest="mainBorder_geom", default="polygon", help="Defines the column name of the border's geometry")
    optParser.add_option("--inner", dest="innerBorders", default=None, help="Defines the optional inner boundaries to load")
    optParser.add_option("--bounds", dest="bounds", default=None, help="Defines the bounding box to show")
    #
    optParser.add_option("-n", "--net", dest="net", default=None, help="Defines the optional road network source")
    optParser.add_option("--water", dest="water", default=None, help="Defines the optional water source")
    #
    optParser.add_option("--minV", dest="minV", type=float, default=None, help="Sets the lower value bound")
    optParser.add_option("--maxV", dest="maxV", type=float, default=None, help="Sets the upper value bound")
    optParser.add_option("--levels", dest="levels", default=None, help="Sets the discrete levels")
    optParser.add_option("--measure-label", dest="measurelabel", default="", help="Sets the colorbar measure label")
    #
    optParser.add_option("-F", "--figsize", dest="figsize", default="8,5", help="Defines figure size")
    optParser.add_option("-C", "--colormap", dest="colmap", default="RdYlGn_r", help="Defines the color map to use")
    optParser.add_option("-I", "--invalid", dest="invalidColor", default="azure", help="Defines the color to use when data is missing")
    optParser.add_option("-L", "--logarithmic", dest="logarithmic", action="store_true", default=False, help="Whether logarithmic scaling shall be used")
    optParser.add_option("--contour", dest="contour", action="store_true", default=False, help="Triggers contour rendering")
    optParser.add_option("--isochrone", dest="isochrone", action="store_true", default=False, help="Triggers isochrone rendering")
    optParser.add_option("--no-legend", dest="no_legend", default=False, action="store_true", help="If set, no legend will be drawn")
    optParser.add_option("-t", "--title", dest="title", default=None, help="Sets the figure title")
    optParser.add_option("--from.borderwidth", dest="from_borderwidth", type=float, default=1., help="Sets the width of the border of the loaded objects")
    optParser.add_option("--net.width", dest="net_width", type=float, default=1., help="Sets the width scale of the network")
    #
    optParser.add_option("-o", "--output", dest="output", default=None, help="Defines the name of the graphic to generate")
    optParser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False, help="Triggers verbose output")
    optParser.add_option("--report-all-missing-values", dest="report_all_missing_values", action="store_true", default=False, help="Triggers reporting all missing values")
    optParser.add_option("-S", "--no-show", dest="no_show", action="store_true", default=False, help="Does not show the figure if set")
  
    options, remaining_args = optParser.parse_args(args)
    if options.objects==None:
        print ("You have to define the objects to load using '--from / -f")
    if options.measures==None:
        print ("You have to define the measures to load using '--measures / -m")
    if options.objects==None or options.measures==None:
        exit()
    return options, remaining_args


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


def main(argv):
    options, remaining_args = parse_options(argv)
    # -- load objects
    # load the main border optionally
    mainBorder = None
    if options.mainBorder!=None:
        if options.verbose: print ("Loading main border...")
        shapes = load_shapes(options.mainBorder, options.projection, False, geomField=options.mainBorder_geom)
        mainBorder = shapes[list(shapes.keys())[0]] # use only the first geometry
    # load the inner borders optionally
    innerBorders = None
    if options.innerBorders is not None:
        if options.verbose: print ("Loading inner borders...")
        innerBorders = load_shapes(options.innerBorders, options.projection, False)
    # load the network optionally
    network = None
    if options.net is not None:
        if options.verbose: print ("Loading the road network...")
        network = net.loadNet("postgresql:"+options.net, options.projection, ["street_type"])
    # load the water optionally
    water = None
    if options.water is not None:
        if options.verbose: print ("Loading the water layer...")
        water = load_shapes(options.water, options.projection, False)
    # load shapes and measures
    if options.verbose: print ("Loading origin geometries...")
    obj2geom = load_shapes(options.objects, options.projection, options.contour, options.objectsID, options.objectsGeom, options.objectsFilter)
    if options.verbose: print ("Loading measures...")
    obj2value = load_measures(options.measures, options.measuresValue, options.minV, options.maxV, options.isochrone)
    # -- draw
    plot(mainBorder, innerBorders, network, water, obj2geom, obj2value, options, remaining_args)
    # show / save
    if options.output is not None:
        matplotlib.pyplot.savefig(options.output)
    if not options.no_show:
        matplotlib.pyplot.show()


# -- main check
if __name__ == "__main__":
    main(sys.argv)
  
  