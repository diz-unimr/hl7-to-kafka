#!/usr/bin/env bash

echo -n -e "\x0b$(cat hl7v2-mllp-sample.txt)\x1c\x0d" | nc localhost 2575
