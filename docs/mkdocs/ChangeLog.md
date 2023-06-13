# Change Log

## UrMoAC-0.8.0 (to come)

### cmd_options branch

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
* added --od-connections option which allows to specifiy a table with explicit origin/destination-tuples to route between
* [issue #17](https://github.com/DLR-VF/UrMoAC/issues/17) added the possibility to save current command line options to a configuration file, to read configuration file
* added the possibility to save an options template
* [issue #20](https://github.com/DLR-VF/UrMoAC/issues/20) added a custom mode
* [issue #28](https://github.com/DLR-VF/UrMoAC/issues/28) the precision of floating point values in output can be changed using the --precision <INT> option (default: 2)
* [issue #38](https://github.com/DLR-VF/UrMoAC/issues/38) mapping of objects to edges is now multithreaded (controlled by the --threads option)
* [issue #10](https://github.com/DLR-VF/UrMoAC/issues/10) [SUMO networks](https://sumo.dlr.de/docs/Networks/SUMO_Road_Networks.html) can now be loaded directly. The file extension must be &ldquo;.net-xml&rdquo;.
* [issue #11](https://github.com/DLR-VF/UrMoAC/issues/11) origins, destinations, and aggregation areas can now be loaded from [SUMO shapefiles](https://sumo.dlr.de/docs/Simulation/Shapes.html) directly. The file extension is &ldquo;.poi.xml&rdquo;. __Please note that UrMoAC only accepts numerical IDs, at least currently.__
* You may directly load a [SUMO Edge-Based Network State](https://sumo.dlr.de/docs/Simulation/Output/Lane-_or_Edge-based_Traffic_Measures.html) as travel time information.
* [issue #42](https://github.com/DLR-VF/UrMoAC/issues/42) solved: GTFS files can now be loaded directly from disc
* [issue #3](https://github.com/DLR-VF/UrMoAC/issues/31) solved: **the outputs do not include the access/egress distaces and traveltimes from/to the buildings to/from the road; we assume that this makes the outputs more standard compliant; the access/egress distances are still a part of the [https://github.com/DLR-VF/UrMoAC/wiki/OutputFormats](nm-ext-output)**
* finally added at least one [visualisation tool](./VisualisationTools.md)
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
* [issue #1](https://github.com/DLR-VF/UrMoAC/issues/1) solved: the tool reports when an edge with a velocity or a length of 0 is loaded, as well when an edge id is used more than once
* removed the **avg_v** field from nm-ext-output as it is too complicated to keep track of connection with dist=0 (same origin/destination) and the value can be easily computed from **avg_tt** and **avg_distance**


### Tools

* reorganised tools, tools are sorted by source type (osm, gtfs, sumo)
* [issue #6](https://github.com/DLR-VF/UrMoAC/issues/6) solved: **Moved to Python 3.x**


## UrMoAC-0.4.0 (03.03.2021)

* added static documentation
