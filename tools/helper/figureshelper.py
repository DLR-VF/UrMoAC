#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""Some plotting helper."""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2017-2024, Institute of Transport Research, German Aerospace Center (DLR)"
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
import os
from pylab import *
import matplotlib.patches as patches
from matplotlib.collections import PatchCollection
import colorhelper


# --- function definitions --------------------------------------------------
def saveShowClear(options, oName):
    fp = os.path.join(options.output, oName)
    savefig(fp+".png")
    #savefig(fp+".svg")
    #os.system("java -jar c:\\MehrProgramme\\svg2emf.jar "+fp+".svg")
    if options.show: show()
    clf()
    close()        


def plotArea(options, shapes, colors, cmap, cmin, cmax, bounds, ftitle, oName, explColors=None): 
    fig = figure(figsize=(8,6))
    ax = fig.add_subplot(111)
    if cmin==None or cmax==None:
        for c in colors:
            if cmin==None or c<cmin: cmin=c
            if cmax==None or c>cmax: cmax=c
        
    for i,c in enumerate(colors):
        cNorm    = matplotlib.colors.Normalize(vmin=cmin, vmax=cmax)
        scalarMap = matplotlib.cm.ScalarMappable(norm=cNorm, cmap=cmap)
        if explColors==None or i not in explColors:
            colors[i] = scalarMap.to_rgba(colors[i])
        else:
            colors[i] = explColors[i]
    ax.add_collection( PatchCollection ( shapes, cmap="bone", facecolor=colors, linewidths=.2, edgecolor="black" ) )
    xlim(bounds[0], bounds[2])
    ylim(bounds[1], bounds[3])
    title(ftitle, size=16)
    sm = matplotlib.cm.ScalarMappable(cmap=get_cmap(cmap), norm=plt.Normalize(vmin=cmin, vmax=cmax))
    # fake up the array of the scalar mappable. Urgh..." (pelson, http://stackoverflow.com/questions/8342549/matplotlib-add-colorbar-to-a-sequence-of-line-plots)
    sm._A = []
    colorbar(sm)
    saveShowClear(options, oName)



        

        
def plotPie(options, tMap, tTitle, oName, colors=None, codes=None, withEmpty=False, legendPos=None, legendCols=None):
    labels = []
    values = []
    colors = []
    vsum = 0
    if codes!=None:
        for e in codes:
            if withEmpty or e[0] in tMap:
                labels.append(e[1])
                if e[0] not in tMap: values.append(0)
                else:
                    values.append(tMap[e[0]])
                    vsum += tMap[e[0]]
                colors.append(e[2])
    else:
        colors = None
        for e in tMap:
            if tMap[e]!=0:
                labels.append(e)
                values.append(tMap[e])
                vsum += tMap[e]
    fig = figure(figsize=(6,6))
    ax = fig.add_subplot(111)
    patches, texts, autotexts = pie(values, autopct='%1.1f%%', startangle=90, colors=colors, textprops={"fontsize":18, "color":"white"})
    if legendPos==None: legend(labels)
    else: legend(labels, loc=legendPos, ncol=legendCols)
    title(tTitle, size=18)
    saveShowClear(options, oName)

        
def plotHist(options, hist, binSize, xlimV, tTitle, oName, color=None, ylimV=None, xLabel=None):
    fig = figure(figsize=(8,6))
    ax = fig.add_subplot(111)
    xs = range(0, binSize*len(hist), binSize)
    bar(xs, hist, width=binSize*.8)
    #legend(labels)
    title(tTitle, size=18)
    if xlimV!=None: xlim(xlimV[0], xlimV[1])
    if ylimV!=None: ylim(ylimV[0], ylimV[1])
    if xLabel!=None: xlabel(xLabel)
    ylabel("occurences [#]")
    saveShowClear(options, oName)


def plotStackedBar(options, tMap, tTitle, oName, codes, withEmpty=False, legendPos=None, legendCols=None):
    fig = figure(figsize=(12,3))
    ax = fig.add_subplot(111)
    patch_handles = []
    left = 0
    for c in codes:
        if c[0] in tMap:
            patch_handles.append(ax.barh(0, tMap[c[0]], color=c[2], label=c[1], align='center', left=left, height=.6))
            left += tMap[c[0]]
    # go through all of the bar segments and annotate
    """
    for j in xrange(len(patch_handles)):
        for i, patch in enumerate(patch_handles[j].get_children()):
            bl = patch.get_xy()
            x = 0.5*patch.get_width() + bl[0]
            y = 0.5*patch.get_height() + bl[1]
            ax.text(x,y, "%d%%" % (percentages[i,j]), ha='center')
    """
    if legendPos==None: legend()
    else: legend(loc=legendPos, ncol=legendCols)
    yticks([-1, 1])
    ylim(-.5, .5)
    title(tTitle, size=18)
    saveShowClear(options, oName)



