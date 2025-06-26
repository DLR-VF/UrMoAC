Frequently Asked Questions
==========================

## Scientific Use

### Which version should I use?

We always encourage you to use the last stable release, because you can easily refer to it as every stable release has an own DOI. Of course, a release may contain bugs or lack features you need.


## Reported Errors

### Why do I get "Duplicate object '0' occurred."?

You should check the ID column of your origins / destination for entries where the ID is not set. For any reasons, this is reported as 0 and counted as being duplicate.


## Computation

### Sometimes UrMoAC seems to be stucked when computing the last value

When using multiple threads and heterogenous distributions of origins and destinations, the longest paths remain being computed while other were already finished. As such, the last one seems to take more time than the others, yet it does not have to be the last one, just the one that takes the most time to be computed.

Though, if multiple origins are located at a baldy connected edge, they will be subsequently computed, yielding indeed in an increased computation time for the last edge.







