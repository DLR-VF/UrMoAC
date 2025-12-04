# Importing GTFS

If accessibility measures for public transport shall be computed, UrMoAC requires the representation of the public transport offer within the region in form of [GTFS](https://developers.google.com/transit/gtfs/) data. [GTFS](https://developers.google.com/transit/gtfs/) data comes as text files. For using it with UrMoAC, it has to be imported into the database. The script [importGTFS.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/importGTFS.py) does this.

[importGTFS.py](https://github.com/DLR-VF/UrMoAC/blob/master/tools/importGTFS.py) is called like the other UrMoAC import scripts:
```python importGTFS.py <INPUT_PATH> <HOST>,<DB>,<SCHEMA>.<PREFIX>,<USER>,<PASSWD>```
Where:

* ***&lt;INPUT_PATH&gt;***: the path to the folder the GTFS files are located within;
* ***&lt;HOST&gt;***: the name of your database server;
* ***&lt;DB&gt;***: the name of your database;
* ***&lt;SCHEMA&gt;***: the database schema to store the database tables at;
* ***&lt;PREFIX&gt;***: a prefix for the database tables;
* ***&lt;USER&gt;***: the name of the user who has access (can generate tables and write into them) the database;
* ***&lt;PASSWD&gt;***: the password of the user.




