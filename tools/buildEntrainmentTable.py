import sys, psycopg2


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
  