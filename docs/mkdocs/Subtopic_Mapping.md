# Mapping objects to the network

## Subtask description

UrMoAC routes from a set of origins to a set of destinations, bound by variable limits. Most often, origins and destinations are buildings. They are allocated at roads, with some distances to these. For routing over the road network, we have to determine at which road a building / structure is allocated. This is what we understand as &ldquo;mapping objects to the network&rdquo;.


## Current Assumptions
The current approach is based on the assumptions that we have:

* a travelling network made of roads represented as line strips (with no width),
* origins / destinations represented as points (the centroids of infrastructure)

As such, we determine the next road that allows to use the major mode of transport and that is the most near one to the currently investigated origin / destination.

This is not necessarily true &mdash; e.g. a fence may hinder someone to get to the nearest road. But currently, we assume that this information is not available.


## Issues and solutions

### The nearest edge
Of course, we try to get the tool as fast as possible. Thereby, given an object (origin / destination) to map, we do not compute the distance to all edges the network consists of but use a spatial index to get the nearest ones.


### Allocating along bidirectional roads
In reality, a lane is unidirectional and has a two-dimensional geometry. That&apos;s usually not the case in digital nowadays road network formats, unless you use formats like [openDRIVE](https://www.asam.net/standards/detail/opendrive/) or any other high definition maps.

If both directions of a road are separated, e.g. by tram lines, or a grass verge, each direction is represented using a separated edge in common road network formats as [OpenStreetMap](https://www.openstreetmap.org/). But if you have a small bidirectional road with no separation, both directions are often represented by a single line strip with the additional information that the street goes in both directions. Tools like [SUMO&apos;s](https://www.eclipse.org/sumo/) [netconvert](https://sumo.dlr.de/docs/netconvert.html) apply some geometry changes to pull both directions apart &mdash; at least for visualisation. We ignore the fact and keep both directions having the same geometry.

What we nonetheless want is to allocate the mapped objects &ldquo;at the right side of the road&rdquo;. This is easily done if both directions are separated as in this case, we just have to find the nearest edge. But for bidirectional roads with same geometries for both directions, we have to compute for which of a road direction&apos;s the object is located on the right side.

One note: as UrMoAC looks at both sides of a road when routing, the allocation to the correct side of a road is not really necessary. Though, it does not hurt and increases the overall correctness.

As we already iterate over the some next roads, see above, we additionally perform the check at which side of the road the object is located at. For this purpose, we iterate over the lines an edge consists of and compute the direction using the following 

minDir = (coord[1].x - coord[0].x) * (p.getY() - coord[0].y) - (p.getX() - coord[0].x) * (coord[1].y - coord[0].y);

### Allocating at the chosen edge

### Besides edge length


## Further Research Questions

* One should test whether supporting the doors of a building would deliver more exact results.
* Are further data about footpaths available?
* The mapping of areal objects, like parks etc. is currently not solved. Different questions and issues arise here:
     * How deep into an area should one got to &ldquo;access&rdquo; it?
     * Determine roads that are within the area or cross its boundaries.





