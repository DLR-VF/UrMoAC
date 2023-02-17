# Tutorial: Computing the accessibility to the next public transport halt

## Task
Computing the access to the next public transport halt within a region is a very common use case for UrMoAC. Within this tutorial, a basic usage of the tool for this purpose is described and discussed. This tutorial shows as well how to apply a spatial aggregation of the results.



## Prerequisites
For the first, you will need:

* tools and applications: UrMoAC, [Java](https://java.com/), [Python](https://www.python.org/), [PostgreSQL](https://www.postgresql.org/) with [PostGIS](https://postgis.net/) extensions
* data: the origins (buildings) and destinations (public transport halts) and the road network



## Basic Computation

### Step 1: get the data
Download an area you are interested in from [OpenStreetMap](http://www.openstreetmap.org). We will use a representation of the city of Berlin for our examples. This data already contains buildings, public transport halts, and a road network. Please note that the quality of [OpenStreetMap](http://www.openstreetmap.org) differs between regions. For Germany, the data is quite complete and up-to-date.

As said, we will use the city of Berlin as an example. We use a data set available on [Geofabrik](https://download.geofabrik.de/). the one we used was generated on the 10<sup>th</sup> of March 2021. It looks like this:

[[https://github.com/DLR-VF/UrMoAC/blob/master/images/scr_berlin.png|alt=berlin]]
Fig. 1: The used Berlin data set (see the external article about [Generating scalable maps with OSM and SUMO](http://www.krajzewicz.de/blog/sumo-scalable-osm-maps.php) for more information)

### Step 2: import the data into a database
#### Step 2.1: import OSM data
UrMoAC comes with a Python help script for importing [OpenStreetMap](http://www.openstreetmap.org) data into a database. My call for importing the downloaded and extracted OSM-file into my local database is as following:

    ...\tools\osm>python osm2db.py localhost;urmoac_tests;berlin.osm20210310;<USER>;<PASSWD> data\berlin20210310.osm

Please note that you need the PostGis extensions to be installed. If not, use:

    CREATE EXTENSION postgis;

As described in the section about [import scripts](ImportScripts), osmdb gets the definition of the database to generate as the first, and about the file to parse as the second parameter. The format of the first (see [import scripts](ImportScripts) ) is &lt;HOST&gt;;&lt;DB&gt;;&lt;SCHEMA&gt;.&lt;PREFIX&gt;;&lt;USER&gt;;&lt;PASSWD&gt;.

The tool builds the tables as given in [import scripts](ImportScripts) and reports about inserting nodes, ways, and relations. It takes some time, for Berlin, with 5.7Mio nodes, .8Mio ways, and 14k relations, my computer needed about 25 minutes.

#### Step 2.2: prepare the road network
As described in [import scripts](ImportScripts), you may use the [osmdb_buildWays.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildWays.py) script to build your road network from a previously imported OSM data. In our case, the call is: 

    ...\tools\osm>python osmdb_buildWays.py localhost;urmoac_tests;berlin.osm20210310;<USER>;<PASSWD>

You may note that the tool reports about unknown highway or railway tags. Usually, these are yet unbuilt or even erased roads. For importing the road network of Berlin (about 1Mio edges), the tool needed about six minutes.

#### Step 2.3: prepare the buildings (origins)
Use the tool [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) (see [import scripts](ImportScripts) ) to import buildings by calling:

    ...\tools\osm>python osmdb_buildStructures.py localhost;urmoac_tests;berlin.osm20210310;<USER>;<PASSWD> structure_defs/defs_buildings.txt localhost;urmoac_tests;berlin.osm20210310_buildings;<USER>;<PASSWD>

You will obtain a table named &ldquo;osm20210310_buildings&rdquo; that includes the buildings.

#### Step 2.4: prepare the public transport halts (destinations)
Again, you may use [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) (see [import scripts](ImportScripts) ) to import public transport halts. The call is:

    ...\tools\osm>python osmdb_buildStructures.py localhost;urmoac_tests;berlin.osm20181028;<USER>;<PASSWD> structure_defs/def_pt_halts.txt localhost;urmoac_tests;berlin.osm20210310_pthalts;<USER>;<PASSWD>

You will obtain a table named &ldquo;osm20210310_pthalts&rdquo; that includes the public transport halts.

### Step 3: compute the accessibility
After having imported our data, we can simply run UrMoAC for computing the access to the next public transport halt.

The call looks like the following:

    ...>java --from "db;jdbc:postgresql://localhost/urmoac_tests;berlin.osm20210310_buildings;<USER>;<PASSWD>" --to "db;jdbc:postgresql://localhost/urmoac_tests;berlin.osm20210310_pthalts;<USER>;<PASSWD>" --net "db;jdbc:postgresql://localhost/urmoac_tests;berlin.osm20181028_network;<USER>;<PASSWD>" --time 28800 --mode foot --epsg 25833 --nm-output "houses2pthalts.csv" --verbose --shortest

The options mean the following:

* --from &ldquo;db;jdbc:postgresql://localhost/urmoac_tests;berlin.osm20210310_buildings;&lt;USER&gt;;&lt;PASSWD&gt;&rdquo;: use the buildings stored in the database table berlin.osm20210310_buildings as origins
* --to &ldquo;db;jdbc:postgresql://localhost/urmoac_tests;berlin.osm20210310_pthalts;&lt;USER&gt;;&lt;PASSWD&gt;&rdquo;: use the public transport halts stored in the database table berlin.osm20210310_buildings as destinations
* --net &ldquo;db;jdbc:postgresql://localhost/urmoac_tests;berlin.osm20181028_network;&lt;USER&gt;;&lt;PASSWD&gt;&rdquo;: use the network stored in berlin.osm20181028_network for routing
* --time 28800: we do need the time definitions; eventually, this is irrelevant for most modes
* --mode foot: define that we want to compute accessibilities for walking
* --epsg 25833: define the projection
* --nm-output &ldquo;file;houses2pthalts.csv&rdquo;: write a basic output to the file houses2pthalts.csv
* --verbose: report what you do
* --shortest: we want the access to the next public transport halt

Given this, the tool will generate the file &ldquo;houses2pthalts.csv&rdquo; which contains the following information for each origin (building) stored line-by-line:

* building-ID
* pthalt-ID
* travel time in seconds
* distance in meters
* -1, as we did not define a weight
* -1, as we do not aggregate
 
 
### Step 4: display the results
!!!



## Discussion
Ok, we can show the accessibilities, but the map representation showing single buildings can be only badly investigated. What would be nice is an aggregation of areas. We&apos;ll do this in the next step.


