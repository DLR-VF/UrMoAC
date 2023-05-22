from optparse import OptionParser
import matplotlib
import matplotlib.pyplot
import matplotlib.patches
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
import spatialhelper



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
  
  

def plotArea_Contours(shapel, obj2pos, obj2val, colmap, shapes2=None, title=None, bounds=None, figsize=(8,5), invalidColor="azure"):
    # compute the boundary to show if not given
    if bounds==None:
        bounds = spatialhelper.getBounds(list(obj2pos.values()))
    # compute points
    xs = []
    ys = []
    zs = []
    for g in obj2pos:
        if g in obj2val: # !!! warn
            xs.append(obj2pos[g][0])
            ys.append(obj2pos[g][1])
            zs.append(obj2val[g])
    xi = numpy.linspace(min(xs), max(xs), 1000)
    yi = numpy.linspace(min(ys), max(ys), 1000)
    xi, yi = numpy.meshgrid(xi, yi)
    zi = scipy.interpolate.griddata((xs, ys), zs, (xi, yi), method='linear')
    # open figure
    fig = matplotlib.pyplot.figure(figsize=figsize)
    ax = fig.add_subplot(111)
    # set clip
    polys = []
    if shapel is not None:
        for poly in shapel:
            polys.append(matplotlib.pyplot.Polygon(poly[0], lw=2, fc="none", ec="black", zorder=1000))
            polys.append(matplotlib.pyplot.Polygon(poly[0], lw=0, fc=colmap(1800), ec="black", zorder=-1))
        [ax.add_patch(i) for i in polys]
    # set figure boundaries and axes
    #
    levels = [0, 300, 600, 900, 1200, 1500, 1800]
    valueMeasure = "s"
    clip = None
    if shapel is not None:
        # https://stackoverflow.com/questions/69268369/how-to-use-set-clip-path-with-multiple-polygons
        vertices = [i.get_path().vertices for i in polys]
        vertices = [item for sub_list in vertices for item in sub_list]
        codes = [i.get_path().codes for i in polys]
        codes = [item for sub_list in codes for item in sub_list]
        #print (codes)
        clip = matplotlib.patches.PathPatch(matplotlib.patches.Path(vertices, codes), transform=ax.transData)
        #ax.add_patch( patchL )
    cs1 = matplotlib.pyplot.contourf(xi, yi, zi, levels=levels, cmap=colmap, zorder=20)
    if shapes2: colB = ax.add_collection( PatchCollection( shapes2, facecolors='none', linewidths=1., edgecolor="black", zorder=40  ))
    if clip is not None:
        for col in cs1.collections:
            col.set_clip_path(clip)
    sm = matplotlib.cm.ScalarMappable(cmap=colmap, norm=matplotlib.pyplot.Normalize(vmin=0, vmax=600))
    # fake up the array of the scalar mappable. Urgh..." (pelson, http://stackoverflow.com/questions/8342549/matplotlib-add-colorbar-to-a-sequence-of-line-plots)
    sm._A = []
    labels = []
    for il,l in enumerate(levels):
        if il==0: continue 
        if il==len(levels)-1: l = str(levels[il-1]) + "- " + valueMeasure
        else: l = str(levels[il-1]) + "-" + str(levels[il]) + " " + valueMeasure
        labels.append(l)
    if True:
        cbar = colorbar_index(len(levels[1:]), colmap, labels)#["300s","600s","900s","1200s","1500s","1800s"])
        cbar.ax.tick_params(labelsize=12)   
    if title is not None: 
        title = title.replace("\\n", "\n")
        matplotlib.pyplot.title(title, size=16)
    matplotlib.pyplot.xlim(bounds[0], bounds[2])
    matplotlib.pyplot.ylim(bounds[1], bounds[3])
    ax.grid(False)
    ax.set_xticklabels([])
    ax.set_yticklabels([])
    matplotlib.pyplot.tick_params(axis='x', which='both', bottom=False, top=False, labelbottom=False)
    matplotlib.pyplot.tick_params(axis='y', which='both', left=False, right=False, labelleft=False)
    matplotlib.pyplot.gca().xaxis.set_major_locator(matplotlib.pyplot.NullLocator())
    matplotlib.pyplot.gca().yaxis.set_major_locator(matplotlib.pyplot.NullLocator())
    ax.set_aspect('equal', 'datalim')
    matplotlib.pyplot.subplots_adjust(bottom=0.05, right=.96, left=.02, top=0.9, wspace=0)
    return fig, ax


