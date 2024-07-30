# Road Networks

## Outline

Road networks are one of the core data used by UrMoAC. There are several aspects of modern road network representations and processing steps performed by UrMoAC that should be discussed. They are given in the following.


## Basic Layout

The road network in UrMoAC is a directed graph with road segments as edges and intersections as nodes.

Road networks in UrMoAC are represented as line strings with the following attributes:

* ID
* starting node ID
* ending node ID
* allowed velocity
* length
* allowance for
    * foot
    * bike
    * car
* geometry (line string)

## Determining the opposite direction

One of UrMoAC's features is the inclusion of destination allocated at opposite side of an accessed road.

One step within for achieving this functionality is the correct determination of the edge that represents the opposite direction, for each edge. Currently, this is done in a post-processing step performed after loading the network.

It turns out that this is not as easy as expected. Only looking at the combination of the nodes an edge starts and ends at and selecting the edge that connects both nodes in the opposite direction is not sufficient. One may, e.g. find a lane with parking lots adjacent to an edge (12.629690,53.500018). Issues may as well occure on edges which start and end at the same node and well, yes, you may simply find duplicate edges in OSM (187925375, 187925376).

In the future, one should think about moving some of these functionalities into the OSM import module - this module inserts two edges with opposite directions into the network representation as soon as it encounters a bi-directional edge. As such, the information about the opposite edge is already available herein.

* Split circular roads more than once to omit the isue of not recognizing edges that connect same nodes

## Loops

## 

## Adding reversed pedestrian edges
Pedestrians are usually allowed to pass edges in both directions. There may be areas where this is prohibited - like access gates, e.g. - but we currently disregard this as we concentrate on plain movement through cities currently.

As discussed, we treat each road to have a single direction. When dealing with roads stored as bidirectional roads in the origin input data, this is unproblematic as two roads would be built, one for each direction, see e.g. [Subtopics/OpenStreetMap](Subtopic_OSM.md). But if a road is given as a unidirectional road in OSM, pedestrians could use it only in one direction what is not realistic. Therefore, UrMoAC adds additional roads that allow only pedestrians and can be traversed in the opposite direction to roads that do not have an assigned opposite road and allow pedestrian traffic.

This is done in the method ```extendDirections``` within the ```de.dlr.ivf.urmo.router.shapes.DBNet``` class.



## See also

* [Subtopics/Routing](Subtopic_Routing.md): notes on routing
* [Subtopics/OpenStreetMap](Subtopic_OSM.md): notes on importing and using OpenStreetMap data



## Further research questions / extensions

* turn restrictions
* one-directional pedestrian paths
* lightning
* pavement
* type of bicycle road
* type of walking path
* green (trees esp.)
* optimisation
    * join edges with same attributes at no-intersection nodes



## Closing
Please let us know if any information is missing or wrong.





