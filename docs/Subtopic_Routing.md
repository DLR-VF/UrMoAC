# Routing

## Outline

Routing is of course the most core part of UrMoAC as we want to find paths between origins and destination along the roads of the road network.

Currently, UrMoAC supports only a Dijkstra-based breadth-first search over the network with some extensions that will be discussed in the following. UrMoAC loops over all origins and starts the search at each origin's position mapped to the road network, see !!!.

The search is edge-based.

Some peculiarities increase the routing's complexity:

* bidirectional routing for accessing destination on the opposite side of the road
* starting and ending at an arbitrary positions along a road
* transport mode changes
* routing along public transport lines

They will be discussed in the following.



## Bidirectional Routing



## See also

* [Subtopics/Networks](Subtopic_Networks.md): notes on networks and their representation


## Further research questions / extensions




## Closing
Please let us know if any information is missing or wrong.
