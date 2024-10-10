# This folder should contain the jar file corresponding to Hexaly.
# Change the parameter -Dfile below to the path of the jar file.
mvn install:install-file -Dfile=YOUR_JAR_FILE_HERE -DgroupId=es.urjc.etsii.grafo -DartifactId=localsolver -Dversion=12.0 -Dpackaging=jar