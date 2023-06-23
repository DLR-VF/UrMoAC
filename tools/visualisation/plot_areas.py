#!/usr/bin/env python
# =========================================================
# plot_area.py
#
# @author Daniel Krajzewicz
# @date 01.05.2023
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Plots measures as an areal contour plot
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2016-2023 DLR Institute of Transport Research
# All rights reserved.
# =========================================================


# --- imported modules ------------------------------------
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



# --- helper functions ------------------------------------
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
    
        
def colorbar_index(ncolors, cmap, ticklabels):
    cmap = cmap_discretize(cmap, ncolors)
    mappable = matplotlib.cm.ScalarMappable(cmap=cmap)
    mappable.set_array([])
    mappable.set_clim(-0.5, ncolors+0.5)
    colorbar = matplotlib.pyplot.colorbar(mappable)
    colorbar.set_ticks(numpy.linspace(0, ncolors, ncolors))
    colorbar.set_ticklabels(ticklabels)
    return colorbar    
  
  

def plot_area_contours(fig, ax, obj2pos, obj2val, colmap, invalidColor="azure"):
    # compute points
    xs = []
    ys = []
    zs = []
    for g in obj2pos:
        if g not in obj2val:
            continue
        xs.append(obj2pos[g]._shape[0])
        ys.append(obj2pos[g]._shape[1])
        zs.append(min(obj2val[g], 900)) # !!!
    xi = numpy.linspace(min(xs), max(xs), 1000) # !!!
    yi = numpy.linspace(min(ys), max(ys), 1000) # !!!
    xi, yi = numpy.meshgrid(xi, yi)
    zi = scipy.interpolate.griddata((xs, ys), zs, (xi, yi), method='linear')
    #
    levels = [0, 150, 300, 450, 600, 750, 900, 1200]
    valueMeasure = "s"
    cs1 = matplotlib.pyplot.contourf(xi, yi, zi, levels=levels, cmap=colmap, zorder=20)
    #cs1 = matplotlib.pyplot.contourf(xi, yi, zi, levels=100, cmap=colmap, zorder=20)
    if clip is not None:
        for col in cs1.collections:
            col.set_clip_path(clip)
    sm = matplotlib.cm.ScalarMappable(cmap=colmap, norm=matplotlib.pyplot.Normalize(vmin=0, vmax=900))
    # fake up the array of the scalar mappable. Urgh..." (pelson, http://stackoverflow.com/questions/8342549/matplotlib-add-colorbar-to-a-sequence-of-line-plots)
    sm._A = []
    labels = []
    for il,l in enumerate(levels):
        if il==0: continue 
        if il==len(levels)-1: l = ">= " + str(levels[il-1]) + " " + valueMeasure
        else: l = str(levels[il-1]) + " " + valueMeasure + " - " + str(levels[il]) + " " + valueMeasure
        labels.append(l)
    if True:
        cbar = colorbar_index(len(levels[1:]), colmap, labels)#["300s","600s","900s","1200s","1500s","1800s"])
        cbar.ax.tick_params(labelsize=12)   
    #return fig, ax



def plot_area_objects(fig, ax, obj2pos, obj2val, colmap, invalidColor="azure", from_borderwidth=1.):
    #levels = [0, 150, 300, 450, 500, 650, 900]
    valueMeasure = "s"
    sm = matplotlib.cm.ScalarMappable(cmap=colmap, norm=matplotlib.pyplot.Normalize(vmin=0, vmax=900))
    patches = []
    for o in obj2pos:
        if o not in obj2val:
            print ("Missing values for object %s" % o)
            p = obj2pos[o].artist(fc=invalidColor, ec="black", lw=from_borderwidth, zorder=800)
        else:
            p = obj2pos[o].artist(fc=sm.to_rgba(obj2val[o]), ec="black", lw=from_borderwidth, zorder=800)
        if p is None:
            print ("Missing geometry for object %s" % o)
            continue
        """
        if clip is not None:
            print("Here")
            p.set_clip_path(clip)
        """
        ax.add_artist(p)
    # fake up the array of the scalar mappable. Urgh..." (pelson, http://stackoverflow.com/questions/8342549/matplotlib-add-colorbar-to-a-sequence-of-line-plots)
    sm._A = []
    labels = []
    if True:
        cbar = matplotlib.pyplot.colorbar(sm, ticks=range(0, 1000, 100))
        cbar.ax.tick_params(labelsize=12)   
        cbar.ax.set_yticklabels(["0 s", "100 s", "200 s", "300 s", "400 s", "500 s", "600 s", "700 s", "800 s", ">= 900 s"])



