#!/usr/bin/env bash

echo -n -e "\x0b$(cat hl7v2-oru-r01-sample.hl7)\x1c\x0d" | nc localhost 2575
