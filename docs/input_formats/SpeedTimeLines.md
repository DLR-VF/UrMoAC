# Speed Time Lines

The average travel time when driving a vehicle depends on the current situation on the roads. Thereby, it is not sufficient to use the maximum allowed velocity. Instead, one should as well define speed time lines. Each entry in the according dataset defines the speed of a single road for a defined time span. Each entry has the following fields:

* edge ID: The ID of the edge as given in the network description
* begin time: The begin of the time span in seconds
* end time: The end of the time span in seconds
* speed: The speed to set for the given edge within the given time span in km/h

The following options are used in combination with this data type:

* __--traveltimes _&lt;TRAVEL_TIMES&gt;___.

We sometimes use the [edge-based traffic measures](https://sumo.dlr.de/wiki/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures) output from [SUMO](http://sumo.dlr.de) for obtaining the speed time lines of a road network.

## Database Format

A speed time line entry consists of the following data in the database:

| Column Name | Type | Purpose | 
| ---- | ---- | ---- |
| ibegin | float | The begin of the time interval in s | 
| iend | float | The end of the time interval in s | 
| eid | String| The name of the road as defined in the &ldquo;oid&rdquo; field of the road network | 
| speed | float | The average/current/maximum speed at this road | 

Use __--traveltimes jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___ to load speed time lines from a database. See [Origins and Destinations](./OriginsDestinations.md) for an explanation.


## File (.csv) Format

Speed time lines can be read from .csv files. Speed information for a specific time span and edge is stored in a single line. The following example defines a speed restriction for edge 10000 between 7:00am and 8:00am of 20 km/h:

```10000;25200;28800;20```

Use __--traveltimes _&lt;MYFILE&gt;.csv;___ to load speed time lines from a .csv-file.


## SUMO (.dump.xml)

You may directly load a [SUMO Edge-Based Network State](https://sumo.dlr.de/docs/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures.html).

The file type is recognized by the extension, i.e. use __--traveltimes _&lt;MYFILE&gt;_.dump.xml__ to load a [SUMO Edge-Based Network State](https://sumo.dlr.de/docs/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures.html).


