#!/usr/bin/env bash

JETTY_CONSOLE_JAR="../fcrepo4/fcrepo-webapp/target/fcrepo-webapp-4.5.1-SNAPSHOT-jetty-console.jar"

mvn clean package
zip -0 -r "$JETTY_CONSOLE_JAR" WEB-INF/lib/fcrepo-connector-annex-1.0-SNAPSHOT.jar
java -jar -Dfcrepo.modeshape.configuration=file:repository-symlink.json "$JETTY_CONSOLE_JAR"
