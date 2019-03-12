#!/bin/bash

source ./config.sh

function gitClean {
    commit="$1"
    git clean -ffdxq && \
        git submodule foreach --recursive git clean -ffdx && \
        git reset --hard ${commit} && \
        git submodule foreach --recursive git reset --hard && \
        git submodule update --init --recursive
}

function processProject {
    name="$1"
    url="$2"
    clonePath="$CLONE_DIRECTORY/$name"

    rm -rf -- "./$clonePath"
    git clone git@github.com:$url "$clonePath"

    # Start the analysis in a new process and continue starting the next apps
    # TODO: Limit the number of processes running in parallel
    ./commitLooper.sh ${name} &
}

## Args parsing
if [[ ! -z "$1" && ! -z "$2" ]]; then
    echo "Two args founds, switching to single app mode"
    name="$1"
    url="$2"
fi

if [[ -z "$1" ]]; then
    appsFile="apps.csv"
else
    appsFile="$1"
fi

## Script
echo "ProjectLooper version: $VERSION"
echo "Using input file $appsFile"
cat ${appsFile} | while IFS=, read name path; do
    if [[ -z ${name} && -z ${path} ]];then
        echo "Skipping empty line"
    else
        processProject $name $path
    fi
done