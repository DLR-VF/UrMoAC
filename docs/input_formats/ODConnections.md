# O/D-connections

Usually, UrMoAC routes from all given origins to all defined destinations, bound by the given limits. But UrMoAC may as well route between a given set of explicit origin/destination-tuples. Each origin/destination-relationship is defined by:

* origin ID: The ID of the origin to route from as given in the origins;
* destination ID: The ID of the destination to route to as given in the destinations.

The following options are used in combination with this data type:

* __--od-connections _&lt;OD_CONNECTIONS&gt;___.

## Database Format

An OD-connection is represented using the following attributes in the database:

| Default Column Name | Type | Purpose |
| ---- | ---- | ---- |
| origin | long | ID of the origin as loaded using --from |
| destination | long | ID of the origin as loaded using --to|

Use __--od-connections jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___ to load OD-connections from a database. See [Sources and Destinations](./OriginsDestinations.md) for an explanation.


## File (.csv) Format

You may load OD-connections .csv-files. Each line must contain one origin/destination-tuple. The following example shows a connection between an origin with ID=100 and a destination with ID=200:

```100;200```

Use __--od-connections _&lt;MYFILE&gt;.csv;___ to load OD-connections from a .csv-file.

