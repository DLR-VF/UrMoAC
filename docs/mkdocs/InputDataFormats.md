# Input Data Formats

UrMoAC reads information about the origins, destinations, aggregation areas, the road network, the public transport system, as well as entrainment options from a database or from files. Below, you may find the definitions of the data formats used for these data types.

**Please note that we currently advice the usage of a database. Here, no projection issues occur as projection to the chosen coordinate system is done using the database&#39;s native methods. Handling of files with different projections is not yet solved, all have to contain positions encoded in the same metric coordinate system. In addition, using a database offers the biggest flexibility in terms of filtering and processing data.**


## Origins and Destinations

Usually, single buildings or other infrastructure (e.g., shops, work places, public transport halts) are used as origins and as destinations for computing accessibility measures. You may, for example, think about computing the accessibility to the next public transport halt. In this case, you would have single buildings as origins and public transport halts as destinations.

Each of these &ldquo;locations&rdquo;, origins and destinations, consists of:

* a __numeric__ ID (identifier);
* a position (geo-location);
* optionally a weighting factor.

The following options are used in combination with this data type:

* __--from _&lt;SOURCES&gt;___: defines where to load origins from;
* __--to _&lt;DESTINATIONS&gt;___: defines where to load destinations from.

We often use locations from [OpenStreetMap](http://www.openstreetmap.org). A [tool for importing them](ImportScripts) into a database is supported.

### Database Format

An origin or a destination is represented using the following attributes in the database:

| Default Column Name | Type | Purpose |
| ---- | ---- | ---- |
| gid | long | Names the object |
| the_geom | PostGIS-Geometry | Defines the object&apos;s position in space |
| N/A (optional) | double | Weights the object |

To load objects from a database, you have to give the complete path to the according database table as well as the credentials needed to access it. As such, the call to load origins from a database looks like: __--from jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___, where:

* ___&lt;DB_HOST&gt;___ is the adress of the database server
* ___&lt;SCHEMA&gt;___ is the database schema the table is located within
* ___&lt;TABLE&gt;___ is the name of the database table
* ___&lt;USER&gt;___ is the name of the user that can read the table
* ___&lt;PASSWORD&gt;___ is the database password of this user

The information where to read the &ldquo;weight&rdquo; of an object can be defined using the options __--weight _&lt;COLUMN_NAME&gt;___ for origins and __--variable _&lt;COLUMN_NAME&gt;___ for destinations.

When loading origins / destinations from a database, additional options can be used to change some defaults:

* __--from.filter _&lt;FILTER&gt;___: defines a filter that is added to the select statement as a `WHERE`-clause.
* __--from.id _&lt;COLUMN_NAME&gt;___: sets the name of the database column to read the object ID from (default is __gid__).
* __--from.geom _&lt;COLUMN_NAME&gt;___: sets the name of the database column to read the object geometry from (default is __the_geom__).

The same options are available for destinations (__--to.filter _&lt;FILTER&gt;___, __--to.id _&lt;COLUMN_NAME&gt;___, __--to.geom _&lt;COLUMN_NAME&gt;___), origin aggregation areas (__--from-agg.filter _&lt;FILTER&gt;___, __--from-agg.id _&lt;COLUMN_NAME&gt;___, __--from-agg.geom _&lt;COLUMN_NAME&gt;___), and destination aggregation areas (__--to-agg.filter _&lt;FILTER&gt;___, __--to-agg.id _&lt;COLUMN_NAME&gt;___, __--to-agg.geom _&lt;COLUMN_NAME&gt;___).

Please note that currently, UrMoAC uses the centroid of a given geometry as the respectively starting / ending position. This means, the geometry of an origin or a destination is arbitrary as long as it can be converted into a point using the PostGIS-function `ST_Centroid`.


### File (.csv) Format

You may load origins / destinations from .csv-files. Here, every origin / destination is stored in one line individually. They should consist of an ID, followed by the x- and the y-coordinate of the respective location&apos;s position or centroid in meters. The following example defines an origin or a destination with the ID 2000, x-position -200 and y-position 0:

```2000;-200;0```

Lines with a leading &lsquo;#&rsquo; and empty lines are omitted.

If the object shall be weighted, the value can be added as an additional field, e.g. (the weight is 5):

```2000;-200;0;5```

Please note that no projection is applied to locations stored in .csv files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;.csv___ and __--to _&lt;MYFILE&gt;.csv___ to load origins or respectively destinations from .csv-files.


### File (.wkt) Format

You may load origins / destinations from .wkt-files. Here, every origin / destination is stored in one line individually. They should consist of an ID, followed by the location&apos;s position or centroid in meters. The following example defines an origin / a destination with the ID 2000, x-position -200 and y-position 0:

```2000;POINT(-200 0)```

If the object shall be weighted, the value can be added as an additional field, e.g. (the weight is 5):

```2000;POINT(-200 0);5```

Lines with a leading &lsquo;#&rsquo; and empty lines are omitted.

Please note that no projection is applied to locations stored in .wkt files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;.wkt;___ and __--to _&lt;MYFILE&gt;.wkt___ to load origins or respectively destinations from .wkt-files.


### SUMO (.poi.xml)

You may load origins / destinations from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).

