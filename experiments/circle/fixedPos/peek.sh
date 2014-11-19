#!/bin/bash
find -name "nohup.out" -print -exec tail -n1 {} \;
ls -l *.txt
ps aux | grep java
