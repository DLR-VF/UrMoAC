# Extended O/D output

This output is enabled using the option __--ext-od-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| avg_distance | real| The average distance between the origin(s) and the destination(s) |
| avg_tt | real | The average travel time to get from the origin(s) to the destination(s) |
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
| avg_pt_tt | real | The average time spent in public transport |
| avg_pt_interchange_time | real | The average time needed to change between different public transport lines |
| modes | text | The used modes |
| beeline_distance | real | The beeline distance between the origin(s) and the destination(s) |
| manhattan_distance | real | The manhattan distance between the origin(s) and the destination(s) |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.





