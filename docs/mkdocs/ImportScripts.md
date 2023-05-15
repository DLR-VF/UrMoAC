Currently, only scripts that import data into a [PostGIS](https://postgis.net/)-enabled SQL-database.

As such, you need a running [PostgreSQL](https://www.postgresql.org/) database with [PostGIS](https://postgis.net/) extensions.


## Importing Shapefiles
Importing shapefiles is done using the tools brought by PostgreSQL:

```shp2pgsql -s <SRID> -W <ENCODING> <SHAPEFILE> | psql -h <HOST> -d <DBNAME> -U <USERNAME>```

## Importing [OpenStreetMap](http://www.openstreetmap.org)
We assume you have downloaded the area of your interest as a plain OSM XML file. Other formats are currently not supported.

In a first step, you have to write this data into a database. This is done using the script &ldquo;[osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py)&rdquo;. Then, you probably want to build a road network from this data. This is done using the script &ldquo;[osmdb_buildWays.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildWays.py)&rdquo;.

### Importing OpenStreetMap into the database
The purpose of the [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py) tool is to store the contents of a given OSM XML file into a PostGIS-enabled PostgreSQL database.

The [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py) tool gets two parameter on the command line:

* The definition of the database and the access to it that shall be used to store the data;
* The OSM XML file to get the data from.

Consequently, the call is:

```python osm2db.py <HOST>;<DB>;<SCHEMA>.<PREFIX>;<USER>;<PASSWD> <FILE>```

Where:

* ___&lt;HOST&gt;___: the name of your database server
* ___&lt;DB&gt;___: the name of your database
* ___&lt;SCHEMA&gt;___: the database schema to store the database tables at
* ___&lt;PREFIX&gt;___: a prefix for the database tables
* ___&lt;USER&gt;___: the name of the user who has access (can generate tables and write into them) the database
* ___&lt;PASSWD&gt;___: the password of the user
* ___&lt;FILE&gt;___: the name of the OSM XML file that shall be imported.

The tool reads the OSM nodes, ways, and relations and stores them into generated database tables. The generated database tables are:

* <b><i>&lt;PREFIX&gt;</i>_member</b>: an OSM relation member (rid &mdash; relation id, elemid &mdash; the element id, type &mdash; the element&apos;s type as text, role &mdash; the element&apos;s role as text, idx &mdash; the element&apos;s index)
* <b><i>&lt;PREFIX&gt;</i>_node</b>: an OSM node (id, pos &mdash; the position in WGS84)
* <b><i>&lt;PREFIX&gt;</i>_ntag</b>: a node attribute (id, k &mdash; key as text, v &mdash; value as text)
* <b><i>&lt;PREFIX&gt;</i>_rel</b>: an OSM relation (id)
* <b><i>&lt;PREFIX&gt;</i>_rtag</b>: a relation attribute (id, k &mdash; key as text, v &mdash; value as text)
* <b><i>&lt;PREFIX&gt;</i>_way</b>: an OSM way (id, refs &mdash; list of geometry node ids)
* <b><i>&lt;PREFIX&gt;</i>_wtag</b>: a way attribute (id, k &mdash; key as text, v &mdash; value as text)

### Building the road network from OpenStreetMap data
After inserting the data into the database using [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py), you may extract/build the transport network using this data. This is done using the [osmdb_buildWays.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildWays.py) script. It reads the imported OSM data and builds a database table that matches the road network representation used by UrMoAC. The generated database table is called &ldquo;<b><i>&lt;PREFIX</i>&gt;_network</b>&rdquo;.

The call is:

```python osmdb_buildWays.py <HOST>;<DB>;<SCHEMA>.<PREFIX>;<USER>;<PASSWD>```

Where:

* ___&lt;HOST&gt;___: the name of your database server;
* ___&lt;DB&gt;___: the name of your database;
* ___&lt;SCHEMA&gt;___: the database schema to store the database tables at;
* ___&lt;PREFIX&gt;___: a prefix for the database tables;
* ___&lt;USER&gt;___: the name of the user who has access (can generate tables and write into them) the database;
* ___&lt;PASSWD&gt;___: the password of the user.

### Using OpenStreetMap data to build tables of certain structures
The script [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) builds a database table that can be used to read origins / destinations from by parsing the contents of an OSM representation imported using [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py).

Some structures can be represented in different ways within [OpenStreetMap](http://www.openstreetmap.org). The script [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) tries to offer a simple way to gather the information about all instances of specific structures and store them into a single table, independent to their original representation within [OpenStreetMap](http://www.openstreetmap.org).

For this purpose, [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) reads a definition file that describes which nodes, ways, or relations, defined by their tags, belong to a specific kind of structures. This is done by reading a definitions file. The format of the definitions file is:

```
[<OSM_DATATYPE>]
<FILTER>
...
<FILTER>
```

The _&lt;OSM_DATATYPE&gt;_ is one of &ldquo;node&rdquo;, &ldquo;way&rdquo;, and &ldquo;relation&rdquo;. After this data type definition, the filtering options are given that describe which elements of this data type shall be included. Each line describes a single set and the sets will be merged. A ___&lt;FILTER&gt;___ includes one or more key/value pairs. If more than one is given, they are divided by a &lsquo;&&rsquo;. An element needs to match all of the key/value pairs within a line for being included.

Some examples:

```
[node]
public_transport=stop_position
```

All [OpenStreetMap](http://www.openstreetmap.org) nodes that have the combination key=&apos;public_transport&apos; and value=&apos;stop_position&apos; &mdash; all public transport stops &mdash; are included.


```
[node]
public_transport=stop_position&bus=yes
```

All [OpenStreetMap](http://www.openstreetmap.org) nodes that have the combination key=&apos;public_transport&apos; and value=&apos;stop_position&apos; and the combination key=&apos;bus&apos; and value=&apos;yes&apos; &mdash; all public transport stops where busses stop &mdash; are included.

Currently, the following definition files are available:

* [def_pt_halts.txt](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/structure_defs/def_pt_halts.txt): imports all public transport halts
* [def_buildings.txt](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/structure_defs/def_buildings.txt): imports all buildings

[osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) is currently under development and experimental.

## Importing [GTFS](https://developers.google.com/transit/gtfs/)

If accessibilities for using public transport shall be computed, UrMoAC requires the representation of the public transport offer within the region in form of [GTFS](https://developers.google.com/transit/gtfs/) data. [GTFS](https://developers.google.com/transit/gtfs/) data comes as text files. For using it with UrMoAC, it has to be imported into the database. The script [importGTFS.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/importGTFS.py) does this.

[importGTFS.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/importGTFS.py) is called like the other UrMoAC import scripts:
```python importGTFS.py <INPUT_PATH> <HOST>;<DB>;<SCHEMA>.<PREFIX>;<USER>;<PASSWD>```
Where:

* ___&lt;INPUT_PATH&gt;___: the path to the folder the GTFS files are located within;
* ___&lt;HOST&gt;___: the name of your database server;
* ___&lt;DB&gt;___: the name of your database;
* ___&lt;SCHEMA&gt;___: the database schema to store the database tables at;
* ___&lt;PREFIX&gt;___: a prefix for the database tables;
* ___&lt;USER&gt;___: the name of the user who has access (can generate tables and write into them) the database;
* ___&lt;PASSWD&gt;___: the password of the user.



