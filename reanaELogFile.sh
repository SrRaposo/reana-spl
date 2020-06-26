#!/bin/bash

rm -r AnalysisLog
rm *.add
rm -r BSN

mkdir AnalysisLog

for i in $(seq 0 $1)
do
    java -jar reanaE.jar --all-configurations --uml-model=bmBSN$i.xml --feature-model=fmBSN$i.txt >> AnalysisLog/evolution$i.out
done
