Below, you may find the command line options the tool uses, sorted by their scope.

Please note that some options may be defined using an abbreviation; non-abbreviated option names start with &lsquo;--&rsquo; (two minus signs); abbreviated option names are only one character long and start with &lsquo;-&rsquo; (one minus sign).

## Input Options
### Basic Definitions
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --config _&lt;CONFIGURATION&gt;_<br>-c _&lt;CONFIGURATION&gt;_ | N/A (optional) | Defines the configuration to load. |
| --from _&lt;OBJECT_SOURCE&gt;_<br>-f _&lt;OBJECT_SOURCE&gt;_ | N/A (__mandatory__) | Defines the data source of origins. |
| --to _&lt;OBJECT_SOURCE&gt;_<br>-t _&lt;OBJECT_SOURCE&gt;_ | N/A (__mandatory__) | Defines the data source of destinations. |
| --net _&lt;NET_SOURCE&gt;_<br>-n _&lt;NET_SOURCE&gt;_ | N/A (__mandatory__) | Defines the road network to load. |
| --mode _[&apos;car&apos;, &lsquo;foot&rsquo;, &lsquo;bike&rsquo;]_ | N/A (__mandatory__) | The transport mode to use. |
| --from-agg _&lt;OBJECT_SOURCE&gt;_ | N/A (optional) | Defines the data source of origin aggregation areas. |
| --to-agg _&lt;OBJECT_SOURCE&gt;_ | N/A (optional) | Defines the data source of destination aggregation areas. |
| --pt _&lt;GTFS_TABLES_PREFIX&gt;_<br>-p _&lt;GTFS_TABLES_PREFIX&gt;_ | N/A (optional) | Defines the GTFS-based public transport representation. |
| --traveltimes _&lt;TT_SOURCE&gt;_ | N/A (optional) | Defines the data source of traveltimes. |
| --epsg _&lt;EPSG_NUMBER&gt;_ | N/A (mandatory) | The EPSG projection to use. |
| --time _&lt;TIMES&gt;_ | N/A (mandatory) | The time the trips start at in seconds. |
| --od-connections _&lt;OD_SOURCE&gt;_ | N/A (optional) | When set, O/D-connections to compute are read from the given data source. |

### Input Adaptation
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --from.filter _&lt;FILTER&gt;_<br>-F _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for origins to load. |
| --from.id _&lt;COLUMN_NAME&gt;_ | &ldquo;gid&rdquo; | Defines the column name of the origins&apos; ids. |
| --from.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;the_geom&rdquo; | Defines the column name of the origins&apos; geometries. |
| --to.filter _&lt;FILTER&gt;_<br>-T _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for destinations to load. |
| --to.id _&lt;COLUMN_NAME&gt;_ | &ldquo;gid&rdquo; | Defines the column name of the destinations&apos; ids. |
| --to.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;the_geom&rdquo; | Defines the column name of the destinations&apos; geometries. |
| --from-agg.filter _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for origins aggregations&apos; to load. |
| --from-agg.id _&lt;COLUMN_NAME&gt;_ | &ldquo;gid&rdquo; | Defines the column name of the origins aggregations&apos; ids. |
| --from-agg.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;the_geom&rdquo; | Defines the column name of the origins aggregations&apos; geometries. |
| --to-agg.filter _&lt;FILTER&gt;_ | N/A (optional) | Defines a filter for destination aggregations&apos; to load. |
| --to-agg.id _&lt;COLUMN_NAME&gt;_ | &ldquo;gid&rdquo; | Defines the column name of the destination aggregations&apos; ids. |
| --to-agg.geom _&lt;COLUMN_NAME&gt;_ | &ldquo;the_geom&rdquo; | Defines the column name of the destination aggregations&apos; geometries. |
| --net.vmax _&lt;COLUMN_NAME&gt;_ | &ldquo;vmax&rdquo; | Defines the column name of networks&apos;s vmax attribute. |
| --keep-subnets | N/A (optional) | When set, unconnected network parts are not removed. |

## Weighting Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --weight _&lt;FIELD&gt;_<br>-W _&lt;FIELD&gt;_ | &ldquo;&rdquo; | An optional weighting attribute for the origins. |
| --variable _&lt;FIELD&gt;_<br>-V _&lt;FIELD&gt;_ | &ldquo;&rdquo; | An optional destinations&apos; variable to collect. |

