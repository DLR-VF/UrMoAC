Below, you may find the command line options the tool uses, sorted by their scope.

Please note that some options may be defined using an abbreviation; non-abbreviated option names start with &lsquo;--&rsquo; (two minus signs); abbreviated option names are only one character long and start with &lsquo;-&rsquo; (one minus sign).

## Input options
### Basic definitions
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --config _&lt;CONFIGURATION&gt;_<br>-c _&lt;CONFIGURATION&gt;_ | N/A (optional) | Defines the configuration to load. |
| --from _&lt;OBJECT_SOURCE&gt;_<br>-f _&lt;OBJECT_SOURCE&gt;_ | N/A (__mandatory__) | Defines the data source of origins. |
| --to _&lt;OBJECT_SOURCE&gt;_<br>-t _&lt;OBJECT_SOURCE&gt;_ | N/A (__mandatory__) | Defines the data source of destinations. |
| --net _&lt;NET_SOURCE&gt;_<br>-n _&lt;NET_SOURCE&gt;_ | N/A (__mandatory__) | Defines the road network to load. |
| --mode _[&apos;car&apos;, &lsquo;foot&rsquo;, &lsquo;bike&rsquo;]_<br>-m _[&apos;car&apos;, &lsquo;foot&rsquo;, &lsquo;bike&rsquo;]_ | N/A (__mandatory__) | The transport mode to use. |
| --from-agg _&lt;OBJECT_SOURCE&gt;_ | N/A (optional) | Defines the data source of origin aggregation areas. |
| --to-agg _&lt;OBJECT_SOURCE&gt;_ | N/A (optional) | Defines the data source of destination aggregation areas. |
| --pt _&lt;GTFS_TABLES_PREFIX&gt;_<br>-p _&lt;GTFS_TABLES_PREFIX&gt;_ | N/A (optional) | Defines the GTFS-based public transport representation. |
| --traveltimes _&lt;TT_SOURCE&gt;_ | N/A (optional) | Defines the data source of traveltimes. |
| --epsg _&lt;EPSG_NUMBER&gt;_ | N/A (__mandatory__) | The EPSG projection to use. |
| --time _&lt;TIMES&gt;_ | N/A (__mandatory__) | The time the trips start at in seconds. |
| --od-connections _&lt;OD_SOURCE&gt;_ | N/A (optional) | When set, O/D-connections to compute are read from the given data source. |
| --mode-changes _&lt;CHANGES_SOURCE&gt;_ | N/A (optional) | Load places where the mode of transport can be changed (no pt). |

### Input adaptation
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --from.filter _&lt;FILTER&gt;_<br>-F _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for origins to load. |
| --from.id _&lt;COLUMN_NAME&gt;_ | &ldquo;id&rdquo; | Defines the column name of the origins&apos; ids. |
| --from.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;geom&rdquo; | Defines the column name of the origins&apos; geometries. |
| --from.boundary _&lt;GEOM_SOURCE&gt;_ or _&lt;BOUNDING_BOX&gt;_ | &ldquo;&rdquo; | Defines a boundary for the origins. |
| --to.filter _&lt;FILTER&gt;_<br>-T _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for destinations to load. |
| --to.id _&lt;COLUMN_NAME&gt;_ | &ldquo;id&rdquo; | Defines the column name of the destinations&apos; ids. |
| --to.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;geom&rdquo; | Defines the column name of the destinations&apos; geometries. |
| --to.boundary _&lt;GEOM_SOURCE&gt;_ or _&lt;BOUNDING_BOX&gt;_ | &ldquo;&rdquo; | Defines a boundary for the destinations. |
| --from-agg.filter _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for origins aggregations&apos; to load. |
| --from-agg.id _&lt;COLUMN_NAME&gt;_ | &ldquo;id&rdquo; | Defines the column name of the origins aggregations&apos; ids. |
| --from-agg.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;geom&rdquo; | Defines the column name of the origins aggregations&apos; geometries. |
| --from-agg.boundary _&lt;GEOM_SOURCE&gt;_ or _&lt;BOUNDING_BOX&gt;_ | &ldquo;&rdquo; | Defines a boundary for the origins aggregation areas. |
| --to-agg.filter _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for destinations aggregation areas to load. |
| --to-agg.id _&lt;COLUMN_NAME&gt;_ | &ldquo;id&rdquo; | Defines the column name of the destinations aggregation areas&apos; ids. |
| --to-agg.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;geom&rdquo; | Defines the column name of the destinations aggregation areas&apos; geometries. |
| --to-agg.boundary _&lt;GEOM_SOURCE&gt;_ or _&lt;BOUNDING_BOX&gt;_ | &ldquo;&rdquo; | Defines a boundary for the destinations aggregation areas. |
| --net.vmax _&lt;COLUMN_NAME&gt;_ | &ldquo;vmax&rdquo; | Defines the column name of networks&apos;s vmax attribute. |
| --net.vmax-model _&lt;MODEL_NAME&gt;_ | &ldquo;none&rdquo; | Defines the model to use for adapting edge speeds  ['none', 'vmm1']. |
| --net.boundary _&lt;GEOM_SOURCE&gt;_ or _&lt;BOUNDING_BOX&gt;_ | &ldquo;&rdquo; | Defines a boundary for the network. |
| --net.keep-subnets | N/A (optional) | When set, unconnected network parts are not removed. |
| --net.patch-errors | N/A (optional) | When set, broken edge lengths and speeds will be patched. |
| --pt.boundary _&lt;GEOM_SOURCE&gt;_ or _&lt;BOUNDING_BOX&gt;_ | &ldquo;&rdquo; | Defines a boundary for the PT offer. |


