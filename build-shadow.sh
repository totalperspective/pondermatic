#!/bin/sh
rm -rf dist
npx shadow-cljs $@ release :npm :esm :portal :browser

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
