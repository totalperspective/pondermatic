#!/bin/sh
rm -rf dist
npx shadow-cljs $@ release portal browser esm npm worker  || exit $?
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

cat >dist/browser/package.json <<!EOF
{
    "type": "module"
}
!EOF

chmod +x ./dist/portal.js
