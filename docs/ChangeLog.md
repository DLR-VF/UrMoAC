# Change Log

## UrMoAC-0.X.Y (to come)

### Debugging and improvements

* debugged mapping of public transport halts to edges
* debugged and consolidated reporting of network errors

### Documentation

* corrected docs building
* reorganised docs

### Changes in options and defaults

* option **--keep-subnets** was renamed to **--net.keep-subnets**
* added option **--net.report-all-errors** that forces **UrMoAC** to report all network errors. Otherwise, only the first one of each type is reported. Please note that you may generate a file/database table listing all network errors using the option **--net-errors-output *&lt;OUTPUT&gt;**.
* renamed **--write.subnets *&lt;OUTPUT&gt;*** to **--subnets-output *&lt;OUTPUT&gt;*** (back again)
* added options **--crossing-model *&lt;MODEL&gt;*** for selecting the model of penalties at crossings, including options to set its parameter **--crossing-model.param1 *&lt;DOUBLE&gt;***, and **--crossing-model.param2 *&lt;DOUBLE&gt;***
* renamed **--measure *&lt;ROUTING_MEASURE&gt;*** to **--routing-measure *&lt;ROUTING_MEASURE&gt;*** and according **--measure-param*X* *&lt;DOUBLE&gt;*** to **--routing-measure.param*X* *&lt;DOUBLE&gt;***
* Earlier options for prunning the network and/or the pt-offer were replaced by the following options. Each of those can either get a bounding box (four floats) or the reference to a data source to load the prunning area from.
	* **--from.boundary [_&lt;GEOM_SOURCE&gt;_ | _&lt;BOUNDING_BOX&gt;_]**
	* **--to.boundary [_&lt;GEOM_SOURCE&gt;_ | _&lt;BOUNDING_BOX&gt;_]**
	* **--from-agg.boundary [_&lt;GEOM_SOURCE&gt;_ | _&lt;BOUNDING_BOX&gt;_]**
	* **--to-agg.boundary [_&lt;GEOM_SOURCE&gt;_ | _&lt;BOUNDING_BOX&gt;_]**
	* **--net.boundary [_&lt;GEOM_SOURCE&gt;_ | _&lt;BOUNDING_BOX&gt;_]**
	* **--pt.boundary [_&lt;GEOM_SOURCE&gt;_ | _&lt;BOUNDING_BOX&gt;_]**
* added option **--foot.vmax *&lt;SPEED&gt;*** for changing the default walking speed; *&lt;SPEED&gt;* is given in km/h

### Changes in computation

* previously, intermodality was somehow covered by giving **UrMoAC** a list of modes to use. This has been replaced by distinct mode change points, defined using the option **--mode-changes *&lt;INPUT&gt;***
* an initial model for adding penalties at crossing was added. It can be enabled using the option **--crossing-model *&lt;MODEL&gt;*** where ***&lt;MODEL&gt;*** may be one of "*none*" and "*ctm1*". (Floating point) parameter of the model can be set using the options ***--crossing-model.param1 *&lt;FLOAT&gt;***, and ***--crossing-model.param2 *&lt;FLOAT&gt;***.
* an initial model for reducing speeds (mainly for MIT routing) was added. It can be enabled using the option **--net.vmax-model *&lt;MODEL&gt;*** where ***&lt;MODEL&gt;*** may be one of "*none*" and "*vmm1*"
* Reducing memory footprint and increasing speed
	* The option **--prunning.remove-geometries** will remove the edge geometries after mapping sources/destiations for reducing the memory footprint; You will be warned, when using in combination with **--direct-output** as this is the only output that needs edge geometries
	* The computation uses a less complex algorithm when computing unimodal accessibilities
	* Added the option **--prunning.remove-dead-ends** for removing unused dead ends (ones that do not have an origin / a destination)
* added incline to edges
		
### Continuous Integration 

* added an action for checking whether the current version builds

### Tools
* **osmdb_buildStructures.py**:
    * It is no longer necessary to define all values for nodes, ways, and relations, each. You may use a '*' as the type qualify to set the subsequent extraction patterns for all.
    * added an oid field to the output as nodes/ways/relations in OSM may have overlapping IDs. Together with the type field, one should be able to get the according source.
* all tools: unified options parsing; synopsis was kept, but tools that generate tables now support deleting previously built tables and/or appending data to existing tables



## UrMoAC-0.8.2 (06.08.2024)

