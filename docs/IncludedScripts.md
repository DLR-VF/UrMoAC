# Included Scripts and Tools

UrMoAC comes with some auxiliary scripts for different purposes.
These scripts are located in ***&lt;UrMoAC&gt;*/tools** and are sorted by topic:

* **attic**: Old and outdated tools and scripts
* **auxiliary**: Some helper scripts, currently for converting networks and shapes between csv, SUMO, and shapefile formats
* **gtfs**: Scripts for parsing and importing GTFS data sets into a database
* **helper**: Different helper scripts
* **osm**: Scripts for parsing and importing OSM data sets into a database
* **visualisation**: Scripts that visualise the results


## Auxiliary

| Script | Purpose | Call |
| ------ | ------- | ---- |
| csvnet2shapefile.py | Converts a csv net file into a shapefile. | csvnet2shapefile.py *&lt;INPUT_CSV_NET&gt;* *&lt;SHAPEFILE_OUTPUT_PREFIX&gt;* |
| csvshape2shapefile.py | Converts a csv shape file into a shapefile. | csvshape2shapefile.py *&lt;INPUT_CSV_SHAPES&gt;* *&lt;SHAPEFILE_OUTPUT_PREFIX&gt;* |
| csvnet2sumo.py | Converts a csv net file into SUMO nodes and edges files. | csvnet2sumo.py *&lt;INPUT_CSV_NET&gt;* *&lt;SUMO_OUTPUT_PREFIX&gt;* |
| csvshape2sumo.py | Converts a csv shape file into a SUMO shapes file. | csvshape2sumo.py *&lt;INPUT_CSV_SHAPES&gt;* *&lt;SUMO_SHAPE_OUTPUT&gt;* |

## GTFS

| Script | Purpose | Call |
| ------ | ------- | ---- |
| importGTFS.py | Imports a GTFS data set into a database. | see [Importing GTFS](./importer/GTFS.md) |
| gtfs_defs.py | GTFS data definitions built using parse_reference.py. | - |
| parse_reference.py | Builds gtfs_defs.py from a local copy of the GTFS definition. | parse_reference.py (but the definitions should be up-to-date) |

## OSM

| Script | Purpose | Call |
| ------ | ------- | ---- |
| osmmodes.py | Defines modes of transport for the OSM importer. | - |
| osm.py | OSM data model. | - |
| osmdb.py | OSM database representation. | - |
| osm2db.py | Imports an OSM-file into the database. | see [Importing OpenStreetMap into the database](./importer/OpenStreetMap.md#importing-openstreetmap-into-the-database) |
| osmdb_buildStructures.py  | Builds a table with defined structures (not the network) using an OSM-database representation. | see [Using OpenStreetMap data to build tables of certain structures](./importer/OpenStreetMap.md#using-openstreetmap-data-to-build-tables-of-certain-structures) |
| osmdb_buildWays.py  | Builds a road network table using  an OSM-database representation. | see [Building the road network from OpenStreetMap data](./importer/OpenStreetMap.md#building-the-road-network-from-openstreetmap-data) |

## Helper

| Script | Purpose | Call |
| ------ | ------- | ---- |
| colorhelper.py | Colors and colormap helper. | - |
| colormap.py | Colormap helper. | - |
| figureshelper.py | Some plotting helper. | - |
| geom_helper.py | Some geometry helper. | - |
| net.py  | A network representation. | - |
| simplemaps.py | Some simple maps for data categorization. | - |
| spatialhelper.py | Methods for computing boundaries and centers. | - |
| wkt.py | Defines geometry objects and parses WKT. | - |

## Visualisation

| Script | Purpose | Call |
| ------ | ------- | ---- |
| plot_area.py | Plots measures on a map. | see [plot_area](./eval/PlotArea.md) |
