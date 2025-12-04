# Direct output

This output is enabled using the option __--direct-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| edge | text | The ID of the current edge |
| line | text | The ID of the line used to pass this edge |
| mode | text | The mode used to pass this edge |
| tt | real | The travel time needed to pass this edge |
| node | text | The ID of the current node |
| idx | text | The index of this element within this route |
| geom | PostGIS LINESTRING | The shape of this edge - may be pruned when being the first / last edge |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.

