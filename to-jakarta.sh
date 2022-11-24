#!/usr/bin/env bash

# move to jakarta parent
find . -type f -name 'pom.xml' -exec sed -i'' -e 's/smallrye-parent/smallrye-jakarta-parent/g' {} +
# java sources
find . -type f -name '*.java' -exec sed -i'' -e 's/javax./jakarta./g' {} +
# service loader files
find . -path "*/src/main/resources/META-INF/services/javax*" | sed -e 'p;s/javax/jakarta/g' | xargs -n2 git mv
# docs
find documentation -type f -name '*.md' -exec sed -i'' -e 's/javax./jakarta./g' {} +

mvn -ntp build-helper:parse-version versions:set -DnewVersion=3.1.0-SNAPSHOT
find examples -maxdepth 1 -type d | xargs -I{} mvn -ntp -pl {} build-helper:parse-version versions:set -DnewVersion=3.1.0-SNAPSHOT

mvn -ntp versions:update-property -Dproperty=version.eclipse.microprofile.config -DnewVersion=3.0
#https://issues.sonatype.org/browse/MVNCENTRAL-6872
#mvn -ntp versions:update-property -Dproperty=version.jakarta.validation -DnewVersion=3.0
sed -i'' -e 's/validation>2.0.2</validation>3.0.1</g' validator/pom.xml
mvn -ntp versions:update-property -Dproperty=version.hibernate.validator -DnewVersion=7.0.1.Final
mvn -ntp versions:update-property -Dproperty=version.jakarta.el -DnewVersion=4.0
mvn -ntp versions:update-property -Dproperty=version.smallrye.common -DnewVersion=2.0.0
mvn -ntp versions:set-property -Dproperty=version.smallrye.testing -DnewVersion=2.1.0
mvn -ntp versions:update-property -Dproperty=version.jakarta.json -DnewVersion=2.0.1

mvn versions:set-property -Dproperty=tck.skip.owb -DnewVersion=true