## O/D Weighting options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --weight _&lt;FIELD&gt;_<br>-W _&lt;FIELD&gt;_ | None/empty | An optional weighting attribute for the origins. |
| --variable _&lt;FIELD&gt;_<br>-V _&lt;FIELD&gt;_ | None/empty | An optional destinations&apos; variable to collect. |

## Routing options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --max-number _&lt;INTEGER&gt;_ | N/A (optional) | The maximum number of destinations to visit. |
| --max-distance _&lt;DOUBLE&gt;_ | N/A (optional) | The maximum distance to check. |
| --max-tt _&lt;DOUBLE&gt;_ | N/A (optional) | The maximum travel time to check. |
| --max-variable-sum _&lt;DOUBLE&gt;_ | N/A (optional) | The maximum sum of variable&apos;s values to collect. |
| --shortest | N/A (optional) | Searches only one destination per origin. |
| --requirespt | N/A (optional) | When set, only information that contains a PT part are stored. |
| --routing-measure [&apos;tt_mode&apos;, &lsquo;price_tt&rsquo;, &lsquo;interchanges_tt&rsquo;, &lsquo;maxinterchanges_tt&rsquo;] | N/A (optional) | The measure to use during the routing. |
| --routing-measure.param1 _&lt;DOUBLE&gt;_ | N/A (optional) | The parameter for the first routing measure&apos;s variable. |
| --routing-measure.param2 _&lt;DOUBLE&gt;_ | N/A (optional) | The parameter for the second routing measure&apos;s variable. |

## Network Simplification Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --prunning.remove-geometries | N/A (optional) | Removes edge geometries. |
| --prunning.remove-dead-ends | N/A (optional) | Removes dead ends with no objects. |

## Public Transport options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --date _&lt;DATE&gt;_ | N/A (optional); __mandatory__ when using public transport | The date for which the accessibilities shall be computed. |
| --entrainment _&lt;ENTRAINMENT_SOURCE&gt;_<br>-E _&lt;ENTRAINMENT_SOURCE&gt;_ | N/A (optional) | Data source for entrainment description. |
| --pt-restriction _&lt;CARRIERS&gt;_ | N/A (optional) | A list of carriers that shall be loaded (all are loaded if not given). |

## Mode options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --foot.vmax _&lt;DOUBLE&gt;_ | 3.6 | Maximum walking velocity (in km/h). |
| --custom.vmax _&lt;DOUBLE&gt;_ | N/A (optional) | Maximum velocity of the custom mode (in km/h). |
| --custom.kkc-per-hour _&lt;DOUBLE&gt;_ | N/A (optional) | kkc used per hour when using the custom mode. |
| --custom.co2-per-km _&lt;DOUBLE&gt;_ | N/A (optional) | CO2 emitted per kilometer when using the custom mode. |
| --custom.price-per-km _&lt;DOUBLE&gt;_ | N/A (optional) | Price for using the custom mode per kilometre. |
| --custom.allowed _&lt;MODE&gt;[,_&lt;MODE&gt;_]*_ | N/A (optional) | The type of roads the custom mode can use (combination of &lsquo;foot&rsquo;, &lsquo;bike&rsquo;, &lsquo;car&rsquo; divided by &lsquo;;&rsquo;). |

## Output options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --od-output _&lt;OUTPUT&gt;_<br>-o _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the n:m output. |
| --ext-od-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the extended n:m output. |
| --stat-od-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the n:m statistics output. |
| --interchanges-output _&lt;OUTPUT&gt;_<br>-i _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the interchanges output. |
| --edges-output _&lt;OUTPUT&gt;_<br>-e _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the edges output. |
| --pt-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the public transport output. |
| --direct-output _&lt;OUTPUT&gt;_<br>-d _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the direct output. |
| --origins-to-road-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines output of the mapping between from-objects to the road. |
| --destinations-to-road-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines output of the mapping between to-objects to the road. |
| --subnets-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the output of subnets. |
| --net-errors-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the output for network errors and warnings. |
| --crossings-output | N/A (optional) | Defines the output for crossing times. |
| --dropprevious | N/A (optional) | When set, previous output with the same name is replaced. |
| --precision _&lt;INTEGER&gt;_ | 2 | Defines the number of digits after the decimal point. |
| --comment | N/A (optional) | Adds a comment with the used options into generated output dbs. |

## Process options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --threads _&lt;INTEGER&gt;_ | 1 | The number of threads to use. |
| --verbose<br>-v | N/A (optional) | Prints what is being done. |
| --net.report-all-errors | N/A (optional) | When set, all errors are printed. |
| --subnets-summary | N/A (optional) | Prints a summary on found subnets |
| --save-config _&lt;FILENAME&gt;_ | N/A (optional) | Saves the set options as a configuration file. |
| --save-template _&lt;FILENAME&gt;_ | N/A (optional) | Saves a configuration template to add options to. |
| --help<br>-? | N/A (optional) | Prints the help screen. |




