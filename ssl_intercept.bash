#!/bin/bash

JAVA_HOME=$(/usr/libexec/java_home)
wget -O DOIRootCA2.cer http://sslhelp.doi.net/docs/DOIRootCA2.cer

echo "Password is most likely 'changeit'"

sudo keytool -import -file DOIRootCA2.cer -alias DOIRootCA2 -keystore $JAVA_HOME/jre/lib/security/cacerts


printf 'org.gradle.jvmargs=-Djavax.net.ssl.trustStore=' > gradle.properties
printf $JAVA_HOME >> gradle.properties
printf '/jre/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit' >> gradle.properties
