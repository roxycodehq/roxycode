#!/bin/bash

if [ "$#" -lt 2 ]; then
    echo "Usage: ./jsmashy.sh <input-dir> <output-file>"
    exit 1
fi

mvn exec:java -pl jsmashy-cli -Dexec.mainClass="org.roxycode.jsmashy.cli.Main" -Dexec.args="$1 $2"
