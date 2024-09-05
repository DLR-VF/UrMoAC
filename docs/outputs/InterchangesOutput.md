# Interchanges output

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

