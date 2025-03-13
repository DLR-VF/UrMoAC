# Included Data

## Entrainment

* ***&lt;UrMoAC&gt;*/data/berlin_bike_entrainment.csv**: Defines the entrainment possibilities of bikes within the public transport in Berlin and Brandenburg (Berlin uses own carrier definitions)

## OSM Extraction Pattern

The extraction patterns define which OSM-structures shall be extracted, based on keys/values.
Their usage is documented at [Building the road network from OpenStreetMap data](./importer/OpenStreetMap.md#using-openstreetmap-data-to-build-tables-of-certain-structures).

There are some sets of these files, as listed in the following.

The files are located in ***&lt;UrMoAC&gt;*/tools/osm/structure_defs**.

### MyFairShare structures

The OSM data used in the MyFairShare project (see [Benchmarking Cities of 15 Minutes using Open Data and Tools within the MyFairShare Project](./Publications.md#10)) are given in the following table.

| File Name                   | Description                         |
| --------------------------- | ----------------------------------- |
| def_leisure.txt             | Places of leisure activities        |
| def_education.txt           | Places of education activities      |
| def_shopping.txt            | Places of shopping activities       |
| def_errand.txt              | Places of errand activities         |
| def_landuse_commercial.txt  | Commercial areas                    |
| def_landuse_industrial.txt  | Industrial areas                    |
| def_park_ride.txt           | Park&Ride facilities                |
| def_landuse_farmyard.txt    | Farm yards (not used)               |
| def_landuse_allotments.txt  | Allotment areas (not used)          |
| def_landuse_residential.txt | Residential areas (not used)        |
| def_landuse_retail.txt      | Retail areas (not used)             |
| def_landuse.txt             | All land use information (not used) |


## Often used structures

The following structures are used often as origins / destinations - together with the ones given above.

| File Name             | Description            |
| --------------------- | ---------------------- |
| def_buildings.txt     | All buildings          |
| def_kindergarten.txt  | Kindergartens          |
| def_pt_halts.txt      | Public transport halts |
| def_rail_stations.txt | Rails stations         |
| def_school.txt        | Rails stations         |


### Areal structures

The following structures are mainly used for visualisation purposes, see also see [plot_area.py](./eval/PlotArea.md).

| File Name               | Description                                   |
| ----------------------- | --------------------------------------------- |
| def_city_boundaries.txt | City boundaries (type=boundary & place=city)  |
| def_water.txt           | Water areas                                   |


### Other

| File Name   | Description             |
| ----------- | ----------------------- |
| def_all.txt | Extracts all structures |



