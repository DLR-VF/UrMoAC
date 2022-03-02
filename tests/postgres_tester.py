import os
import subprocess
import sys
import psycopg2

# use the first argument to build / delete the named tables
action = None
conn = psycopg2.connect("dbname='urmoac_postgres_tests' user='urmoactests' host='localhost' password='urmoactests'")
cursor = conn.cursor()
argv = sys.argv
if "sys:delete" in argv:
  action = "delete"
  argv.remove("sys:delete")
if "sys:create" in argv:
  action = "create"
  argv.remove("sys:create")
for j,t in enumerate(argv):
  if j==0:
    continue
  if t.find("jdbc:postgresql:")<0:
    continue
  if t.find("jdbc:postgresql:")>0:
    t = t[t.find("jdbc:postgresql:"):]
  t = t.split(";")
  # assert schema exists
  schema = t[1][:t[1].find(".")]
  cursor.execute("CREATE SCHEMA IF NOT EXISTS %s;" % schema)
  conn.commit()
  if action=="delete":
    # delete table to have a clean db
    print ("dropping %s;" % t[1])
    cursor.execute("DROP TABLE IF EXISTS %s;" % t[1])
    conn.commit()
  elif action=="create":
    # build a table to check what happens if destination already exists
    print ("creating %s;" % t[1])
    cursor.execute("CREATE TABLE IF NOT EXISTS %s(id int, data text);" % t[1])
    conn.commit()
  
# get path to UrMoAC.jar
path = os.path.dirname(argv[0])
path = os.path.join(path, "..", "bin", "UrMoAC.jar")
call = ["java", "-jar", path]
call.extend(argv[1:])
# execute UrMoAC
print ("Running UrMoAC with %s" % " ".join(argv[1:]))
subprocess.call(call)

# write generated files
print ("Collecting results")
fdo1 = open("postgres_dump.txt", "w")
fdo2 = open("postgres_meta.txt", "w")
for j,t in enumerate(argv):
  if j==0:
    continue
  # patch/get name
  if t.find("jdbc:postgresql:")<0:
    continue
  if t.find("jdbc:postgresql:")>0:
    t = t[t.find("jdbc:postgresql:"):]
  t = t.split(";")
  # get values and write them to a file
  cursor.execute("SELECT * FROM %s;" % t[1])
  fdo1.write("%s\n" % t[1])
  for r in cursor.fetchall():
    for i,rv in enumerate(r):
      if i!=0:
        fdo1.write(";")
      fdo1.write("%s" % rv)
    fdo1.write("\n")
  fdo1.write("----------------------------\n")
  # get comments and write them to a file
  cursor.execute("select obj_description('%s'::regclass);" % t[1])
  fdo2.write("%s\n" % t[1])
  for r in cursor.fetchall():
    for i,rv in enumerate(r):
      if i!=0:
        fdo2.write(";")
      fdo2.write("%s" % rv)
    fdo2.write("\n")
  fdo2.write("----------------------------\n")
fdo1.close()
fdo2.close()
  
  
