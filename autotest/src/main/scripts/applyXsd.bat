@ECHO OFF
SET OUT_DIR=..\java\com\ctf\autotest\Xml
SET TARGET_DIR=..\java
SET PKG_NAME=com.ctf.autotest.Xml
SET XSD_FILE=..\resources\TestPlan.xsd
@ECHO ON

RMDIR /S /Q %OUT_DIR%
xjc -Xlocator -d %TARGET_DIR% -p %PKG_NAME% -encoding UTF-8 -readOnly %XSD_FILE%
PAUSE
