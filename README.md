# UrMoAC

[![Logo](https://raw.githubusercontent.com/DLR-VF/UrMoAC/main/docs/images/UrMoAC.png)](https://raw.githubusercontent.com/DLR-VF/UrMoAC/main/docs/images/UrMoAC.png)
[![License: EPL2](https://img.shields.io/badge/license-EPL2-green)](https://github.com/DLR-VF/UrMoAC/blob/main/LICENSE.md)
[![DOI](https://img.shields.io/badge/doi-10.5281%2Fzenodo.17814529-blue)](https://doi.org/10.5281/zenodo.17814529)
[![Documentation Status](https://readthedocs.org/projects/urmoac/badge/?version=latest)](https://urmoac.readthedocs.io/en/latest/?badge=latest)
![Build Status](https://github.com/DLR-VF/UrMoAC/actions/workflows/maven_build.yml/badge.svg)

&ldquo;Urban Mobility Accessibility Computer&rdquo; or &ldquo;UrMoAC&rdquo; is a tool for computing accessibility measures, supporting aggregation, variable limits, and intermodal paths. It is a scientific tool.

This version of the documentation describes the current development version. You should use one of the available [releases](https://github.com/DLR-VF/UrMoAC/releases). The according documentation can be found at [readthedocs](http://urmoac.readthedocs.io) or within the release itself.

What the tool basically does is to load a set of origin locations and a set of destination locations as well as a road network and optionally a description of the public transport offer. Then, it iterates over all loaded origins and computes the respective accessibility measure for each of them by routing to all destinations within the defined limit. Optionally, areas by which the origins and destinations shall be aggregated may be loaded.

Some features:

* input is read from databases or files;
* variable origins / destinations;
* variable aggregation options;
* weights for origins and destinations;
* flexible limits for search: max. time, max. distance, max. number, max. seen value, nearest only;
* support for different transport modes, as well as intermodal accessibilities;
* GTFS-based public transport accessibility computation;
* possibility to read time-dependent travel times (for motorised individual traffic);
* penalties at crossings (experimental);
* support for intermodal paths beyond using public transport (experimental);
* support for data preparation and visualisation.

## Documentation

The complete documentation is located at <http://urmoac.readthedocs.io>. It should cover different versions.

When using one of the releases, you should consult the included documentation as the information below describes the current state of the development.

Please consult the section *Links* below for further information sources.

## Installation

**UrMoAC** is written in the [Java](https://www.java.com/) programming language. You need [Java](https://www.java.com/) to run it. The easiest way to install it is to download the .jar-file from the latest [release](https://github.com/DLR-VF/UrMoAC/releases). Further possibilities to run it are given at [Installation](https://urmoac.readthedocs.io/en/latest/Installation.html).

## Usage examples

A most basic call may look as following:

```console
java -jar UrMoAC.jar --from origins.csv --to destinations.csv --net network.csv --od-output nm_output.csv --mode bike --time 0 --epsg 0
```

Which would compute the accessibility of the destinations stored in ```destinations.csv``` starting at the origins stored in ```origins.csv``` along the road network stored in ```network.csv``` for the transport mode bike. Information about the used file formats are given at [Input Data Formats](https://urmoac.readthedocs.io/en/latest/input_formats/OriginsDestinations.html) and subsequent pages.

## License

**UrMoAC** is licensed under the [Eclipse Public License 2.0](LICENSE.md).

**When using it, please cite it as:**

Daniel Krajzewicz, Dirk Heinrichs and Rita Cyganski (2017) [_Intermodal Contour Accessibility Measures Computation Using the 'UrMo Accessibility Computer'_](https://elib.dlr.de/118235/). International Journal On Advances in Systems and Measurements, 10 (3&4), Seiten 111-123. IARIA.

And / or use the DOI: [![DOI](https://img.shields.io/badge/doi-10.5281%2Fzenodo.17814529-blue)](https://doi.org/10.5281/zenodo.17814529) (v0.10.0)

## Support and Contribution

**UrMoAC** is under active development and we are happy about any interaction with users or developers.

## Authors

**UrMoAC** has been developed at the [Institute of Transport Research](http://www.dlr.de/vf) of the [German Aerospace Center](http://www.dlr.de).

Authors:

* [Daniel Krajzewicz](https://github.com/dkrajzew) 
* [Simon Nieland](https://github.com/SimonNieland) 
* [Yannick Voigt](https://github.com/y-voigt)
* Logo by [Madeleine Hüger](mailto:madeleinehueger@gmail.com)

## Links

You may find further information about **UrMoAC** at the following pages:
* a complete documentation is located at <http://urmoac.readthedocs.io>;
* the recent as well as the previous releases can be found at <https://github.com/DLR-VF/UrMoAC/releases>;
* the source code repository is located at <https://github.com/DLR-VF/UrMoAC>;
* the issue tracker is located at <https://github.com/DLR-VF/UrMoAC/issues>;
* you may start a discussion or join an existing one at <https://github.com/DLR-VF/UrMoAC/discussions>.

## Legal

Please find additional legal information at [Legal](https://urmoac.readthedocs.io/en/latest/Legal.html) and subsequent pages.
