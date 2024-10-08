#!/bin/sh
if [ -f .env ]; then
  source .env
fi

if [ -z "$CLOJARS_USERNAME" ]; then
  echo "CLOJARS_USERNAME is not set"
  exit 1
fi
if [ -z "$CLOJARS_PASSWORD" ]; then
  echo "CLOJARS_PASSWORD is not set"
  exit 1
fi
export CLOJARS_USERNAME=$CLOJARS_USERNAME
export CLOJARS_PASSWORD=$CLOJARS_PASSWORD

CURRENT_VERSION=$(cat VERSION)
git flow release start $1 || exit $?
VERSION=$(cat VERSION)

if [ "$CURRENT_VERSION" = "$VERSION" ]; then
  echo "Version is not changed, aborting release"
  git checkout develop
  git flow release delete -f $VERSION
  exit 1
fi
./build.sh $VERSION || exit $?
./deploy.sh || exit $?
git commit -am "Build artefacts"
git flow release finish
git push --all