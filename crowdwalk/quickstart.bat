if not defined CROWDWALK (
set CROWDWALK=%~dp0
)

set JAVA=java
if defined JAVA_HOME (
set JAVA=%JAVA_HOME%\bin\java
)

set JAVAOPT=-Dfile.encoding=UTF-8 -Dprism.allowhidpi=false %JAVA_OPTS%
set JAR=%CROWDWALK%\build\libs\crowdwalk.jar

echo %JAVA% %JAVAOPT% -jar %JAR% %*
"%JAVA%" %JAVAOPT% -jar "%JAR%" %*
