# Public Transport output

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
