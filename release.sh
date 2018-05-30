#!/bin/bash

mvn -B -fn clean

mvn -B release:prepare -Prelease -DreleaseVersion=${1} -DdevelopmentVersion=${2} -DgenerateBackupPoms=false

mvn -B release:perform -Prelease

