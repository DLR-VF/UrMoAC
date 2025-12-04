Below, you may find a brief explanation about how to use UrMoAC.

## Environment

* UrMoAC is run on the [command line](https://en.wikipedia.org/wiki/Command-line_interface). This means, on Windows, you have to open ```cmd.exe```. You should know how to open a terminal if you are working on Linux.
* UrMoAC is written in [Java](https://projects.eclipse.org/projects/adoptium.temurin). You need at least a [Java](https://projects.eclipse.org/projects/adoptium.temurin) interpreter (JRE &mdash; Java Runtime Environment) to run it.
* Some of the supporting import scripts are written in [Python](https://www.python.org/). You need [Python](https://www.python.org/) in version 3 to run them.


## Needed data and data sources

* The tool reads data from **files** or a [PostGIS](https://postgis.net/)-enabled SQL-database. We usually use [PostgreSQL](https://www.postgresql.org/) with [PostGIS](https://postgis.net/) extensions.
* The tool requires at least the following data when started: 

    1. a list of (at least one) origins, see [Input Data Formats/Origins and Destinations](./input_formats/OriginsDestinations.md);
    2. a list of (at least one) destinations, see [Input Data Formats/Origins and Destinations](./input_formats/OriginsDestinations.md);
    3. a road network, see [Input Data Formats/Road Networks](./input_formats/RoadNetwork.md);
    4. the mode of transport to use, defined on the command line;
    5. the time of the day for which the accessibility measures shall be computed, defined on the command line;
    6. the map projection, defined on the command line.

* There are other [options](Options.md) that control the behaviour.
* Of course, the tool assumes specific input data formats for defining origins, destinations, the road network, and, if used, the public transport network. There are some import tools, e.g. for importing [OpenStreetMap](https://www.openstreetmap.org) networks or importing [GTFS](https://gtfs.org/) public transport definitions.

## Basic example

For computing the accessibility of places stored in destinations.csv from places stored in origins.csv by bike using the road network stored in network.csv, write:

```cmd
java -jar UrMoAC.jar --from origins.csv --to destinations.csv --net network.csv --od-output nm_output.csv --mode bike --time 0 --epsg 0
```


## Tutorials
You may find some tutorials that give examples for using the tool at the following pages:

* [Computing the accessibility to the next public transport halt](./tutorials/TutorialNextPTHalt.md)
* [Using QGIS to visualise UrMoAC results](./tutorials/TutorialQGISVisualisation.md)

