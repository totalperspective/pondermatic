#!/bin/sh
git flow release start $1
./build.sh
./deploy.sh
git commit -am "Build artefacts"
git flow release finish
