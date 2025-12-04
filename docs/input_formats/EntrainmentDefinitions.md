# Entrainment Definitions

There are different rules for entraining own mobility options within public transport. E.g., it is allowed to take a bike on board of a city rail (S-Bahn) and metro (U-Bahn) as well as of a tram in Berlin, but not allowed to take it into a bus. The entrainment table contains entrainment possibilities. Each allowed entrainment is defined by the following attributes:

* carrier: the major UrMoAC mode, yet usually &ldquo;pt&rdquo; for public transport;
* carrier_subtype: a __numeric__ ID of the pt carrier type as defined by the &ldquo;route_type&rdquo; attribute stored in &ldquo;[routes.txt](https://gtfs.org/schedule/reference/#routestxt)&rdquo;; Please note that it may differ from the GTFS standard;
* carried: the UrMoAC mode that may be entrained.

The following options are used in combination with this data type:

* __--entrainment _&lt;ENTRAINMENT_SOURCE&gt;___.

Please note that we do not have much experience in using the entrainment table, yet.

## Database Format

The definition of an entrainment possibility consists of the following data in a database:

| Column Name | Type | Purpose | 
| ---- | ---- | ---- |
| carrier | string | The major mode name | 
| carrier_subtype | string | The subtype as defined in GTFS | 
| carried | string | The name of the entrained mode, e.g. &ldquo;bike&rdquo; | 

Use __--entrainment jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___ to load entrainment definitions from a database. See [Origins and Destinations](./OriginsDestinations.md) for an explanation.


## File (.csv) Format

You may load entrainment definitions from .csv-files. Each entrainment definition is stored in a single line individually. The following example defines that a bike may be taken on board of a pt subcarrier 400 (i.e. city rail in Berlin&apos;s GTFS definition):

```pt;400;bike```

Use __--entrainment _&lt;MYFILE&gt;.csv;___ to load the entrainment definitions from a .csv-file.


