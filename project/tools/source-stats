#!/usr/bin/env bash
# Display source code lines statistics
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null && pwd)"

cd ${SCRIPT_DIR}/../..

echo -e "\n### Files"
echo "* Main: `find  . -name '*.scala' -type f | grep src/main/ | wc -l`"
echo "* Test: `find  . -name '*.scala' -type f | grep src/test/ | wc -l`"
echo "* Build: `find . \( -name '*.sbt' \) -type f | wc -l`"
echo -e "\n### Lines"
echo "* Main: `find  . -name '*.scala' -type f | grep src/main/ | xargs cat | wc -l`"
echo "* Test: `find  . -name '*.scala' -type f | grep src/test/ | xargs cat | wc -l`"
echo "* Build: `find . \( -name '*.sbt' \) -type f | xargs cat | wc -l`"
echo