Please note that currently only __numerical__ shape IDs are accepted.

Please note that no projection is applied to locations stored in [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html) files, thereby you should set __--epsg 0__.

Please note that SUMO shapes do not store a weight.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;_.poi.xml__ and __--to _&lt;MYFILE&gt;_.poi.xml__ to load origins or respectively destinations from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).





## Aggregation Areas

UrMoAC can aggregate the results by variable areas. The aggregation can be applied to origins, destinations, or both.

Each of these &ldquo;areas&rdquo; consist of:

* a __numeric__ ID (identifier);
* a polygon.

The following options are used in combination with this data type:

* __--from-agg _&lt;SOURCES_AGGREGATION_AREAS&gt;___;
* __--to-agg _&lt;DESTINATIONS_AGGREGATION_AREAS&gt;___.

### Database Format

An aggregation area is represented using the following attributes in a database:

| Default Column Name | Type | Purpose |
| ---- | ---- | ---- | 
| gid | int/long | Names the area |
| the_geom | PostGIS-Polygon | Defines the area&apos;s shape |

Use __--from-agg jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___ to load origin aggregation areas from a database. See [Sources and Destinations](./InputDataFormats.md#/#origins-and-destinations) for an explanation.


### File (.csv) Format

You may load aggregation areas from .csv-files. Here, every area is stored in one line individually. The geometry must be a polygon, defined by subsequent x- and y-coordinates in meters. It is not necessary to close the polygon. The following example defines an aggregation area with ID &ldquo;1000&rdquo; having a box shape between -200;-200 and 200;200:

```1000;-200;-200;200;-200;200;200;-200;200```

Lines with a leading &lsquo;#&rsquo; are omitted.

Please note that no projection is applied to aggregation areas stored in .csv files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from-agg _&lt;MYFILE&gt;.csv___ and __--to-agg _&lt;MYFILE&gt;.csv___ to load origin or respectively destination aggregation areas from .csv-files.


### File (.wkt) Format

You may load aggregation areas from .wkt-files. Here, every area is stored in one line individually. The geometry must be a polygon, stored as WKT. The following example defines an aggregation area with ID &ldquo;1000&rdquo; having a box shape between -200;-200 and 200;200:

```1000;POLYGON((-200 -200, 200 -200, 200 200, -200 200, -200 -200))```

Lines with a leading &lsquo;#&rsquo; are omitted.

Please note that no projection is applied to aggregation areas stored in .wkt files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from-agg _&lt;MYFILE&gt;.wkt;___ and __--to-agg _&lt;MYFILE&gt;.wkt___ to load origin or respectively destination aggregation areas from .wkt-files.


### SUMO (.poi.xml)

You may load origin / destination aggregation areas from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).

Please note that currently only __numerical__ shape IDs are accepted.

Please note that no projection is applied to locations stored in [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html) files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;_.poi.xml__ and __--to _&lt;MYFILE&gt;_.poi.xml__ to load origin or respectively destination aggregation areas from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).





## Road Network

The road network is used to route between origins and destinations, taking into regard the maximum allowed speed as well as mode-related restrictions. UrMoAC uses an own road network representation.

The following attributes describe a road:

* ID: the ID of the road;
* from node: the __numeric__ ID of the node (intersection) the road starts at;
* to node: the __numeric__ ID of the node (intersection) the road ends at;
* foot: whether pedestrians may use this road;
* bike: whether bicyclists may use this road;
* car: whether motorised vehicles may use this road;
* speed: the maximum velocity allowed on this road in km/h;
* length: the length of this road in meters;
* geometry: a line strip defining this road&apos;s geometry.

