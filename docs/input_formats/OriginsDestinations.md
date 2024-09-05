# Origins and Destinations

Usually, single buildings or other infrastructure (e.g., shops, work places, public transport halts) are used as origins and as destinations for computing accessibility measures. You may, for example, think about computing the accessibility to the next public transport halt. In this case, you would have single buildings as origins and public transport halts as destinations.

Each of these &ldquo;locations&rdquo;, origins and destinations, consists of:

* a __numeric__ ID (identifier);
* a position (geo-location);
* optionally a weighting factor.

The following options are used in combination with this data type:

* __--from _&lt;SOURCES&gt;___: defines where to load origins from;
* __--to _&lt;DESTINATIONS&gt;___: defines where to load destinations from.

We often use locations from [OpenStreetMap](http://www.openstreetmap.org). A [tool for importing them](../importer/OpenStreetMap.md) into a database is supported.

## Database Format

An origin or a destination is represented using the following attributes in the database:

| Default Column Name | Type | Purpose |
| ---- | ---- | ---- |
| id | long | Names the object |
| geom | PostGIS-Geometry | Defines the object&apos;s position in space |
| N/A (optional) | double | Weights the object |

To load objects from a database, you have to give the complete path to the according database table as well as the credentials needed to access it. As such, the call to load origins from a database looks like: __--from jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___ where:

* ___&lt;DB\_HOST&gt;___ is the adress of the database server
* ___&lt;SCHEMA&gt;___ is the database schema the table is located within
* ___&lt;TABLE&gt;___ is the name of the database table
* ___&lt;USER&gt;___ is the name of the user that can read the table
* ___&lt;PASSWORD&gt;___ is the database password of this user

The information where to read the &ldquo;weight&rdquo; of an object can be defined using the options __--weight _&lt;COLUMN_NAME&gt;___ for origins and __--variable _&lt;COLUMN_NAME&gt;___ for destinations.

When loading origins / destinations from a database, additional options can be used to change some defaults:

* __--from.filter _&lt;FILTER&gt;___: defines a filter that is added to the select statement as a `WHERE`-clause.
* __--from.id _&lt;COLUMN_NAME&gt;___: sets the name of the database column to read the object ID from (default is __id__).
* __--from.geom _&lt;COLUMN_NAME&gt;___: sets the name of the database column to read the object geometry from (default is __geom__).

The same options are available for destinations (__--to.filter _&lt;FILTER&gt;___, __--to.id _&lt;COLUMN_NAME&gt;___, __--to.geom _&lt;COLUMN_NAME&gt;___), origin aggregation areas (__--from-agg.filter _&lt;FILTER&gt;___, __--from-agg.id _&lt;COLUMN_NAME&gt;___, __--from-agg.geom _&lt;COLUMN_NAME&gt;___), and destination aggregation areas (__--to-agg.filter _&lt;FILTER&gt;___, __--to-agg.id _&lt;COLUMN_NAME&gt;___, __--to-agg.geom _&lt;COLUMN_NAME&gt;___).

Please note that currently, UrMoAC uses the centroid of a given geometry as the respectively starting / ending position. This means, the geometry of an origin or a destination is arbitrary as long as it can be converted into a point using the PostGIS-function `ST_Centroid`.


## File (.csv) Format

You may load origins / destinations from .csv-files. Here, every origin / destination is stored in one line individually. They should consist of an ID, followed by the x- and the y-coordinate of the respective location&apos;s position or centroid in meters. The following example defines an origin or a destination with the ID 2000, x-position -200 and y-position 0:

```2000;-200;0```

Lines with a leading &lsquo;#&rsquo; and empty lines are omitted.

If the object shall be weighted, the value can be added as an additional field, e.g. (the weight is 5):

```2000;-200;0;5```

Please note that no projection is applied to locations stored in .csv files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;.csv___ and __--to _&lt;MYFILE&gt;.csv___ to load origins or respectively destinations from .csv-files.


## File (.wkt) Format

You may load origins / destinations from .wkt-files. Here, every origin / destination is stored in one line individually. They should consist of an ID, followed by the location&apos;s position or centroid in meters. The following example defines an origin / a destination with the ID 2000, x-position -200 and y-position 0:

```2000;POINT(-200 0)```

If the object shall be weighted, the value can be added as an additional field, e.g. (the weight is 5):

```2000;POINT(-200 0);5```

Lines with a leading &lsquo;#&rsquo; and empty lines are omitted.

Please note that no projection is applied to locations stored in .wkt files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;.wkt;___ and __--to _&lt;MYFILE&gt;.wkt___ to load origins or respectively destinations from .wkt-files.


## SUMO (.poi.xml)

You may load origins / destinations from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).

Please note that currently only __numerical__ shape IDs are accepted.

Please note that no projection is applied to locations stored in [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html) files, thereby you should set __--epsg 0__.

Please note that SUMO shapes do not store a weight.

The file type is recognized by the extension, i.e. use __--from _&lt;MYFILE&gt;_.poi.xml__ and __--to _&lt;MYFILE&gt;_.poi.xml__ to load origins or respectively destinations from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html).

