# O/D output

This output is enabled using the option __--od-output _&lt;OUTPUT&gt;___ (__-o _&lt;OUTPUT&gt;___ for short).
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


