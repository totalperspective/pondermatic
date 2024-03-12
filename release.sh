#!/bin/sh
git flow release start $1
./build.sh || exit $?
./deploy.sh || exit $?
git commit -am "Build artefacts"
git flow release finish
git push --all