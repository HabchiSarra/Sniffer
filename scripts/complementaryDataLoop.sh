#!/bin/sh

appsFile="allApps.csv"
outDir=`realpath output`
inputDir=`realpath output` # We scan back every existing output dirs to update metrics
dbDir=`realpath databases`
tmpDir="/tmp/tracker"
mkdir -p $tmpDir

detector="SmellDetector.jar"

##
# Call Detector for the given application
#
# $1 - application name
# $2 - Detector request type
##
function detectorRequest {
    java -Xss512m -jar $detector query -r $2 -db "$dbDir/$1/databases/graph.db" -d true
}

##
# Start the metrics calculation for the given app
#
# $1 - application name
##
function retrieveNbMethodsAndClasses {
    appName=$1
    echo "# Computing methods and classes for project $appName"
    detectorRequest $appName ALLNUMMETHODS
    detectorRequest $appName COUNTVAR
    detectorRequest $appName COUNTINNER
    detectorRequest $appName COUNTASYNC
    detectorRequest $appName COUNTVIEWS

}

##
# Returns 1 if the given word is present in the string,
# returns 0 otherwise.
#
# $1 - The word to analyze
# $2 - The string to check
##
function isWordPresent {
   echo "$2" | grep -E "$1" | wc -l
}

##
# Retrieve commits and their classification regarding actions
# (fix, refactor, feature, ...)
#
# $1 - application name
# $2 - Application path on GitHub
##
function sortCommitByTypes {
    appPath=$2
    gitDir="/tmp/$appName-git"
    logFile=`realpath "$appName-commits"`
    resultFile="$logFile-results"
    echo "project,commit,feat,fix,docs,style,refactor,perf,tests,chores" > $resultFile
    COMMIT_SEPARATOR="أ"

    if [[ ! -d $gitDir ]]; then
        git clone https://github.com/$appPath $gitDir
    fi

    cd $gitDir
    git log --topo-order --reverse --pretty="%H$COMMIT_SEPARATOR%s %B"  > $logFile
    cd -

    currentCommit=""
    currentContent=""
    cat $logFile | while IFS=$COMMIT_SEPARATOR read commit content; do
        if [[ -z $commit && -z $content ]];then
            echo "Skipping empty line"
        else
            if [[ -z $content ]]; then
                # We have to concatenate some body which has been set to commit
                currentContent="$currentContent $commit"
            else
                if [[ ! -z $currentCommit ]]; then
                    # We analyze the currently gathered commit
                    # analyzeCommitLanguage $commit $content
                    feat=`isWordPresent 'feat' $currentContent`
                    fix=`isWordPresent 'fix|debug' $currentContent`
                    docs=`isWordPresent 'docs|documentation' $currentContent`
                    style=`isWordPresent 'style' $currentContent`
                    refactor=`isWordPresent 'refactor' $currentContent`
                    perf=`isWordPresent 'perf' $currentContent`
                    tests=`isWordPresent 'test' $currentContent`
                    chores=`isWordPresent 'chore' $currentContent`

                    # Outputing result
                    echo "$appPath,$currentCommit,$feat,$fix,$docs,$style,$refactor,$perf,$tests,$chores" >> $resultFile
                fi
                # We assign the new line to our new current commit
                echo "Analyzing new commit $commit"
                currentCommit=$commit
                currentContent=$content
            fi
        fi
    done
}

##
# Retrieve methods and classes,
# then sort commits with keywords
#
# $1 - application name
# $2 - Application path on GitHub
##
function compute {
    appName=$1
    appPath=$2

    echo "##### Starting process for $appName ####"
    tmpOutput="$tmpDir/$appName"
    finalOutput="$outDir/$appName"

    mkdir -p $tmpOutput; cd $tmpOutput
    retrieveNbMethodsAndClasses $appName
    sortCommitByTypes $appName $appPath
    cd -

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
            compute $name $path
        fi
    done
else
    # If no input is given we scan every available inputs
    for db in $(ls $inputDir); do
        appName=$(basename $db)
        appPath=$(grep -- $appName $appsFile | cut -d, -f2)
        compute $appName $appPath
    done
fi
