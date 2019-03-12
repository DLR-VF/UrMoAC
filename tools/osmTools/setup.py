import sys
from cx_Freeze import setup, Executable

# Dependencies are automatically detected, but it might need fine tuning.
additional_mods = ["matplotlib.backends.backend_tkagg",'FileDialog', "Tkinter", "_graph_validation", 
                   "tkFileDialog", 'numpy.core._methods', 'numpy.lib.format']
includefiles = [(r'C:\Anaconda2\Lib\site-packages\scipy')]
# GUI applications require a different base on Windows (the default is for a
# console application).
base = None
if sys.platform == "win32":
    base = "Win32GUI"

setup(  name = "guifoo",
        version = "0.1",
        description = "My GUI application!",
        options = {"build_exe": {'includes': additional_mods, 'include_files': includefiles}},
        executables = [Executable("osm_preprocessing.py", base=base)])