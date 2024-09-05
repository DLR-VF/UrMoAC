# Importing Shapefiles
Importing shapefiles is done using the tools brought by PostgreSQL:

```shp2pgsql -s <SRID> -W <ENCODING> <SHAPEFILE> | psql -h <HOST> -d <DBNAME> -U <USERNAME>```
