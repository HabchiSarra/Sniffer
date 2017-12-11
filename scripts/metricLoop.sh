#!/bin/sh

appsFile="allApps.csv"
outDir="output"
inputDir="output" # We scan back every existing output dirs to update metrics
dbDir="databases"
githubKeyFile="./githubKey"
tmpDir="/tmp/tandoori"
mkdir -p $tmpDir

##
# Start the metrics calculation for the given app
#
# $1 - application name
# $2 - application path on GitHub
##
function startDevNote {
    appName=$1
    appPath=$2
    echo "##### Starting process for $appName ####"
    tmpOutput="$tmpDir/$appName"
    finalOutput="$outDir/$appName"
    echo "Re-using metrics and developers to compute new metrics ($finalOutput -> $tmpOutput)"
    cp -r $finalOutput $tmpOutput
    echo "# Computing metrics for project $appName - $appPath"
    ./devNote.sh -d "$dbDir/$appName" -p "$appPath" -k "$(cat $githubKeyFile)" -o "$tmpOutput"
    echo "# Removing previous result and moving new"
    rm -rf $finalOutput
    mv $tmpOutput $finalOutput
}

if [[ -f $1 ]]; then
    # If we have a file as input argument (csv) we read it and do the given applications
    echo "Using input file $1"
    cat $1 | while IFS=, read name path; do
        if [[ -z $name && -z $path ]];then
            echo "Skipping empty line"
        else
            startDevNote $name $path
        fi
    done
else
    # If no input is given we scan every available inputs
    for db in $(ls $inputDir); do
        appName=$(basename $db)
        appPath=$(grep -- $appName $appsFile | cut -d, -f2)
        startDevNote $appName $appPath
    done
fi

./packResults.sh
