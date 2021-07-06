#!/bin/bash

for f in `find . -type f -name '*.scala' -o -name '*.java' -o -name '*.sbt' -o -name '*.xml' | grep src`; do sed -i 's/jsonrpc/automorph/g' "$f" ; done
for f in `find . -type d -name jsonrpc | grep src`; do git mv "$f" "`echo $f | sed 's/jsonrpc/automorph/g'`" ; done

