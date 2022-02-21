set TEXTTEST_HOME=%~dp0
set URMOAC_BINARY=%~dp0\..\bin\UrMoAC.jar
set POSTGRES_TESTER_BINARY=%~dp0\.\postgres_tester.py
set PYTHON=python

SET TEXTTESTPY=texttest.exe

start %TEXTTESTPY% -a postgres_tester
