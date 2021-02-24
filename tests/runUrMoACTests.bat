set TEXTTEST_HOME=%~dp0
set URMOAC_BINARY=%~dp0\..\bin\UrMoAC.jar
set PYTHON=python

SET TEXTTESTPY=texttest.exe

start %TEXTTESTPY% -a urmoac
