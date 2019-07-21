#!/bin/bash

# First parameter is amount of events, second is sleeping time between events in milliseconds

event_count=1000
throttle=0

if [[ -n "$1" && "$1" -ge 0 ]]
then
event_count=$1
fi

if [[ -n "$2" && "$2" -ge 0 ]]
then
throttle=$2
fi

adb shell monkey -v -v -v --throttle $throttle --pct-syskeys 0 -p ru.spb.speech $event_count