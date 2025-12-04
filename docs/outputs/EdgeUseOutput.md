# Edge Use output

This output is enabled using the option __--edges-output _&lt;OUTPUT&gt;___.
The generated database table has the following format:

| Column Name | Type | Content |
| ---- | ---- | ---- |
| fid | bigint | The ID of the origin object |
| sid | bigint | The ID of the destination object |
| eid | text | The id of the described edge |
| num | real | The number of times this edge was used |
| origins_weight | real | The sum of the weight(s) of the origin(s)&apos; that used this road  |
| normed | real | The number of times this edge was used normed by the origin(s)&apos; weight(s) |

When writing to a file, these attributes are stored in a single line, separated by &lsquo;;&rsquo;.
