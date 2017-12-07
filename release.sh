#!/bin/bash -xe
rm -rf dist/
mkdir dist

mvn clean package
cp target/gocd-spark-notifier*.jar dist/
mvn clean