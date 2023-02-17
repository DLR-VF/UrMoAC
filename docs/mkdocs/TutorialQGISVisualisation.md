# Tutorial: Using QGIS to visualise UrMoAC results

## Task
Most of the visualisation we do is done using Python and matplotlib. But you may as well use usual geo information systems (GIS) for this purpose. This tutorial shows how to visualise UrMoAC results with [QGIS](https://www.qgis.org/de/site/).

## Prerequisites
Let&apos;s assume you&apos;d have computed the accessibility from buildings to the next public transport halt as described within the tutorial [Computing the accessibility to the next public transport halt](TutorialNextPTHalt). 

This means, you have the following tables:

* !!!: the buildings
* !!!: the public transport halts
* !!!: the accessibility values computed using UrMoAC

## Visualisation using [QGIS](https://www.qgis.org/de/site/)
Start [QGIS](https://www.qgis.org/de/site/) and load the buildings first. You may have to add your database server to the list of known servers and open the path (host-scheme-table) to the buildings table. You should see something like what is shown in the following image.

!!!

Now, load the accessibility table by choosing the menu !!!. The table !!! should be now visible in your project tree.

Now, connect the buildings to the !!! table. For this purpose, select !!!. 

Now, double click on the buildings and select the !!! visualisation mode. In the !!! selection, choose !!!. Set the number of classes to 20 and press ok. The result are buildings colored by their travel time to the next public transport halt, as shown in the following image.

!!!









