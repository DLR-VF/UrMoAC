# Origins / Destinations Mapping output

These outputs give the mapping of origin and destination locations to the road network. They are enabled using the option __--origins-to-road-output _&lt;OUTPUT&gt;___ for origins or using __--destinations-to-road-output _&lt;OUTPUT&gt;___ for destinations. It is rather used for debugging or visualisation.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| id | bigint | The ID of the origin / destination object |
| rid | text | The name of the road the origin / destination is mapped on |
| rpos | real | The position along the edge the objects is mapped to |
| dist | real | The distance between the object and the edge |
| conn | LINESTRING | A geometrical representation of the connection between the object and the position on the edge it is mapped to |

The geometry always contains a line between two points &mdash; the origin or respectively the destination position and the position on the network this origin / destination is mapped to.

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;. The geometry is represented as `x1;y1;x2;y2` within the csv output.