[![DOI](https://img.shields.io/badge/doi-10.5281%2Fzenodo.13234444-blue)](https://doi.org/10.5281/zenodo.13234444)

Re-release due to broken build. No further changes in comparison to v0.8.0.



## UrMoAC-0.8.0 (31.07.2024)

[![DOI](https://img.shields.io/badge/doi-10.5281%2Fzenodo.13142701-blue)](https://doi.org/10.5281/zenodo.13142701)

### Changes in options and defaults

* mode names consolidation (you will get a deprecation warning when using old names):
    * "bicycle" is now "bike"
    * "passenger" is now "car"
* output name consolidation:
    * "nm-output" is now "od-output"
    * "ext-nm-output" is now "ext-od-output"
    * "stat-nm-output" is now "stat-od-output"
* replaced ';' as divider by ',' (you will get a deprecation warning when using the old divider):
    * databases definition (input and output)
    * modes to use
    * carrier definition
    * within UrMoAC
    * within included Python-scripts (please note that all fields are now divided using a ',')
* __references to database are now defined on the command line like: *&lt;HOST&gt;*,*&lt;DB&gt;*,*&lt;SCHEMA&gt;*.*&lt;TABLE&gt;*,*&lt;USER&gt;*,*&lt;PASSWD&gt;*__ - schema and table name are divided using a '.', all other fields using a ','
* The default for ID column of database objects is now "id", no longer "gid"
* The default for the geometry column of database objects is now "geom", no longer "the_geom"
    * If you cannot use your old networks, try to add the option --net.geom=the_geom
    * If you cannot use your old origins, try to add the option --from.geom=the_geom
    * If you cannot use your old destinations, try to add the option --to.geom=the_geom

### Debugging and improvements

* patched several documentation issues
* more verbose error handling
* The script "plot_area.py" now supports disaggregated, aggregated, and filled contours visualisations

### Changes in computation

* we moved from edge-based to origin-based computation. This solved some oddities and inexact results, but slowed down the computation. We will try to improve the speed back again, keeping the current quality.



## UrMoAC-0.6.0 (26.05.2023)

[![DOI](https://img.shields.io/badge/doi-10.5281%2Fzenodo.79406006-blue)](https://doi.org/10.5281/zenodo.7940600)

### Channels and links

* changed license to [EPL 2.0](LICENSE.md)
* added [discussions](https://github.com/DLR-VF/UrMoAC/discussions) about the project
* docs are now available at [readthedocs](https://urmoac.readthedocs.io/)


### New Features

* initial file support (all but GTFS)
  * **Attention! option --from-filter was renamed to --from.filter**
  * **Attention! option --to-filter was renamed to --to.filter**
* added --od-connections option which allows to specify a table with explicit origin/destination-tuples to route between
* [issue #17](https://github.com/DLR-VF/UrMoAC/issues/17) added the possibility to save current command line options to a configuration file, to read configuration file
* added the possibility to save an options template
* [issue #20](https://github.com/DLR-VF/UrMoAC/issues/20) added a custom mode
* [issue #28](https://github.com/DLR-VF/UrMoAC/issues/28) the precision of floating point values in output can be changed using the --precision <INT> option (default: 2)
* [issue #38](https://github.com/DLR-VF/UrMoAC/issues/38) mapping of objects to edges is now multithreaded (controlled by the --threads option)
* [issue #10](https://github.com/DLR-VF/UrMoAC/issues/10) [SUMO networks](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html) can now be loaded directly. The file extension must be &ldquo;.net.xml&rdquo;.
* [issue #11](https://github.com/DLR-VF/UrMoAC/issues/11) origins, destinations, and aggregation areas can now be loaded from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html) directly. The file extension is &ldquo;.poi.xml&rdquo;. __Please note that UrMoAC only accepts numerical IDs, at least currently.__
* You may directly load a [SUMO Edge-Based Network State](https://sumo.dlr.de/docs/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures.html) as travel time information.
* [issue #42](https://github.com/DLR-VF/UrMoAC/issues/42) solved: GTFS files can now be loaded directly from disc
* [issue #3](https://github.com/DLR-VF/UrMoAC/issues/31) solved: **the outputs do not include the access/egress distances and travel times from/to the buildings to/from the road; we assume that this makes the outputs more standard compliant; the access/egress distances are still a part of the ext-nm-output**
* finally added at least one [visualisation tool (plot_area.py)](./eval/PlotArea.md)
* added a citation file
* got a DOI (10.5281/zenodo.7940600)


### Debugging and Improvements

* We had a degradation between August 2021 and February 2022 that made public transport routing impossible; it&apos;s patched
* We had a degradation between August 2021 and February 2022 that reinserted user password into db comments; it&apos;s patched, and comments are only generated when **--comment** is set
* [issue #25](https://github.com/DLR-VF/UrMoAC/issues/25) solved: origin weights are not used when no aggregation area is given
* improved (faster) subnets removal
* output
    * [issue #14](https://github.com/DLR-VF/UrMoAC/issues/14) solved: mapping is sorted by edges, then by objects
* improved mapping of objects to the network; it seems like we&apos;ve had a glitch which allocated some locations (&lt;1 % in urban areas) to the wrong edge
* extended handling of input errors, especially when reading files. Now, wrong / broken attributes of loaded artefacts should be reported. Let us know if not (when the application stops with an exception)&hellip;
* [issue #24](https://github.com/DLR-VF/UrMoAC/issues/24) solved: Collecting results when aggregating them is now much faster
* [issue #48](https://github.com/DLR-VF/UrMoAC/issues/48) solved: Better reporting of unconnected networks
    * Option **--subnets** that keeps unconnected sub-networks was renamed to **--keep-subnets**
    * Added the option **--subnets-summary** that lists (very verbose) all found subnets
    * Added the option **--subnets-output *&lt;OUTPUT&gt;*** that writes the found subnets
        * Renamed **--subnets-output *&lt;OUTPUT&gt;*** to **--write.subnets *&lt;OUTPUT&gt;***
* [issue #1](https://github.com/DLR-VF/UrMoAC/issues/1) solved: the tool reports when an edge with a velocity or a length of 0 is loaded, as well when an edge id is used more than once
* removed the **avg_v** field from nm-ext-output as it is too complicated to keep track of connection with dist=0 (same origin/destination) and the value can be easily computed from **avg_tt** and **avg_distance**


### Tools

* reorganised tools, tools are sorted by source type (osm, gtfs, sumo)
* [issue #6](https://github.com/DLR-VF/UrMoAC/issues/6) solved: **Moved to Python 3.x**


## UrMoAC-0.4.0 (03.03.2021)

* added static documentation
