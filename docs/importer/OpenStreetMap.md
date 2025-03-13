# Importing OpenStreetMap
We assume you have downloaded the area of your interest as a plain [OpenStreetMap](http://www.openstreetmap.org) XML file (*.osm). Other formats are currently not supported.

In a first step, you have to write this data into a database. This is done using the script &ldquo;[osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py)&rdquo;. Then, you probably want to build a road network from this data. This is done using the script &ldquo;[osmdb_buildWays.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildWays.py)&rdquo;. You may as well extract origins and destinations from your OSM database using the script &ldquo;[osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py)&rdquo;.

## Importing OpenStreetMap into the database
The purpose of the [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py) tool is to store the contents of a given OSM XML file into a PostGIS-enabled PostgreSQL database.

The [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py) tool gets two parameters on the command line:

* The definition of the database that shall be used to store the data and the access information;
* The OSM XML file to get the data from.

Consequently, the call is:

```python osm2db.py <HOST>,<DB>,<SCHEMA>.<PREFIX>,<USER>,<PASSWD> <FILE>```

Where:

* ***&lt;HOST&gt;***: the name of your database server
* ***&lt;DB&gt;***: the name of your database
* ***&lt;SCHEMA&gt;***: the database schema to store the database tables at
* ***&lt;PREFIX&gt;***: a prefix for the database tables
* ***&lt;USER&gt;***: the name of the user who has access (can generate tables and write into them) the database
* ***&lt;PASSWD&gt;***: the password of the user
* ***&lt;FILE&gt;***: the name of the OSM XML file that shall be imported.

The tool reads the OSM nodes, ways, and relations and stores them into generated database tables. The generated database tables are:

* <b><i>&lt;PREFIX&gt;</i>_member</b>: an OSM relation member (rid &mdash; relation id, elemid &mdash; the element id, type &mdash; the element&apos;s type as text, role &mdash; the element&apos;s role as text, idx &mdash; the element&apos;s index)
* <b><i>&lt;PREFIX&gt;</i>_node</b>: an OSM node (id, pos &mdash; the position in WGS84)
* <b><i>&lt;PREFIX&gt;</i>_ntag</b>: a node attribute (id, k &mdash; key as text, v &mdash; value as text)
* <b><i>&lt;PREFIX&gt;</i>_rel</b>: an OSM relation (id)
* <b><i>&lt;PREFIX&gt;</i>_rtag</b>: a relation attribute (id, k &mdash; key as text, v &mdash; value as text)
* <b><i>&lt;PREFIX&gt;</i>_way</b>: an OSM way (id, refs &mdash; list of geometry node ids)
* <b><i>&lt;PREFIX&gt;</i>_wtag</b>: a way attribute (id, k &mdash; key as text, v &mdash; value as text)

## Building the road network from OpenStreetMap data
After inserting the data into the database using [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py), you may extract/build the transport network using this data. This is done using the [osmdb_buildWays.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildWays.py) script. It reads the imported OSM data and builds a database table that matches the road network representation used by UrMoAC. The generated database table is called &ldquo;<b><i>&lt;PREFIX</i>&gt;_network</b>&rdquo;.

The call is:

```python osmdb_buildWays.py <HOST>,<DB>,<SCHEMA>.<PREFIX>,<USER>,<PASSWD>```

Where:

* ***&lt;HOST&gt;***: the name of your database server;
* ***&lt;DB&gt;***: the name of your database;
* ***&lt;SCHEMA&gt;***: the database schema to store the database tables at;
* ***&lt;PREFIX&gt;***: a prefix for the database tables;
* ***&lt;USER&gt;***: the name of the user who has access (can generate tables and write into them) the database;
* ***&lt;PASSWD&gt;***: the password of the user.

## Using OpenStreetMap data to build tables of certain structures
The script [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) builds a database table that can be used to read origins / destinations from by parsing the contents of an OSM representation imported using [osm2db.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osm2db.py).

Some structures can be represented in different ways within [OpenStreetMap](http://www.openstreetmap.org). The script [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) tries to offer a simple way to gather the information about all instances of specific structures and store them into a single table, independent to their original representation within [OpenStreetMap](http://www.openstreetmap.org).

For this purpose, [osmdb_buildStructures.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/osm/osmdb_buildStructures.py) reads a definition file that describes which nodes, ways, or relations, defined by their tags, belong to a specific kind of structures. The format of the definitions file is:

```
[<OSM_DATATYPE>]
<FILTER>
...
<FILTER>
```

The ___&lt;OSM\_DATATYPE&gt;___ is one of &ldquo;node&rdquo;, &ldquo;way&rdquo;, and &ldquo;relation&rdquo;. After this data type definition, the filtering options are given that describe which elements of this data type shall be included. Each line describes a single set and the sets will be merged. A ___&lt;FILTER&gt;___ includes one or more key/value pairs. If more than one is given, they are divided by a &lsquo;&&rsquo;. An element needs to match all of the key/value pairs within a line for being included.

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

The call is:

```python osmdb_buildStructures.py <INPUT_TABLES_PREFIX> <DEF_FILE> <OUTPUT_TABLE>```

Where:

* ***&lt;INPUT_TABLES_PREFIX&gt;***: definition of the database to find the imported OSM data at;
* ***&lt;DEF_FILE&gt;***: path to the definitions file to use;
* ***&lt;OUTPUT_TABLE&gt;***: definition of the database table to generate.

***&lt;INPUT_TABLES_PREFIX&gt;*** is defined as following: ***&lt;HOST&gt;***,***&lt;DB&gt;***,***&lt;SCHEMA&gt;***.***&lt;PREFIX&gt;***,***&lt;USER&gt;***,***&lt;PASSWD&gt;***

Where:

* ***&lt;HOST&gt;***: the name of your database server;
* ***&lt;DB&gt;***: the name of your database;
* ***&lt;SCHEMA&gt;***: the database schema to store the database tables at;
* ***&lt;PREFIX&gt;***: a prefix for the database tables;
* ***&lt;USER&gt;***: the name of the user who has access (can generate tables and write into them) the database;
* ***&lt;PASSWD&gt;***: the password of the user.

***&lt;OUTPUT_TABLE&gt;*** is defined as following:***&lt;HOST&gt;***,***&lt;DB&gt;***,***&lt;SCHEMA&gt;***.***&lt;TABLE&gt;***,***&lt;USER&gt;***,***&lt;PASSWD&gt;***

Where:

* ***&lt;HOST&gt;***: the name of your database server;
* ***&lt;DB&gt;***: the name of your database;
* ***&lt;SCHEMA&gt;***: the database schema to store the database tables at;
* ***&lt;TABLE&gt;***: the name of the table to generate;
* ***&lt;USER&gt;***: the name of the user who has access (can generate tables and write into them) the database;
* ***&lt;PASSWD&gt;***: the password of the user.



