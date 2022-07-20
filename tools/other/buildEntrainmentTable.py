#!/usr/bin/env python
# =========================================================
# buildEntrainmentTable.py
# @author Daniel Krajzewicz
# @date 2018
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Builds an example table that defines entrainment
#
# This is a very simple tool to demonstrate how an 
# entrainment table looks like.
#
# In principle, it is outdated a bot as well as entrainment
# tables may as well be given as csv-files.
# =========================================================


# --- imports ---------------------------------------------
import sys, psycopg2




# --- functionality ---------------------------------------
conn = psycopg2.connect("dbname='XXX' user='XXX' host='XXX' password='XXX'")
cursor = conn.cursor()
cursor.execute("""CREATE TABLE public.entrainment (
    carrier varchar(40), 
    carrier_subtype smallint, 
    carried varchar(40) 
);""")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',100,'bicycle')")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',109,'bicycle')")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',400,'bicycle')")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',1000,'bicycle')")
conn.commit()       
  