def parse_options():
    optParser = OptionParser(usage="""usage: %prog [options].""")
    optParser.add_option("-f", "--from", dest="objects",default=None, help="Defines the objects (origins) to load")
    optParser.add_option("--from.id", dest="objectsID", default="gid", help="Defines the name of the field to read the object ids from")
    optParser.add_option("--from.geom", dest="objectsGeom",default="polygon", help="Defines the name of the field to read the object geometries from")
    optParser.add_option("--from.filter", dest="objectsFilter",default=None, help="Defines a SQL WHERE-clause parameter to filter the origins to read")
    optParser.add_option("-m", "--measures", dest="measures", default=None, help="Defines the measures' table to load")
    optParser.add_option("-i", "--value", dest="measuresValue", default='avg_tt', help="Defines the name of the value to load from the measures")
    optParser.add_option("-p", "--projection", dest="projection", type=int, default=25833, help="Sets the projection EPSG number")
    #
    optParser.add_option("-b", "--border", dest="mainBorder", default=None, help="Defines the border geometry to load")
    optParser.add_option("--inner", dest="innerBorders", default=None, help="Defines the optional inner boundaries to load")
    optParser.add_option("--bounds", dest="bounds", default=None, help="Defines the bounding box to show")
    #
    optParser.add_option("-n", "--net", dest="net", default=None, help="Defines the optional road network source")
    optParser.add_option("--water", dest="water", default=None, help="Defines the optional water source")
    #
    optParser.add_option("-C", "--colmap", dest="colmap", default="RdYlGn_r", help="Defines the color map to use")
    optParser.add_option("--contour", dest="contour", action="store_true", default=False, help="Triggers contour rendering")
    optParser.add_option("-t", "--title", dest="title", default=None, help="Sets the figure title")
    optParser.add_option("--from.borderwidth", dest="from_borderwidth", type=float, default=1., help="Sets the width of the border of the loaded objects")
    #
    optParser.add_option("-o", "--output", dest="output", default=None, help="Defines the name of the graphic to generate")
    optParser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False, help="Triggers verbose output")
    optParser.add_option("-S", "--no-show", dest="no_show", action="store_true", default=False, help="Does not show the figure if set")
  
    options, remaining_args = optParser.parse_args()
    if options.objects==None:
        print ("You have to define the objects to load using '--from / -f")
    if options.measures==None:
        print ("You have to define the measures to load using '--measures / -m")
    if options.objects==None or options.measures==None:
        exit()
    return options, remaining_args


def load_shapes(source, projection, asCentroid=False, idField="gid", geomField="polygon", geomFilter=None):
    (host, db, tableFull, user, password) = source.split(";")
    (schema, table) = tableFull.split(".")
    where = ""
    if geomFilter is not None:
        where = " WHERE %s" % geomFilter
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    if asCentroid: 
        cursor.execute("SELECT %s,ST_AsText(ST_Centroid(ST_Transform(%s, %s))) FROM %s.%s%s" % (idField, geomField, projection, schema, table, where))
    else: 
        print ("SELECT %s,ST_AsText(ST_Transform(%s, %s)) FROM %s.%s%s" % (idField, geomField, projection, schema, table, where))
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



def load_measures(source, measure):
    (host, db, tableFull, user, password) = source.split(";")
    (schema, table) = tableFull.split(".")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    cursor.execute("SELECT fid,%s FROM %s.%s" % (measure, schema, table))
    obj2value = {}
    for r in cursor.fetchall():
        obj2value[int(r[0])] = float(r[1])
    return obj2value


if __name__ == "__main__":
    options, remaining_args = parse_options()
    
    # -- load objects
    # load the main border optionally
    mainBorder = None
    if options.mainBorder!=None:
        shapes = load_shapes(options.mainBorder, options.projection, False)
        mainBorder = shapes[list(shapes.keys())[0]] # use only the first geometry
    # load the inner borders optionally
    innerBorders = load_shapes(options.innerBorders, options.projection, False) if options.innerBorders is not None else None
    # load the network optionally
    network = net.loadNet("postgresql:"+options.net, options.projection, ["street_type"]) if options.net is not None else None
    # load the water optionally
    water = load_shapes(options.water, options.projection, False) if options.water is not None else None
    # load shapes and measures
    obj2geom = load_shapes(options.objects, options.projection, options.contour, options.objectsID, options.objectsGeom, options.objectsFilter)
    obj2value = load_measures(options.measures, options.measuresValue)
    
    # -- draw
    # open figure
    figsize = (8,5)
    fig = matplotlib.pyplot.figure(figsize=figsize)
    ax = fig.add_subplot(111)
    #
    invalidColor = "azure"
    colormap = matplotlib.pyplot.get_cmap(options.colmap)
    # build clip
    clip = mainBorder.artist(lw=2, fc="red", ec="black", transform=ax.transData) if mainBorder is not None else None
    # draw
    if options.contour:
        plot_area_contours(fig, ax, obj2geom, obj2value, colormap)
    else:
        plot_area_objects(fig, ax, obj2geom, obj2value, colormap, from_borderwidth=options.from_borderwidth)
    if water is not None:
        for w in water:
            p = water[w].artist(fc="#3cacd5", lw=0, zorder=900)
            ax.add_artist(p)
    if innerBorders is not None:
        ax.add_collection(PatchCollection(innerBorders, facecolors='none', linewidths=1., edgecolor="black", zorder=40  ))
    if network is not None:
        net.plotNet(ax, network, color="#000000", alpha=1)
    # apply clipping
    for o in ax.get_children():
        o.set_clip_path(clip)
    # add bounds after clipping
    if mainBorder is not None:
        ax.add_patch(mainBorder.artist(lw=2, fc="none", ec="black", zorder=1000))
        ax.add_patch(mainBorder.artist(lw=0, fc=invalidColor, ec="black", zorder=-1))
    # decorate
    if options.title is not None: 
        matplotlib.pyplot.title(title.replace("\\n", "\n"), size=16)
    
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
            bounds = spatialhelper.geometries_bounds(list(obj2pos.values()))
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

    # show / save
    if options.output is not None:
        matplotlib.pyplot.savefig(options.output)
    if not options.no_show:
        matplotlib.pyplot.show()
  
  