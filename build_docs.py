import os, shutil

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
