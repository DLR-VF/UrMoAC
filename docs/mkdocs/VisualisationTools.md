UrMoAC includes some scripts for evaluating and visualising the results, based on the outstanding [matplotlib library](https://matplotlib.org/) for scientific visualisations using Python. You may find some [matplotlib](https://matplotlib.org/) examples on the [matplotlib gallery page](https://matplotlib.org/stable/gallery/index).

Below, you will find the descriptions of the tools included in the UrMoAC package.

## plot_area

**plot_area.py** shows the results of an accessibility computation on a map.

It generates figures as the following:

# ![berlin_building2pt.png](./images/berlin_building2pt.png)

**plot_area.py** is a Python script and has to be started on the command line. Its options are given in the next table.

| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --from _&lt;OBJECTS&gt;_<br>-f _&lt;OBJECTS&gt;_ | N/A (mandatory) | Defines the objecs (origins) to load. |
| --from.id _&lt;COLUMN_NAME&gt;_ | &ldquo;gid&rdquo; | Defines the name of the field to read the object ids from. |
| --from.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;the_geom&rdquo; | Defines the name of the field to read the object geometries from. |
| --border _&lt;TABLE&gt;_<br>-b _&lt;TABLE&gt;_ | N/A (mandatory) | Defines the border geometry to load. |
| --measures _&lt;TABLE&gt;_<br>-m _&lt;TABLE&gt;_ | N/A (mandatory) | Defines the measures to load. |
| --index _&lt;MEASURES_INDEX&gt;_<br>-i _&lt;MEASURES_INDEX&gt;_ | 2 (equals avg_tt) | Defines the index of the measure to use. |
| --inner _&lt;TABLE&gt;_ | N/A (optional) | Defines the optionsl inner boundaries. |
| --output _&lt;FILE&gt;_<br>-o _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the name of the graphic to generate. |
| --colmap _&lt;COLORMAP_NAME&gt;_<br>-C _&lt;COLORMAP_NAME&gt;_ | RdYlGn_r | Defines the color map to use. |
| --title _&lt;TITLE&gt;_<br>-t _&lt;TITLE&gt;_ | N/A (optional) | Sets the figure title. |
| --projection _&lt;EPSG_CODE&gt;_<br>-p _&lt;EPSG_CODE&gt;_ | 25833 | Sets the projection EPSG number. |
| --help<br>-h | N/A (optional) | Show a help message and exits. |
| --verbose<br>-v | N/A (optional) | Triggers verbose output. |
| --no-show<br>-S | N/A (optional) | Does not show the figure if set. |


**plot_area.py** is located in &lt;UrMoAC&gt;\tools\visualisation.
