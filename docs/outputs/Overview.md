# Overview

UrMoAC generates the following types of results:

* __--od-output _&lt;OUTPUT&gt;___: (O/D output) a simple output that gives the main measures for each origin(area)/destination(area);
* __--ext-od-output _&lt;OUTPUT&gt;___: (extended O/D output) includes some additional measures, such as the average velocity, consumed energy in kcal, the average price, access and egress times, some additional public transport measures etc.;
* __--stat-od-output _&lt;OUTPUT&gt;___: (O/D statistics output) this output extends the plain O/D output by some statistical measures, such as the mean, median, minimum and maximum values and their 15 percentiles;
* __--pt-output _&lt;OUTPUT&gt;___: (Public Transport output) this output gives some measures concerning public transport trips;
* __--interchanges-output _&lt;OUTPUT&gt;___: (Interchanges output) this output gives some measures about using interchanges;
* __--edges-output _&lt;OUTPUT&gt;___: (Edge Use output) here, the usage of edges in the given network is measured;
* __--direct-output _&lt;OUTPUT&gt;___: (Direct output) for each O/D pair, the complete path including the geometry is given.
* __--origins-to-road-output _&lt;OUTPUT&gt;___: Writes the mapping between origins and the network;
* __--destinations-to-road-output _&lt;OUTPUT&gt;___: Writes the mapping between destinations and the network.

Currently, writing to a [PostgreSQL](https://www.postgresql.org/) database or writing to a file is supported. It is respectively defined as follows:

* when writing to a file, ___&lt;OUTPUT&gt;___ is defined as: ___&lt;FILENAME&gt;.csv___ (the extension must be __&ldquo;.csv&rdquo;__)
* when generating a database table, ___&lt;OUTPUT&gt;___ is defined as: __jdbc:postgresql:_&lt;DB_HOST&gt;_,_&lt;SCHEMA&gt;_._&lt;TABLE&gt;_,_&lt;USER&gt;_,_&lt;PASSWORD&gt;___

Some notes:

* Most of the outputs include the fields __fid__ (ID of the origin) and __sid__ (ID of the destination); in case aggregation areas are loaded, the IDs of the individual origins/destinations are replaced by the ID of the aggregation area they belong to;
* If no aggregation area is given, the fields (avg_*) contain the distinct, unaggregated and not averaged measures between an origin and a destination;
* Some of the outputs are in a mature state and should be very reliable; some specific outputs may be not yet completly verified;
* **Attention!**: some of the outputs are very verbose! You may easily end up with some outputs of several GB when not using them with care!

The outputs are described in-depth in the following.


