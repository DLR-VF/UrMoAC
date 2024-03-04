#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# =============================================================================
# osmdb.py
#
# Author: Daniel Krajzewicz
# Date:   01.04.2018
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2018-2024 Institute of Transport Research,
#                         German Aerospace Center
# All rights reserved.
# =============================================================================
"""This is a very simple tool to demonstrate how an 
entrainment table looks like.

In principle, it is outdated a bot as well as entrainment
tables may as well be given as csv-files."""
# =============================================================================

# --- imported modules --------------------------------------------------------
import sys, psycopg2




# --- functionality ---------------------------------------
conn = psycopg2.connect("dbname='XXX' user='XXX' host='XXX' password='XXX'")
cursor = conn.cursor()
cursor.execute("""CREATE TABLE public.entrainment (
    carrier varchar(40), 
    carrier_subtype smallint, 
    carried varchar(40) 
);""")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',100,'bike')")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',109,'bike')")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',400,'bike')")
cursor.execute("INSERT INTO public.entrainment (carrier,carrier_subtype,carried) VALUES ('pt',1000,'bike')")
conn.commit()       
  