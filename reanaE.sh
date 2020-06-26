#!/bin/bash

rm *.add
rm -r BSN

for i in $(seq 0 $1)
do
    java -jar reanaE.jar --all-configurations --uml-model=bmBSN$i.xml --feature-model=fmBSN$i.txt
done
