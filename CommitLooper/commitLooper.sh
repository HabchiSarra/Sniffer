#!/bin/bash
source ./config.sh

function processCommit {
    projectName="$1"
    commit="$2"
    commitNumber="$3"
    dbPath="`pwd`/../../${DB_DIRECTORY}/${projectName}/databases/graph.db"

    java -Xmx1G -jar "`pwd`/../../${SMELL_DETECTOR_JAR}" \
        analyse `pwd` -db ${dbPath} -n ${projectName} -k ${commit} -cn ${commitNumber}
}

## Args parsing
if [[ -z "$1" ]]; then
    echo "First argument must be the project name!"
    return 1
else
    projectName="$1"
fi

# This argument may be empty, in which case we won't limit the analysis.
startingCommit="$2"


## Script
echo -e "\tCommitLooper version: $VERSION"
echo -e "\tAnalyzing project ${projectName}"
cd "$CLONE_DIRECTORY/${projectName}"
commits=`git log --topo-order --reverse --format=%H`

started=false
index=0
maxIndex=`echo ${commits} | wc -w`
for commit in ${commits} ; do
    index=$((index+1))

    # If we defined a starting commit, we will wait until seeing this
    # commit to start the analysis.
    if [[ -z ${startingCommit} || "${commit}" = "${startingCommit}" ]];then
        started=true
    fi

    if [[ "$started" = true ]];then
        progress=`echo "scale=2; ($index / $maxIndex) * 100" | bc -l`
        echo -e "\t[${projectName}] Analyzing commit ${commit} (${index}/${maxIndex} - ${progress}%)"
        processCommit ${projectName} ${commit} ${index}
    fi
done
