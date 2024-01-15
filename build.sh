#!/bin/sh
npm version patch
TP_PONDERMATIC_VERSION=`npm version | grep pondermatic | cut -d\' -f4`
clojure -T:build clean
clojure -T:build jar :version '"'$TP_PONDERMATIC_VERSION'"'
clojure -T:build install :version '"'$TP_PONDERMATIC_VERSION'"'
rm -rf dist
shadow-cljs release :npm :esm :portal

cat >dist/cjs/package.json <<!EOF
{
    "type": "commonjs"
}
!EOF

cat >dist/mjs/package.json <<!EOF
{
    "type": "module"
}
!EOF

chmod +x ./dist/portal.js
