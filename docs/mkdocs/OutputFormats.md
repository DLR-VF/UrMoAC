UrMoAC generates the following types of results:

* __--nm-output _&lt;OUTPUT&gt;___: (O/D output) a simple output that gives the main measures for each origin(area)/destination(area);
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
* when generating a table, ___&lt;OUTPUT&gt;___ is defined as: __jdbc:postgresql:_&lt;DB_HOST&gt;_;_&lt;SCHEMA.TABLE&gt;_;_&lt;USER&gt;_;_&lt;PASSWORD&gt;___

Some notes:

* Most of the outputs include the fields __fid__ (ID of the origin) and __sid__ (ID of the destination); in case aggregation areas are loaded, the IDs of the individual origins/destinations are replaced by the ID of the aggregation area they belong to;
* If no aggregation area is given, the fields (avg_*) contain the distinct, unaggregated and not averaged measures between an origin and a destination;
* Some of the outputs are in a mature state and should be very reliable; some specific outputs may be not yet completly verified;
* **Attention!**: some of the outputs are very verbose! You may easily end up with some outputs of several GB when not using them with care!

The outputs are described in-depth in the following.



## O/D output

This output is enabled using the option __--nm-output _&lt;OUTPUT&gt;___ (__-o _&lt;OUTPUT&gt;___ for short).
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| avg_distance | real | The average distance between the origin(s) and the destination(s) |
| avg_tt | real| The average travel time to get from the origin(s) to the destination(s) |
| avg_num | real| The average number of collected destinations |
| avg_value | real| The average value of the collected destinations |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## extended O/D output

This output is enabled using the option __--ext-od-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| avg_distance | real| The average distance between the origin(s) and the origin(s) |
| avg_tt | real | The average travel time to get from the origin(s) to the origin(s) |
| avg_num | real | The average number of collected destination(s) |
| avg_value | real | The average value of the collected destination(s) |
| avg_kcal | real | The average kilocalories consumed when travelling from the origin(s) to the destination(s) |
| avg_price | real | The average price of travelling from the origin(s) to the destination(s) |
| avg_co2 | real | The average CO2 emissions emitted when travelling from the origin(s) to the destination(s) |
| avg_interchanges | real | The average number of interchanges when travelling from the origin(s) to the destination(s) |
| avg_access | real | The average access time |
| avg_egress | real | The average egress time |
| avg_waiting_time | real | The average waiting time for the next public transport connection |
| avg_init_waiting_time | real | The average initial waiting time for the first public transport connection |
| avg_pt_tt| real | The average time spent in public transport |
| avg_pt_interchange_time | real | The average time needed to change between different public transport lines |
| modes | text | The used modes |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## O/D output statistics

This output is enabled using the option __--stat-od-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| num | bigint | The number of described connections |
| avg_distance | real| The average distance between the origin(s) and the destination(s) |
| avg_tt | real | The average travel time to get from the origin(s) to the destination(s) |
| avg_value | real | The average value of the collected destination(s) |
| avg_kcal | real | The average kilocalories consumed when travelling from the origin(s) to the destination(s) |
| avg_price | real | The average price of travelling from the origin(s) to the destination(s) |
| avg_co2 | real | The average CO2 emissions emitted when travelling from the origin(s) to the destination(s) |
| med_distance | real| The median of the distances between the origin(s) and the destination(s) |
| med_tt | real | The median of the travel times to get from the origin(s) to the destination(s) |
| med_value | real | The median of the values of the collected destination(s) |
| med_kcal | real | The median of the kilocalories consumed when travelling from the origin(s) to the destination(s) |
| med_price | real | The median of the prices of travelling from the origin(s) to the destination(s) |
| med_co2 | real | The median of the CO2 emissions emitted when travelling from the origin(s) to the destination(s) |
| min_distance | real| The minimum of the distances between the origin(s) and the destination(s) |
| min_tt | real | The minimum of the travel times to get from the origin(s) to the destination(s) |
| min_value | real | The minimum of the values of the collected destination(s) |
| min_kcal | real | The minimum of the kilocalories consumed when travelling from the origin(s) to the destination(s) |
| min_price | real | The minimum of the prices of travelling from the origin(s) to the destination(s) |
| min_co2 | real | The minimum of the CO2 emissions emitted when travelling from the origin(s) to the destination(s) |
| max_distance | real| The maximum of the distances between the origin(s) and the destination(s) |
| max_tt | real | The maximum of the travel times to get from the origin(s) to the destination(s) |
| max_value | real | The maximum of the values of the collected destination(s) |
| max_kcal | real | The maximum of the kilocalories consumed when travelling from the origin(s) to the destination(s) |
| max_price | real | The maximum of the prices of travelling from the origin(s) to the destination(s) |
| max_co2 | real | The maximum of the CO2 emissions emitted when travelling from the origin(s) to the destination(s) |
| p15_distance | real| The 15 percentile of the distances between the origin(s) and the destination(s) |
| p15_tt | real | The 15 percentile of the travel times to get from the origin(s) to the destination(s) |
| p15_value | real | The 15 percentile of the values of the collected destination(s) |
| p15_kcal | real | The 15 percentile of the kilocalories consumed when travelling from the origin(s) to the destination(s) |
| p15_price | real | The 15 percentile of the prices of travelling from the origin(s) to the destination(s) |
| p15_co2 | real | The 15 percentile of the CO2 emissions emitted when travelling from the origin(s) to the destination(s) |
| p85_distance | real| The 85 percentile of the distances between the origin(s) and the destination(s) |
| p85_tt | real | The 85 percentile of the travel times to get from the origin(s) to the destination(s) |
| p85_value | real | The 85 percentile of the values of the collected destination(s) |
| p85_kcal | real | The 85 percentile of the kilocalories consumed when travelling from the origin(s) to the destination(s) |
| p85_price | real | The 85 percentile of the prices of travelling from the origin(s) to the destination(s) |
| p85_co2 | real | The 85 percentile of the CO2 emissions emitted when travelling from the origin(s) to the destination(s) |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## Public Transport output