def getOptions():
    optParser = OptionParser(usage="""usage: %prog [options].""")
    optParser.add_option("-f", "--from", dest="objects",default=None, help="Defines the objects (origins) to load")
    optParser.add_option("--from.id", dest="objectsID", default="gid", help="Defines the name of the field to read the object ids from")
    optParser.add_option("--from.geom", dest="objectsGeom",default="the_geom", help="Defines the name of the field to read the object geometries from")
    optParser.add_option("-b", "--border", dest="mainBorder", default=None, help="Defines the border geometry to load")
    optParser.add_option("-m", "--measures", dest="measures", default=None, help="Defines the measures to load")
    optParser.add_option("-i", "--index", dest="measuresIndex", default=2, type=int, help="Defines the index of the measure to use")
    optParser.add_option("--inner", dest="innerBorders", default=None, help="Defines the optionsl inner boundaries")
    optParser.add_option("-o", "--output", dest="output", default=None, help="Defines the name of the graphic to generate")
    optParser.add_option("-C", "--colmap", dest="colmap", default="RdYlGn_r", help="Defines the color map to use")
    optParser.add_option("-v", "--verbose", dest="verbose", action="store_true", default=False, help="Triggers verbose output")
    optParser.add_option("-S", "--no-show", dest="no_show", action="store_true", default=False, help="Does not show the figure if set")
    optParser.add_option("-p", "--projection", dest="projection", type=int, default=25833, help="Sets the projection EPSG number")
    optParser.add_option("-t", "--title", dest="title", default=None, help="Sets the figure title")
  
    options, remaining_args = optParser.parse_args()
    if options.objects==None:
        print ("You have to define the objects to load using '--from / -f")
    #if options.mainBorder==None:
    #    print ("You have to define the main border (region clip) to load using '--border / -b")
    if options.measures==None:
        print ("You have to define the measures to load using '--measures / -m")
    if options.objects==None or options.measures==None:
        exit()
    return options, remaining_args


def loadShapes(source, projection, asCentroid=False, idField="gid", geomField="polygon"):
    (host, db, tableFull, user, password) = source.split(";")
    (schema, table) = tableFull.split(".")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    if asCentroid: cursor.execute("SELECT %s,ST_AsText(ST_Centroid(ST_Transform(%s, %s))) FROM %s.%s" % (idField, geomField, projection, schema, table))
    else: cursor.execute("SELECT %s,ST_AsText(ST_Transform(%s, %s)) FROM %s.%s" % (idField, geomField, projection, schema, table))
    obj2geom = {}
    for r in cursor.fetchall():
        if r[1].startswith("POINT"):
            geom = wkt.parsePOINT2XY(r[1])
        elif r[1].startswith("POLYGON"):
            geom = wkt.parsePOLY2XYlists(r[1])
        elif r[1].startswith("MULTIPOLYGON"):
            geom = wkt.parseMULTIPOLY2XYlists(r[1])
        else:
            print ("unsupported geometry %s" % r[1])
            exit()
        obj2geom[int(r[0])] = geom
    return obj2geom



def loadMeasures(source, sourceIndex):
    (host, db, tableFull, user, password) = source.split(";")
    (schema, table) = tableFull.split(".")
    conn = psycopg2.connect("dbname='%s' user='%s' host='%s' password='%s'" % (db, user, host, password))
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM %s.%s" % (schema, table))
    obj2value = {}
    for r in cursor.fetchall():
        obj2value[int(r[0])] = float(r[sourceIndex])
    return obj2value


if __name__ == "__main__":
    options, remaining_args = getOptions()
    mainBorder = None
    if options.mainBorder!=None:
        mainBorder = loadShapes(options.mainBorder, options.projection, False)
        mainBorder = mainBorder[list(mainBorder.keys())[0]]
    innerBorders = None
    if options.innerBorders!=None:
        innerBorders = loadShapes(options.innerBorders, options.projection, False)
    obj2geom = loadShapes(options.objects, options.projection, True, options.objectsID, options.objectsGeom)
    obj2value = loadMeasures(options.measures, options.measuresIndex)
    plotArea_Contours(mainBorder, obj2geom, obj2value, matplotlib.pyplot.get_cmap(options.colmap), innerBorders, title=options.title)
    if options.output is not None:
        matplotlib.pyplot.savefig(options.output)
    if not options.no_show:
        matplotlib.pyplot.show()
  
  