## Routing Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --max-number _&lt;INTEGER&gt;_ | N/A (optional) | The maximum number of destinations to visit. |
| --max-distance _&lt;DOUBLE&gt;_ | N/A (optional) | The maximum distance to check. |
| --max-tt _&lt;DOUBLE&gt;_ | N/A (optional) | The maximum travel time to check. |
| --max-variable-sum _&lt;DOUBLE&gt;_ | N/A (optional) | The maximum sum of variable&apos;s values to collect. |
| --shortest | N/A (optional) | Searches only one destination per origin. |
| --requirespt | N/A (optional) | When set, only information that contains a PT part are stored. |
| --measure [&apos;tt_mode&apos;, &lsquo;price_tt&rsquo;, &lsquo;interchanges_tt&rsquo;, &lsquo;maxinterchanges_tt&rsquo;] | N/A (optional) | The measure to use during the routing. |
| --measure-param1 _&lt;DOUBLE&gt;_ | N/A (optional) | The parameter for the first routing measure&apos;s variable. |
| --measure-param2 _&lt;DOUBLE&gt;_ | N/A (optional) | The parameter for the second routing measure&apos;s variable. |

## Public Transport Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --pt-boundary _&lt;BOUNDARY_SOURCE&gt;_ | N/A (optional) | Defines the data source of the boundary for the PT offer. |
| --date _&lt;DATE&gt;_ | N/A (optional); mandatory when using public transport | The date for which the accessibilities shall be computed. |
| --entrainment _&lt;ENTRAINMENT_SOURCE&gt;_<br>-e _&lt;ENTRAINMENT_SOURCE&gt;_ | N/A (optional) | Data source for entrainment description. |
| --pt-restriction _&lt;CARRIERS&gt;_<br>-P _&lt;CARRIERS&gt;_ | N/A (optional) | A list of carriers that shall be loaded (all are loaded if not given). |

## Custom Mode Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --custom.vmax _&lt;DOUBLE&gt;_ | N/A (optional) | Maximum velocity of the custom mode. |
| --custom.kkc-per-hour _&lt;DOUBLE&gt;_ | N/A (optional) | kkc used per hour when using the custom mode. |
| --custom.co2-per-km _&lt;DOUBLE&gt;_ | N/A (optional) | CO2 emitted per kilometer when using the custom mode. |
| --custom.price-per-km _&lt;DOUBLE&gt;_ | N/A (optional) | Price for using the custom mode per kilometre. |
| --custom.allowed _&lt;MODE&gt;[;_&lt;MODE&gt;_]*_ | N/A (optional) | The type of roads the custom mode can use (combination of &lsquo;foot&rsquo;, &lsquo;bike&rsquo;, &lsquo;car&rsquo; divided by &lsquo;;&rsquo;). |

## Output Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --nm-output _&lt;OUTPUT&gt;_<br>-o _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the n:m output. |
| --ext-od-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the extended n:m output. |
| --stat-od-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the n:m statistics output. |
| --interchanges-output _&lt;OUTPUT&gt;_<br>-i _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the interchanges output. |
| --edges-output _&lt;OUTPUT&gt;_<br>-e _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the edges output. |
| --pt-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the public transport output. |
| --direct-output _&lt;OUTPUT&gt;_<br>-d _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the direct output. |
| --origins-to-road-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines output of the mapping between from-objects to the road. |
| --destinations-to-road-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines output of the mapping between to-objects to the road. |
| --subnets-output _&lt;OUTPUT&gt;_ | N/A (optional) | Defines the output of subnets |
| --dropprevious | N/A (optional) | When set, previous output with the same name is replaced. |
| --precision _&lt;INTEGER&gt;_ | 2 | Defines the number of digits after the decimal point. |
| --comment | N/A (optional) | Adds a comment with the used options into generated output dbs. |

## Process Options
| Option  | Default | Explanation |
| ---- | ---- | ---- |
| --threads _&lt;INTEGER&gt;_ | 1 | The number of threads to use. |
| --verbose<br>-v | N/A (optional) | Prints what is being done. |
| --subnets-summary | N/A (optional) | Prints a summary on found subnets |
| --save-config _&lt;FILENAME&gt;_ | N/A (optional) | Saves the set options as a configuration file. |
| --save-template _&lt;FILENAME&gt;_ | N/A (optional) | Saves a configuration template to add options to. |
| --help<br>-? | N/A (optional) | Prints the help screen. |




