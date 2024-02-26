#!/usr/bin/env python
# =========================================================
# postgres_tester.py
# @author Daniel Krajzewicz
# @date 2018-2022
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Executes a test definition for checking 
# interaction with a local Postgres database
#
# The tool is used within the TextTest test system, in
# particular when testing the functionality of using
# a Postgres / PostGIS database.
#
# It parses the given command line options and deletes or
# creates data tables to obtain the desired initial state
# of the database before executing UrMoAC.
#
# UrMoAC is then executed and may write to the database.
#
# The script generates two files collected by the TextTest
# system together with the UrMoAC output. The files are:
# * postgres_dump.txt which includes the contents of the
#   generated table
# * postgres_meta.txt which includes the meta information
#   about the generated table
# =========================================================


# --- imports ---------------------------------------------
import os
import subprocess
import sys
import psycopg2
import re


# --- main method -----------------------------------------
def main(argv):
    # prepare the database first
    conn = psycopg2.connect("dbname='urmoac_postgres_tests' user='urmoactests' host='localhost' password='urmoactests'")
    cursor = conn.cursor()
    action = None
    if "sys:delete" in argv:
        action = "delete"
        argv.remove("sys:delete")
    if "sys:create" in argv:
        action = "create"
        argv.remove("sys:create")
    for j,t in enumerate(argv):
        if j==0:
            # The first argument is the script's name, skip
            continue
        if t.find("jdbc:postgresql:")<0:
            # We are only interested in manipulating the database
            continue
        if t.find("jdbc:postgresql:")>0:
            if t.startswith("db"):
                t = t[3:]
            t = t[t.find("jdbc:postgresql:"):]
        # assert schema exists
        t = re.split(r';|,', t) # https://stackoverflow.com/questions/11050562/how-do-i-split-a-string-in-python-with-multiple-separators
        schema = t[1][:t[1].find(".")]
        cursor.execute("CREATE SCHEMA IF NOT EXISTS %s;" % schema)
        conn.commit()
        # process actions
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
            # The first argument is the script's name, skip
            continue
        # patch/get name
        if t.find("jdbc:postgresql:")<0:
            # We are only interested in reading the database
            continue
        if t.find("jdbc:postgresql:")>0:
            t = t[t.find("jdbc:postgresql:"):]
        t = re.split(r';|,', t) # https://stackoverflow.com/questions/11050562/how-do-i-split-a-string-in-python-with-multiple-separators
        # get values and write them to a file
        cursor.execute("SELECT * FROM %s;" % t[1])
        fdo1.write("%s\n" % t[1])
        for r in cursor.fetchall():
            for i,rv in enumerate(r):
                if i!=0: fdo1.write(";")
                fdo1.write("%s" % rv)
            fdo1.write("\n")
        fdo1.write("----------------------------\n")
        # get comments and write them to a file
        cursor.execute("select obj_description('%s'::regclass);" % t[1])
        fdo2.write("%s\n" % t[1])
        for r in cursor.fetchall():
            for i,rv in enumerate(r):
              if i!=0: fdo2.write(";")
              fdo2.write("%s" % rv)
            fdo2.write("\n")
        fdo2.write("----------------------------\n")
    fdo1.close()
    fdo2.close()
  

# -- main check
if __name__ == '__main__':
  main(sys.argv)
  