This output is enabled using the option __--pt-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| avg_distance | real | The average distance between the origin(s) and the destinations(s) |
| avg_tt | real | The average travel time between the origin(s) and the destinations(s) |
| avg_access_distance | real | The average access distance between the origin(s) and the first public transport halt(s) |
| avg_access_tt | real | The average access travel time to get from the origin(s) to the first public transport halt(s) |
| avg_egress_distance | real | The average distance from the last public transport halt(s) to the destination(s) |
| avg_egress_tt | real | The average travel time to get from the last public transport halt(s) to the destination(s) |
| avg_interchange_distance | real | The average distance within the interchanges |
| avg_interchange_tt | real | The average travel time within the interchanges |
| avg_pt_distance | real | The average distance spent in public transport when travelling from the origin(s) to the destination(s) |
| avg_pt_tt | real | The average travel time in public transport when travelling from the origin(s) to the destination(s) |
| avg_num_interchanges | real | The average number of interchanges when travelling from the origin(s) to the destination(s) |
| avg_waiting_time | real | The average time spent on waiting for the next public transport carrier when travelling from the origin(s) to the destination(s) |
| avg_init_waiting_time | real | The average time spent on waiting for the first public transport carrier when travelling from the origin(s) to the destination(s) |
| avg_num | real | The average number of collected destination(s) |
| avg_value | real | The average value of the collected destination(s) |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## Interchanges output

This output is enabled using the option __--interchanges-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| halt | text | The name (ID) of the described halt |
| line_from | text | The name of the line that was left at the described interchange |
| line_to | text | The name of the line that was entered at the described interchange |
| num | bigint | The number of interchanges of this type |
| tt | real | The average travel time needed for these interchanges |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## Edge Use output

This output is enabled using the option __--edges-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| eid | text | The id of the described edge |
| num | real | The number of times this edge was used |
| srcweight | real | The sum of the weight(s) of the origin(s)&apos; that used this road  |
| normed | real | The number of times this edge was used normed by the origin(s)&apos; weight(s) |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## Direct output

This output is enabled using the option __--direct-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| edge | text | The ID of this edge |
| line | text | The ID of the line used to pass this edge |
| mode | text | The mode used to pass this edge |
| tt | real | The travel time needed to pass this edge |
| node | text | The ID of the current node |
| idx | text | The index of this element within this route |
| geom | PostGIS LINESTRING | The shape of this edge |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.



## Origins / Destinations Mapping output

These outputs gives the mapping of origin and destination locations to the road network. It is enabled using the option __--origins-to-road-output _&lt;OUTPUT&gt;___ for origins or using __--destinations-to-road-output _&lt;OUTPUT&gt;___ for destinations. It is rather used for debugging.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| gid | bigint | The ID of the origin / destination object |
| rid | text | The name of the road the origin / destination is mapped on |
| rpos | real | The position along the edge the objects is mappend to |
| dist | real | The distance between the object and the edge |
| conn | LINESTRING | A geometrical representation of the connection between the object and the position on the edge it is mapped to |

The geometry always contains of a line between two points &mdash; the origin or respectively the destination position and the position on the network this origin / destination is mapped to.

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;. The geometry is represented as `x1;y1;x2;y2` within the csv output.


