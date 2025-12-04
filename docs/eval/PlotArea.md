# plot_area

**plot_area.py** shows the results of an accessibility computation on a map. **plot_area.py** is a Python script and has to be started on the command line. Either the used origins or the origin aggregation areas are colorised or a isolevel view using [matplotlib's](https://matplotlib.org/) contourf-plot is generated.

## Usage

The tool needs at least the information about the origins to load, given using the **--from *&lt;DB_SOURCE&gt;*** (or **-f *&lt;DB_SOURCE&gt;*** for short) option and the measures to load given using the **--measures *&lt;DB_SOURCE&gt;*** (or **-m *&lt;DB_SOURCE&gt;*** for short) option. Both options get a reference to a database table as input, see below. In addition, you may name the column within the measures table from which the values shall be read using **--value *&lt;VALUE_NAME&gt;*** (or **-i *&lt;VALUE_NAME&gt;*** for short), currently defaulting to &ldquo;avg_tt&rdquo; that is included in most outputs. Please note that you **need to define a proper projection** using **--projection *&lt;EPSG_CODE&gt;*** (or **-p *&lt;EPSG_CODE&gt;*** for short). The default projection is 25833 (Berlin).
 
You may adapt the name of the origins' id column using **--from.id *&lt;COLUMN_NAME&gt;*** as well as the name of the origins' geometry column using **--from.geom *&lt;COLUMN_NAME&gt;***. You may as well filter the origin instances to read using the **--from.filter *&lt;WHERE_PARAMETER&gt;*** option.

You may add an outline geometry using **--border *&lt;DB_SOURCE&gt;*** (or **-b *&lt;DB_SOURCE&gt;*** for short). You may as well add an additional inner division, e.g. district boundaries of a city, using **--inner *&lt;DB_SOURCE&gt;***. You may additionally load a road network layer using **--net *&lt;DB_SOURCE&gt;*** (or **-n *&lt;DB_SOURCE&gt;*** for short) as well as a water layer using **--water *&lt;DB_SOURCE&gt;***. You may add a title to the figure using **--title *&lt;TITLE&gt;*** (or **-t *&lt;TITLE&GT;*** for short).

The optional **--bounds *&lt;BOUNDING_BOX&gt;*** option defines the bounding box of the shown area. If not given, the bounding box covering the border geometry defined using the **--border *&lt;DB_SOURCE&gt;*** option will be used. If this is not given as well, the bounding box is computed from the origins' geometries.

You may change the used default colormap 'RdYlGn_r' using the option **--colmap *&lt;COLORMAP_NAME&gt;*** (or **-C *&lt;COLORMAP_NAME&gt;*** for short). The option **--contour** triggers rendering of the area as a isolevel view. You may change the width of the origins' outline using the option **--from.borderwidth *&lt;WIDTH&gt;***.


The file to write the generated figure to is defined using the **--output *&lt;FILE&gt;*** (or **-o *&lt;FILE&gt;*** for short). You may write .png and .svg files. Please consult the [matplotlib](https://matplotlib.org/) documentation for further options.

The option **--verbose** (or **-v** for short) triggers a verbose output. **--help** (or *-h* for short) prints a help screen and quits. If you do not want to see the figure, only save it, you may trigger this using the **--no-show** option (or **-S** for short).


## Examples

It generates figures as the following:

# ![berlin_building2pt.png](./images/berlin_building2pt.png)

## Options

The following table lists the options of **plot_area.py**.

| Option  | Default | Explanation |
| ---- | ---- | ---- |
| **Input options** | | |
| --from _&lt;DB_SOURCE&gt;_<br>-f _&lt;DB_SOURCE&gt;_ | N/A (mandatory) | Defines the objects (origins) to load |
| --measures _&lt;DB_SOURCE&gt;_<br>-m _&lt;DB_SOURCE&gt;_ | N/A (mandatory) | Defines the measures' table to load |
| --value _&lt;VALUE_NAME&gt;_<br>-i _&lt;VALUE_NAME&gt;_ | &ldquo;avg_tt&rdquo; | Defines the name of the value to load from the measures |
| --border _&lt;DB_SOURCE&gt;_<br>-b _&lt;DB_SOURCE&gt;_ | N/A (optional) | Defines the border geometry to load |
| --inner _&lt;DB_SOURCE&gt;_ | N/A (optional) | Defines the optional inner boundaries to load |
| --projection _&lt;EPSG_CODE&gt;_<br>-p _&lt;EPSG_CODE&gt;_ | 25833 | Sets the projection EPSG number |
| --net _&lt;DB_SOURCE&gt;_<br>-n _&lt;DB_SOURCE&gt;_ | N/A (optional) | Defines the optional road network to load |
| --water _&lt;DB_SOURCE&gt;_ | N/A (optional) | Defines the optional water to load |
| **Input adaptation options** | | |
| --from.id _&lt;COLUMN_NAME&gt;_ | &ldquo;id&rdquo; | Defines the name of the field to read the object ids from |
| --from.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;geom&rdquo; | Defines the name of the field to read the object geometries from |
| --from.filter _&lt;WHERE_PARAMETER&gt;_ | N/A (optional) | Defines a SQL WHERE-clause parameter to filter the origins to read |
| --border.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;geom&rdquo; | Defines the name of the field to read the border geometry from |
| **Rendering options** | | |
| --figsize _&lt;WIDTH&gt;_,_&lt;HEIGHT&gt;_<br>-F _&lt;WIDTH&gt;_,_&lt;HEIGHT&gt;_ | 8,5 | Defines figure size |
| --bounds _&lt;BOUNDING_BOX&gt;_ | N/A (optional) | Defines the bounding box |
| --colormap _&lt;COLORMAP_NAME&gt;_<br>-C _&lt;COLORMAP_NAME&gt;_ | RdYlGn_r | Defines the color map to use |
| --invalid _&lt;COLOR&gt;_ | azure | Defines the color to use when data is missing |
| --contour | N/A (optional) | Triggers contour rendering |
| --isochrone | N/A (optional) | Triggers isochrone rendering |
| --title _&lt;TITLE&gt;_<br>-t _&lt;TITLE&gt;_ | N/A (optional) | Sets the figure title |
| --minV _&lt;VALUE&gt;_ | N/A (optional) | Sets the lower value bound |
| --maxV _&lt;VALUE&gt;_ | N/A (optional) | Sets the upper value bound |
| --levels _&lt;FLOAT&gt;_[,_&lt;FLOAT&gt;_]+ | N/A (optional) | Sets the discrete levels |
| --measure-label | N/A (optional) | Sets the colorbar measure label |
| --no-legend | N/A (optional) | If set, no legend will be drawn |
| --from.borderwidth _&lt;WIDTH&gt;_ | 1 | Sets the width of the border of the loaded objects |
| --net.width _&lt;WIDTH&gt;_ | 1 | Sets the width scale of the network |
| **Flow and meta options** | | |
| --output _&lt;FILE&gt;_<br>-o _&lt;FILE&gt;_ | N/A (optional) | Defines the name of the graphic to generate |
| --help<br>-h | N/A (optional) | Show a help message and exits |
| --verbose<br>-v | N/A (optional) | Triggers verbose output |
| --report-all-missing-values | N/A (optional) | Triggers reporting all missing values |
| --no-show<br>-S | N/A (optional) | Does not show the figure if set |


**plot_area.py** is located in &lt;UrMoAC&gt;\tools\visualisation.