def plotAgePyramid(options, tMapF, tMapM, tTitle, oName, colors=["red", "blue"], agg=1):
    matplotlib.rc('xtick', labelsize=18)
    matplotlib.rc('ytick', labelsize=18)
    import matplotlib.ticker as ticker
    ages = set()
    ages |= set(tMapF.keys())
    ages |= set(tMapM.keys())
    minAge = min(ages)
    maxAge = max(ages)
    y = range(minAge, maxAge+1, agg)
    valsF = [0]*(len(y))
    valsM = [0]*(len(y))
    for a in range(minAge, maxAge+1):
        if a in tMapF: valsF[a/agg-minAge] = valsF[a/agg-minAge] + tMapF[a] 
        if a in tMapM: valsM[a/agg-minAge] = valsM[a/agg-minAge] + tMapM[a] 
    for a in range(0, len(valsF)):
        valsF[a] = valsF[a] / 1000.
        valsM[a] = valsM[a] / 1000. 
    xMax = max(max(valsF), max(valsM)) * 1.1
    fig, axes = plt.subplots(figsize=(6,6), ncols=2, sharey=True)
    axes[0].xaxis.set_major_locator(ticker.MultipleLocator(10))
    axes[1].xaxis.set_major_locator(ticker.MultipleLocator(10))
    axes[0].yaxis.set_major_locator(ticker.MultipleLocator(10))
    axes[0].yaxis.tick_right()
    axes[1].yaxis.tick_left()
    axes[0].barh(y, valsF, align='center', color=colors[0], height=agg*.8)
    axes[0].set_xlim(0, xMax)
    #axes[0].tick_params(axis='both', labelsize=16)
    axes[1].barh(y, valsM, align='center', color=colors[1], height=agg*.8)
    axes[1].set_xlim(0, xMax)
    #axes[1].tick_params(axis='both', labelsize=16)
    axes[0].invert_xaxis()
    axes[0].set_ylim(minAge-agg/2., maxAge+agg/2.)
    if tTitle!=None: plt.suptitle(tTitle+" [persons/1000]", size=16)
    saveShowClear(options, oName)


def plotSplitAgePyramid(options, tMapF, tMapM, tTitle, oName, colors=[["red", "light red"], ["blue", "light blue"]], codes=None, agg=1):
    matplotlib.rc('xtick', labelsize=18)
    matplotlib.rc('ytick', labelsize=18)
    import matplotlib.ticker as ticker
    ages = set()
    ages |= set(tMapF.keys())
    ages |= set(tMapM.keys())
    minAge = min(ages)
    maxAge = max(ages)
    
    
    y = range(minAge, maxAge+1, agg)
    valsF = []
    valsM = []
    valsFsum = [0]*len(y)
    valsMsum = [0]*len(y)
    colorsF = []
    colorsM = []

    if codes!=None:
        bs = []
        for e in codes:
            valsF.append([0]*len(y))
            valsM.append([0]*len(y))

            for a in range(minAge, maxAge+1):
                if a in tMapF and e[0] in tMapF[a]: 
                    valsF[-1][a/agg-minAge] = valsF[-1][a/agg-minAge] + tMapF[a][e[0]]
                    valsFsum[a/agg-minAge] = valsFsum[a/agg-minAge] + tMapF[a][e[0]]
                if a in tMapM and e[0] in tMapM[a]: 
                    valsM[-1][a/agg-minAge] = valsM[-1][a/agg-minAge] + tMapM[a][e[0]]
                    valsMsum[a/agg-minAge] = valsMsum[a/agg-minAge] + tMapM[a][e[0]]
            colorsF.append(colorhelper.lighten("#ff0000", e[2]))
            colorsM.append(colorhelper.lighten("#0000ff", e[2]))
            bs.append(e[0])
    else:
        bs = set()
        for a in ages:
            if a in tMapF:
                for b in tMapF[a]:
                    bs.add(b)
            if a in tMapM:
                for b in tMapM[a]:
                    bs.add(b)
            colors = None
        bs = list(bs)
        for b in bs:
            valsF.append([0]*len(y))
            valsM.append([0]*len(y))
            for a in range(minAge, maxAge+1):
                if a in tMapF and b in tMapF[a]: 
                    valsF[-1][a/agg-minAge] = valsF[-1][a/agg-minAge] + tMapF[a][b]
                    valsFsum[a/agg-minAge] = valsFsum[a/agg-minAge] + tMapF[a][b]
                if a in tMapM and b in tMapM[a]: 
                    valsM[-1][a/agg-minAge] = valsM[-1][a/agg-minAge] + tMapM[a][b]
                    valsMsum[a/agg-minAge] = valsMsum[a/agg-minAge] + tMapM[a][b]

    xMax = max(max(valsFsum), max(valsMsum)) * 1.1
    fig, axes = plt.subplots(figsize=(6,6), ncols=2, sharey=True)
    axes[0].xaxis.set_major_locator(ticker.MultipleLocator(5000))
    axes[1].xaxis.set_major_locator(ticker.MultipleLocator(5000))
    axes[0].yaxis.set_major_locator(ticker.MultipleLocator(10))
    axes[0].yaxis.tick_right()
    axes[1].yaxis.tick_left()
    bottomF = [0]*(len(y))
    bottomM = [0]*(len(y))
    for i,b in enumerate(bs):
        axes[0].barh(y, valsF[i], left=bottomF, align='center', color=colorsF[i], height=agg*.8)
        bottomF = [xV + yV for xV, yV in zip(bottomF, valsF[i])]
        axes[1].barh(y, valsM[i], left=bottomM, align='center', color=colorsM[i], height=agg*.8)
        bottomM = [xV + yV for xV, yV in zip(bottomM, valsM[i])]
    axes[0].set_xlim(0, xMax)
    axes[0].tick_params(axis='both', labelsize=16)
    axes[1].set_xlim(0, xMax)
    axes[1].tick_params(axis='both', labelsize=16)
    axes[0].invert_xaxis()
    axes[0].set_ylim(minAge-agg/2., maxAge+agg/2.)
    if tTitle!=None: plt.suptitle(tTitle, size=18)
    saveShowClear(options, oName)

