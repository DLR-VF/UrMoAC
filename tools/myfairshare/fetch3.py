import psycopg2, os, shapely
from rtree import index
import shapely.wkt
import shapely.ops

HOST = ""
USER = ""
PASSWORD = ""


conn = psycopg2.connect("dbname='project_myfairshare' user='%s' host='%s' password='%s'" % (USER, HOST, PASSWORD))
cursor = conn.cursor()



livinglabs = [
 "sarpsborg",
 "london",
 "jelgava",
 "berlin",
 "vienna"
]



destinations1 = [ "buildings", "pt_halts", "education", "errand", "leisure", "shopping", "rail_stations", "park_ride", "school", "kindergarten", "landuse", "landuse_allotments", "landuse_commercial", "landuse_farmyard", "landuse_industrial", "landuse_residential", "landuse_retail" ]
destinations2 = [ "landuse_allotments", "landuse_commercial", "landuse_farmyard", "landuse_industrial", "landuse_residential", "landuse_retail" ]


"""
CRS3035RES1000mN1649000E3035000
012345678901234
               5678901234567890
1kmE4402N3331
"""

def getCells(ll, a):
    ret1 = {}
    ret2 = {}
    cell2geom = {}
    cursor.execute("SELECT gid,cellcode,ST_AsText(ST_Transform(geom, 3035)) FROM inspire.%s_%skm" % (ll, a))
    conn.commit()
    for r in cursor.fetchall():
        ret1[int(r[0])] = [r[1]]
        cell2geom[int(r[0])] = shapely.wkt.loads(r[2])
        ret2[r[1]] = int(r[0])
    return ret1, ret2, cell2geom


def getPopulation(cells1, cells2, ll, a):
    cursor.execute("SELECT gid,grd_id,tot_p_2018 FROM population.%s_%skm" % (ll, a))
    conn.commit()
    seen = set()
    for r in cursor.fetchall():
        id = r[1]
        popN = int(r[2])
        eID = "1km" + id[23:28] + id[15:20] 
        if eID in cells2:
            oID = cells2[eID]
            seen.add(oID)
            cells1[oID].append(id)
            cells1[oID].append(int(popN))
    for c in cells1:
        if c not in seen:
            cells1[c].append("na")
            cells1[c].append(0)
            


def getFacilitiesNumber(cells, cell2geom, spatIndex, ll, a, f):
    for c in cells:
        cells[c].append(0)
    cursor.execute("SELECT gid,ST_AsText(ST_Transform(centroid, 3035)) FROM osm_derived.%s20230216_%s;" % (ll, f))
    conn.commit()
    #i = 0
    for r in cursor.fetchall():
        #print (i)
        #i += 1
        fid = int(r[0])
        geom = shapely.wkt.loads(r[1])
        cellIDs = list(spatIndex.intersection(geom.bounds))
        for cellID in cellIDs:
            if cell2geom[cellID].within(geom):
                cells[cellID][-1] = cells[cellID][-1] + 1
                break







def getFacilitiesArea(cells, cell2geom, spatIndex, ll, a, f):
    for c in cells:
        cells[c].append(0)
    cursor.execute("SELECT gid,ST_AsText(ST_Transform(polygon, 3035)) FROM osm_derived.%s20230216_%s;" % (ll, f))
    conn.commit()
    #i = 0
    for r in cursor.fetchall():
        fid = int(r[0])
        geom = shapely.wkt.loads(r[1])
        allparts = [p.buffer(0) for p in list(geom)]
        geom = shapely.ops.unary_union(allparts)
        if geom.is_empty:
            # some landuse entries are points only - skipping them
            continue
        cellIDs = list(spatIndex.intersection(geom.bounds))
        for cellID in cellIDs:
            cells[cellID][-1] = cells[cellID][-1] + cell2geom[cellID].intersection(geom).area


            

