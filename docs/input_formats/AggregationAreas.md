# Aggregation Areas

UrMoAC can aggregate the results by variable areas. The aggregation can be applied to origins, destinations, or both.

Each of these &ldquo;areas&rdquo; consist of:

* a __numeric__ ID (identifier);
* a polygon.

The following options are used in combination with this data type:

* __--from-agg _&lt;SOURCES_AGGREGATION_AREAS&gt;___;
* __--to-agg _&lt;DESTINATIONS_AGGREGATION_AREAS&gt;___.

## Database Format

An aggregation area is represented using the following attributes in a database:

| Default Column Name | Type | Purpose |
| ---- | ---- | ---- | 
| id | int/long | Names the area |
| geom | PostGIS-Polygon | Defines the area&apos;s shape |

Use __--from-agg jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___ to load origin aggregation areas from a database. See [Sources and Destinations](./OriginsDestinations.md) for an explanation.


## File (.csv) Format

You may load aggregation areas from .csv-files. Here, every area is stored in one line individually. The geometry must be a polygon, defined by subsequent x- and y-coordinates in meters. It is not necessary to close the polygon. The following example defines an aggregation area with ID &ldquo;1000&rdquo; having a box shape between -200;-200 and 200;200:

```1000;-200;-200;200;-200;200;200;-200;200```

Lines with a leading &lsquo;#&rsquo; are omitted.

Please note that no projection is applied to aggregation areas stored in .csv files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from-agg _&lt;MYFILE&gt;.csv___ and __--to-agg _&lt;MYFILE&gt;.csv___ to load origin or respectively destination aggregation areas from .csv-files.


## File (.wkt) Format

You may load aggregation areas from .wkt-files. Here, every area is stored in one line individually. The geometry must be a closed polygon, stored as WKT. The following example defines an aggregation area with ID &ldquo;1000&rdquo; having a box shape between -200;-200 and 200;200:

```1000;POLYGON((-200 -200, 200 -200, 200 200, -200 200, -200 -200))```

Lines with a leading &lsquo;#&rsquo; are omitted.

Please note that no projection is applied to aggregation areas stored in .wkt files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from-agg _&lt;MYFILE&gt;.wkt;___ and __--to-agg _&lt;MYFILE&gt;.wkt___ to load origin or respectively destination aggregation areas from .wkt-files.


## SUMO (.poi.xml)

You may load origin / destination aggregation areas from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).

Please note that currently only __numerical__ shape IDs are accepted.

Please note that no projection is applied to locations stored in [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html) files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;_.poi.xml__ and __--to _&lt;MYFILE&gt;_.poi.xml__ to load origin or respectively destination aggregation areas from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).



