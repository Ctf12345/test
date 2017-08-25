@ECHO OFF
for /f %%i in ('dir /b autotest*.jar') do @set jarfile=%%i
java -jar %jarfile% -f TestPlan.xml -Dass_host="192.168.8.118" -Ddb_ip="192.168.8.99"
@ECHO ON
