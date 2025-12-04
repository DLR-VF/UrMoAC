# Road Network

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

We usually use road networks from [OpenStreetMap](http://www.openstreetmap.org). A [tool for importing them](../importer/OpenStreetMap.md) into a database is supported.

## Database Format

A road network is defined by the roads it consists of, and each road is represented using the following attributes in a database:

| Column Name | Type | Purpose | 
| ---- | ---- | ---- |
| oid | String | The name of the road | 
| geom | PostGIS MultiLineString | The shape of the road | 
| nodefrom | long | The ID of the node the road starts at | 
| nodeto | long | The ID of the node the road ends at | 
| mode_walk | boolean | Whether the road can be used by the mode &ldquo;walking&rdquo;/&ldquo;foot&rdquo; | 
| mode_bike | boolean | Whether the road can be used by the mode &ldquo;bicycling&rdquo;/&ldquo;bike&rdquo; | 
| mode_mit | boolean | Whether the road can be used by the mode &ldquo;motorised individual traffic&rdquo;/&ldquo;car&rdquo;/&ldquo;car&rdquo; | 
| vmax | double | The maximum speed allowed on this road in km/h | 
| length | double | The length of this road | 

Use __--net jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___ to load a network from a database. See [Origins and Destinations](./OriginsDestinations.md) for an explanation.


## File (.csv) Format

You may load networks from .csv-files. Within a network .csv-file, every (always unidirectional) road is stored in a single line. 

The following example defines a road with the ID &ldquo;10000&rdquo; connecting nodes 0 and 1, where all modes are allowed. The maximum velocity is 50 km/h, the length is 500 m. The geometry simply spans between the connected nodes.

```10000;0;1;true;true;true;50;500;-250;0;250;0```

The boolean values for &ldquo;foot&rdquo;, &ldquo;bike&rdquo;, and &ldquo;car&rdquo; may be encodes as &ldquo;true&rdquo; or &ldquo;1&rdquo; when the respective mode is allowed, &ldquo;false&rdquo; or &ldquo;0&rdquo; otherwise.

Please note that no projection is applied to networks stored in .csv files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--net _&lt;MYFILE&gt;.csv___ to load a network from a .csv-file.


## File (.wkt) Format

You may load networks from .wkt-files. Within a network .wkt-file, every road is stored in a single line. 

The following example defines a road with the ID &ldquo;10000&rdquo; connecting nodes 0 and 1, where all modes are allowed. The maximum velocity is 50 km/h, the length is 500 m. The geometry simply spans between the connected nodes.

```10000;0;1;true;true;true;50;500;LINESTRING(-250 0, 250 0)```

The boolean values for &ldquo;foot&rdquo;, &ldquo;bike&rdquo;, and &ldquo;car&rdquo; may be encodes as &ldquo;true&rdquo; or &ldquo;1&rdquo; when the respective mode is allowed, &ldquo;false&rdquo; or &ldquo;0&rdquo; otherwise.

Please note that no projection is applied to aggregation areas stored in .wkt files, thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--net _&lt;MYFILE&gt;.wkt;___ to load a network from a .wkt-file.


## SUMO (.net.xml)

You may directly load a [SUMO network](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html).

Please note that no projection is applied to [SUMO networks](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html), thereby you should set __--epsg 0__.

The file type is recognized by the extension, i.e. use __--net _&lt;MYFILE&gt;_.net.xml__ to load a [SUMO networks](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html).