Roads are unidirectional. The geometry is a line strip (multiple line segments). The length should cover the length of the complete line strip.

The following options are used in combination with this data type:

* __--net _&lt;NETWORK&gt;___.

We usually use road networks from [OpenStreetMap](http://www.openstreetmap.org). A [tool for importing them](ImportScripts) into a database is supported.

### Database Format

A road network is defined by the roads it consists of, and each road is represented using the following attributes in a database:

| Column Name | Type | Purpose | 
| ---- | ---- | ---- |
| oid | String | The name of the road | 
| the_geom | PostGIS MultiLineString | The shape of the road | 
| nodefrom | long | The ID of the node the road starts at | 
| nodeto | long | The ID of the node the road ends at | 
| mode_walk | boolean | Whether the road can be used by the mode &ldquo;walking&rdquo;/&ldquo;foot&rdquo; | 
| mode_bike | boolean | Whether the road can be used by the mode &ldquo;bicycling&rdquo;/&ldquo;bike&rdquo; | 
| mode_mit | boolean | Whether the road can be used by the mode &ldquo;motorised individual traffic&rdquo;/&ldquo;car&rdquo;/&ldquo;car&rdquo; | 
| vmax | double | The maximum speed allowed on this road in km/h | 
| length | double | The length of this road | 

Use __--net jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___ to load a network from a database. See [Sources and Destinations](./InputDataFormats.md#/#origins-and-destinations) for an explanation.


### File (.csv) Format

You may load networks from .csv-files. Within a network .csv-file, every road is stored in a single line. 

The following example defines a road with the ID &ldquo;10000&rdquo; connecting nodes 0 and 1, where all modes are allowed. The maximum velocity is 50 km/h, the length is 500 m. The geometry simply spans between the connected nodes.

```10000;0;1;true;true;true;50;500;-250;0;250;0```

The boolean values for &ldquo;foot&rdquo;, &ldquo;bike&rdquo;, and &ldquo;car&rdquo; may be encodes as &ldquo;true&rdquo; or &ldquo;1&rdquo; when the respective mode is allowed, &ldquo;false&rdquo; or &ldquo;0&rdquo; otherwise.

Please note that no projection is applied to aggregation areas stored in .csv files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--net _&lt;MYFILE&gt;.csv___ to load a network from a .csv-file.


### File (.wkt) Format

You may load networks from .wkt-files. Within a network .wkt-file, every road is stored in a single line. 

The following example defines a road with the ID &ldquo;10000&rdquo; connecting nodes 0 and 1, where all modes are allowed. The maximum velocity is 50 km/h, the length is 500 m. The geometry simply spans between the connected nodes.

```10000;0;1;true;true;true;50;500;LINESTRING(-250 0, 250 0)```

The boolean values for &ldquo;foot&rdquo;, &ldquo;bike&rdquo;, and &ldquo;car&rdquo; may be encodes as &ldquo;true&rdquo; or &ldquo;1&rdquo; when the respective mode is allowed, &ldquo;false&rdquo; or &ldquo;0&rdquo; otherwise.

Please note that no projection is applied to aggregation areas stored in .wkt files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--net _&lt;MYFILE&gt;.wkt;___ to load a network from a .wkt-file.


### SUMO (.net.xml)

You may directly load a [SUMO network](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html).

Please note that no projection is applied to [SUMO networks](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html), thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--net _&lt;MYFILE&gt;_.net.xml__ to load a [SUMO networks](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html).





## Speed Time Lines

The average travel time when riding a vehicle depends on the current situation on the roads. Thereby, it is not sufficient to use the maximum allowed velocity. Instead, one should as well define speed time lines. Each entry in the according dataset defines the speed of a single road for a defined time span. Each entry has the following fields:

* edge ID: The ID of the edge as given in the network description
* begin time: The begin of the time span in seconds
* end time: The end of the time span in seconds
* speed: The speed to set for the given edge within the given time span in km/h

The following options are used in combination with this data type:

* __--traveltimes _&lt;TRAVEL_TIMES&gt;___.

We sometimes use the [edge-based traffic measures](https://sumo.dlr.de/wiki/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures) output from [SUMO](http://sumo.dlr.de) for obtaining the speed time lines of a road network. A [tool for importing them](ImportScripts) into the database is supported.

### Database Format

A speed time line entry consists of the following data in the database:

| Column Name | Type | Purpose | 
| ---- | ---- | ---- |
| ibegin | float | The begin of the time interval in s | 
| iend | float | The end of the time interval in s | 
| eid | String| The name of the road as defined in the &ldquo;oid&rdquo; field of the road network | 
| speed | float | The average/current/maximum speed at this road | 

Use __--traveltimes jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___ to load speed time lines from a database. See [Sources and Destinations](./InputDataFormats.md#/#origins-and-destinations) for an explanation.


### File (.csv) Format

Speed time lines can be read from .csv files. Speed information for a specific time span and edge is stored in a single line. The following example defines a speed restriction for edge 10000 between 7:00am and 8:00am of 20 km/h:

```10000;25200;28800;20```

Use __--traveltimes _&lt;MYFILE&gt;.csv;___ to load speed time lines from a .csv-file.


### SUMO (.dump.xml)

You may directly load a [SUMO Edge-Based Network State](https://sumo.dlr.de/docs/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures.html).

The file type is recognized by the extension, i.e. use __--traveltimes _&lt;MYFILE&gt;_.dump.xml__ to load a [SUMO Edge-Based Network State](https://sumo.dlr.de/docs/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures.html).





## O/D-connections

Usually, UrMoAC routes from all given origins to all defined destinations, bound by the given limits. But UrMoAC may as well route between a given set of explicit origin/destination-tuples. Each origin/destination-relationship is defined by:

* origin ID: The ID of the origin to route from as given in the origins;
* destination ID: The ID of the destination to route to as given in the destinations.

The following options are used in combination with this data type:

* __--od-connections _&lt;OD_CONNECTIONS&gt;___.

### Database Format

An OD-connection is represented using the following attributes in the database:

| Default Column Name | Type | Purpose |
| ---- | ---- | ---- |
| origin | long | ID of the origin as loaded using --from |
| destination | long | ID of the origin as loaded using --to|

Use __--od-connections jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___ to load OD-connections from a database. See [Sources and Destinations](./InputDataFormats.md#/#origins-and-destinations) for an explanation.


### File (.csv) Format

You may load OD-connections .csv-files. Each line must contain one origin/destination-tuple. The following example shows a connection between an origin with ID=100 and a destination with ID=200:

```100;200```

Use __--od-connections _&lt;MYFILE&gt;.csv;___ to load OD-connections from a .csv-file.





## Entrainment Definitions

There are different rules for entraining own mobility options within public transport. E.g., it is allowed to take a bike on board of a city rail (S-Bahn) and metro (U-Bahn) as well as of a tram in Berlin, but not allowed to take it into a bus. The entrainment table contains entrainment possibilities. Each allowed entrainment is defined by the following attributes:

* carrier: the major UrMoAC mode, yet usually &ldquo;pt&rdquo; for public transport;
* carrier_subtype: a __numeric__ ID of the pt carrier type, not necessarily as defined in the GTFS standard for the &ldquo;route_type&rdquo; attribute stored in &ldquo;[routes.txt](https://gtfs.org/schedule/reference/#routestxt)&rdquo;;
* carried: the UrMoAC mode that may be entrained.

The following options are used in combination with this data type:

* __--entrainment _&lt;OD_CONNECTIONS&gt;___.

Please note that we do not have much experience in using the entrainment table, yet.

### Database Format

The definition of an entrainment possibility consists of the following data in a database:

| Column Name | Type | Purpose | 
| ---- | ---- | ---- |
| carrier | string | The major mode name | 
| carrier_subtype | string | The subtype as defined in GTFS | 
| carried | string | The name of the entrained mode, e.g. &ldquo;bike&rdquo; | 

Use __--entrainment jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___ to load entrainment definitions from a database. See [Sources and Destinations](./InputDataFormats.md#/#origins-and-destinations) for an explanation.


### File (.csv) Format

You may load entrainment definitions from .csv-files. Each entrainment definition is stored in a single line individually. The following example defines that a bike may be taken on board of a pt subcarrier 400 (i.e. city rail in Berlin&apos;s GTFS definition):

```pt;400;bike```

Use __--entrainment _&lt;MYFILE&gt;.csv;___ to load the entrainment definitions from a .csv-file.





## GTFS

[GTFS](https://gtfs.org/) data can be directly read from the according files.

A [tool for importing them](ImportScripts) into a database is supported.

