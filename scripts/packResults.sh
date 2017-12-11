#!/bin/sh

#echo "Retrieving results"
#cp -r /run/media/antoine/Maxtor/tandoori-metrics/output/* results/

timestamp=$(date +"%Y-%m-%d_%H-%M-%S")
outDir="packages"
fullTarName="$outDir/results-$timestamp.tgz"
metricsTarName="$outDir/metrics-$timestamp.tgz"

echo "Saving results backup int $fullTarName"
tar caf "$fullTarName" output/*

echo "Creating metrics tarball $metricsTarName"
tar caf "$metricsTarName" output/*/metrics output/*/*commits*
