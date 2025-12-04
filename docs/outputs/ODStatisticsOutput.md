# O/D statistics output 

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