for ll in livinglabs:
    print (ll)
    for a in [1]:#, 10]:
        # read cells
        print ("Generating for %s, %s km" % (ll, a))
        cells1, cells2, cell2geom = getCells(ll, a)
        # add population
        print ("Retrieving population")
        getPopulation(cells1, cells2, ll, a)
        # build rtree index
        spatIndex = index.Index()
        for c in cell2geom:
            spatIndex.insert(c, cell2geom[c].bounds)
        # add point facilities
        for f in destinations1:
            print (" numbers of %s" % (f))
            cursor.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='osm_derived' AND table_name='%s20230216_%s');" % (ll, f))
            conn.commit()
            ret = cursor.fetchall()
            if ret[0][0]:
                getFacilitiesNumber(cells1, cell2geom, spatIndex, ll, a, f)
            else:
                print ("  ... table is missing")
                for c in cells1:
                    cells1[c].append(0)
        # add areal facilities
        for f in destinations2:
            print (" areas of %s" % (f))
            cursor.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='osm_derived' AND table_name='%s20230216_%s');" % (ll, f))
            conn.commit()   
            ret = cursor.fetchall()
            if ret[0][0]:
                getFacilitiesArea(cells1, cell2geom, spatIndex, ll, a, f)
            else:
                print ("  ... table is missing")
                for c in cells1:
                    cells1[c].append(0)
        # build destination table
        cursor.execute("DROP TABLE IF EXISTS inspire.%s_%skm_ext2;" % (ll, a))
        conn.commit()
        call = "CREATE TABLE inspire.%s_%skm_ext2 ( gid bigint, cellcode varchar(14), pop_id varchar(31), pop bigint" % (ll, a)
        s = "(%s, %s, %s, %s"
        n = "(gid, cellcode, pop_id, pop"
        for f in destinations1:
            call = call + ", " + f + "_n int"
            s = s + ", %s"
            n = n + ", " + f + "_n"
        for f in destinations2:
            call = call + ", " + f + "_a float"
            s = s + ", %s"
            n = n + ", " + f + "_a"
        call = call + ", n_all int, a_all float"
        s = s + ", %s, %s, ST_GeomFromText(%s, 3035))"
        n = n + ", n_all, a_all, geom)"
        call = call + ");"
        cursor.execute(call)
        conn.commit()
        cursor.execute("SELECT AddGeometryColumn('inspire', '%s_%skm_ext2', 'geom', 3035, 'MULTIPOLYGON', 2);" % (ll, a))
        conn.commit()
        # build values
        entries = []
        # ids&pop: 4; facilities1: 17; facilities2: 6; sums: 2; geom
        for c in cells1:
            l = [c]
            l.extend(cells1[c])
            l.append(sum(cells1[c][4:21]))
            l.append(sum(cells1[c][21:]))
            l.append(cell2geom[c].wkt)
            entries.append(l)
        # store in db
        args = ','.join(cursor.mogrify(s, i).decode('utf-8') for i in entries)
        t = "%s_%skm_ext2" % (ll, a)
        cursor.execute("INSERT INTO inspire.%s%s VALUES " % (t, n) + (args))
        conn.commit()
        # store in file
        fdo = open("%s_%skm.csv" % (ll, a), "w")
        for c in cells1:
            cell = cells1[c]
            #print (cell)
            isSet1 = 0
            fdo.write("%s;%s;%s;%s" % (c, cell[0], cell[1], cell[2]))
            for i,f in enumerate(destinations1):
                fdo.write(";%s" % cell[i+3])
                isSet1 += cell[i+3]
            isSet2 = 0
            for i,f in enumerate(destinations2):
                fdo.write(";%s" % cell[i+3+17])
                isSet2 += cell[i+3+17]
            fdo.write(";%s;%s" % (isSet1, isSet2))
            fdo.write(";%s" % (cell2geom[c].wkt))
            fdo.write("\n")
        fdo.close()        

