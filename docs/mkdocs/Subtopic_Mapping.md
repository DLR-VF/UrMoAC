# Mapping objects to the network

## Outline

UrMoAC routes from a set of origins to a set of destinations, bound by variable limits. Most often, origins and destinations are buildings. In reality, they are allocated at roads, with some distance to these. For routing over the road network, we have to determine at which road a building / structure is allocated and at which position along the road. This is what we understand as __&ldquo;mapping objects to the network&rdquo;__.



## Problem description

### Bidirectional roads
In reality, a lane is unidirectional and has a two-dimensional geometry. That&apos;s usually not the case in nowadays digital road network formats, unless you use formats like [openDRIVE](https://www.asam.net/standards/detail/opendrive/) or any other high definition maps.

If both directions of a road are separated, e.g. by tram lines or a grass verge, each direction is represented using a separated edge in common road network formats such as [OpenStreetMap](https://www.openstreetmap.org/). But if you have a small bidirectional road with no separation, both directions are often represented by a single line strip with the additional information that the street goes in both directions. Tools like [SUMO&apos;s](https://www.eclipse.org/sumo/) [netconvert](https://sumo.dlr.de/docs/netconvert.html) apply some geometry changes to pull both directions apart. UrMoAC ignores the fact and keeps both directions having the same geometry.

What we nonetheless want is to allocate the mapped objects &ldquo;at the right side of the road&rdquo;. This is easily done if both directions are separated as in this case, we just have to find the nearest edge. But for bidirectional roads with same geometries for both directions, we have to compute for which of a road direction&apos;s the object is located on the right side.

It should be noted that as UrMoAC looks at both sides of a road when routing, the allocation to the correct side of a road is not really necessary. Though, it does not hurt and is assumed to increase the overall correctness.


### Obstacles
Please note that we currently map the starting point to the next (nearest) road. This is not necessarily true — e.g. a fence may hinder someone to get to the nearest road. But currently, we assume that such information is not available.


### Areal objects
As written, origins and destinations are currently represented by their centroids. With an increasing size - like, e.g., in case of bigger parks - the access to the area's boundaries should be delivered.



## Solutions / realisation

### Determining the nearest edge
Mapping of objects to edges is currently implemented within the class ```NearestEdgeFinder``` located in ```de.dlr.ivf.urmo.router.algorithms.edgemapper```.

Of course, we try to get the tool as fast as possible. Thereby, given an object (origin / destination) to map, we do not compute the distance to all edges the network consists of but use a spatial index to get the nearest ones. We use ```org.locationtech.jts.index.strtree.STRtree``` for this purpose.

Here, we simply use the index to search for the nearest edge, first.


### Using the correct side of bidirectional roads
As soon as we have the nearest edge, we check whether the edge is bidirectional - i.e. where it has an "opposite" edge. The information whether an edge has an opposite edge is computed and set while loading the network.

If no opposite edge exists or if the distance of the origin/destination to the opposite edge is bigger than .1 m than the distances to the initially found edge, the initially found (nearest) edge is used.

Otherwise, the side of the road the origin/destination is allocated at is determined for both directions using the following formula: 

$$
dir=(x_{lend}-x_{lbegin})(y_p-y_{lbegin}) - (y_{lend}-y_{lbegin})(x_p-x_{lbegin})
$$

With:

* \(x_{lbegin}\) and \(y_{lbegin}\) being the x and y position of the begin of the line
* \(x_{lend}\) and \(y_{lend}\) being the x and y position of the end of the line
* \(x_p\) and \(y_p\) being the x and y position of the point

The origin/destination is then assigned to the road at which it lies on the right side in driving/walking direction (\(dir<0\)).


### Assignment to roads
After determining the nearest edge and the position along it, the object is assigned to the edge so that the edge "knows" its objects. For routing purposes, see [Subtopics/Routing](Subtopic_Routing.md), this is done for both sides of a bidirectional roads, of course using the respective position along the edge.

### Beyond edge length
The procedure described above works if the origin/destination lies besides the road - the shortest connection between the road and the origin/destination is a normal of the road's shape at this position. Though it may happen, that the origin/destination lies beyond / in front of the nearest road. In such cases, the origin/destination is mapped to the nearest end of the road.



## See also

* [Subtopics/Networks](Subtopic_Networks.md): notes on networks and their representation
* [Subtopics/Routing](Subtopic_Routing.md): notes on routing


## Further research questions / extensions

* One should test whether supporting the doors of a building would deliver more exact results.
* Are further data about footpaths available?
* The mapping of areal objects, like parks etc. is currently not solved. Different questions and issues arise here:
     * How deep into an area should one got to &ldquo;access&rdquo; it?
     * Determine roads that are within the area or cross its boundaries.
* left-hand networks should be treated otherwise
* The z-coordinate is currently not regarded



## Closing
Please let us know if any information is missing or wrong.




