#!/usr/bin/env bash

echo -n -e "$(cat hl7v2-mllp-sample.txt)" | nc localhost 8888
