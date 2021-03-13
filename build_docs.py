#!/usr/bin/env python
# =========================================================
# build_docs.py
# @author Daniel Krajzewicz
# @date 2021
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Builds the static documentation from github wiki
# =========================================================
import os, shutil

# path to mkdocs
USER_PATH = "c:\\users\\dkrajzew\\appdata\\roaming\\python\\python38\\site-packages\\"

os.chdir("docs")
try:
    shutil.rmtree("UrMoAC.wiki")
except OSError as e:
    print("Error: %s : %s" % ("UrMoAC.wiki", e.strerror))
try:
    shutil.rmtree("site")
except OSError as e:
    print("Error: %s : %s" % ("site", e.strerror))
os.system("git clone https://github.com/DLR-VF/UrMoAC.wiki.git")
os.system("python "+USER_PATH+"mkdocs build")
os.system("python "+USER_PATH+"mkdocs serve")

