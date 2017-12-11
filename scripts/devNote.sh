#!/bin/sh
# A POSIX variable

###### This loop script can be used to launch every analysis
#
##!/bin/sh
#
#appsFile="apps.csv"
#outDir="output"
#inputDir="databases"
#githubKeyFile="./githubKey"
#
#for db in $(ls $inputDir); do;
#    appName=$(baseName $db)
#    appPath=$(grep -- $appName $appsFile)
#    echo "# Computing metrics for project $appName - $appPath"
#    ./devNote.sh -d $db -p $appPath -k "$(cat $githubKeyFile)" -o $appName
#done
#######

OPTIND=1         # Reset in case getopts has been used previously in the shell.

# Initialize our own variables:
verbose=0
appDB=""
project=""
outputDir=$(date)
githubAPIKey=""

function show_help {
    echo -e "$(basename $0): Transform the raw application database to developer rating\n
Usage: $(basename $0) [-h] [-v] -d inputDatabase -k apiKey -p username/project -o outputDir
    -h Print this help message
    -v Verbose
    -d application database to use as input
    -p GitHub identifier of the analyzed project
    -k GitHub API key
    -o Output directory.
    " >&2
}

# Parse args
while getopts "h?vo:d:o:p:k:" opt; do
    case "$opt" in
    h|\?)
        show_help
        exit 0
        ;;
    v)  verbose=1
        ;;
    p)  project=$OPTARG
        ;;
    d)  appDB=$(realpath $OPTARG)
        ;;
    k)  githubAPIKey=$OPTARG
        ;;
    o)  outputDir=$(realpath $OPTARG)
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

# Check mandatory arguments
if [ -z $appDB  ]; then
    echo "Missing argument: '-d'"
    show_help
    exit 2;
fi

if [ -z $project ]; then
    echo "Missing argument '-p'"
    show_help
    exit 2;
fi

if [ -z $githubAPIKey ]; then
    echo "Missing argument '-k'"
    show_help
    exit 2;
fi

if [[ -f $outputDir ]]; then
    echo "$outputDir is a file, cannot proceed"
    exit 2
fi

# Parsing done, starting out script
WORKDIR=$(dirname $(realpath -s $0))
GIT_MINER="$WORKDIR/GitMiner.jar"
METRICS_CALC="$WORKDIR/MetricsCalculator.jar"
TANDOORI="$WORKDIR/Tandoori.jar"

##
# Query Tandoori jar to process the smells for a given application.
# We are setting a bigger stacktrace size in this method (512Mo).
#
# $1 - The application database to query
# $2 - The output directory
##
function tandooriQuery {
    appDB="$1"
    smellsDir="$2"
    mkdir -p "$smellsDir"
    [[ $verbose -eq "0" ]] || echo "## Will use database: $appDB, file: $(ls $appDB/..)"
    cd "$smellsDir" # We can't provide an output directory to TANDOORI unfortunately
    java -Xss512m -jar $TANDOORI query -r NONFUZZY -db "$1/databases/graph.db" -d true 
    cd -
}

##
# Retrieve the developpers associated to the given project
# and serialize the results in a database and a CSV.
#
# $1 - The project identifier on GitHub (i.e. 'username/project')
# $2 - The GitHub API key
# $3 - The output directory
##
function findDevelopers {
    apiUrl="https://api.github.com/repos/$1"
    devOutputDir=$3
    mkdir -p "$devOutputDir"
    cd $devOutputDir
    [[ $verbose -eq "0" ]] || echo "## Will contact Github API with url: $apiUrl"
    java -jar $GIT_MINER getCommits -l "$apiUrl" -k $2 -d "$devOutputDir/database"
    cd -
}

##
# Find the developer associated to each commit in smell files 
# and add a row with his ID.
#
# $1 - Directory containing smells files
# $2 - File containing developers output
##
function addDeveloperRow {
    csvFile=$2
    # The header 'key' being found, the "id" element is added on the smells CSV.
    cat $csvFile | while IFS=, read hash name id email; do
        [[ $verbose -eq "0" ]] || echo "## Putting developer $id for commit $hash"
        sed -E -i "s/(.*$hash.*)/\1,$id/" $1/*.csv
    done
}

##
# Analyse the smell results in order to give number of Introduced/Refactored smells per developer
#
# $1 - The directory containing smell results
# $2 - smell summary output file
# $3 - GitMiner output containing project commits
# $4 - GitHub project identifier to set in output
# $5 - Project logs in correct order
# $6 - Project database containing Tandoori analysis
##
function metricsCalculation {
    java -jar $METRICS_CALC -d "$1" -o "$2" -c "$3" -p "$4" -l "$5" -db "$6"
}

##
# Clone the project and sort the commits in topological order
#
# $1 - Project path on GitHub
# $2 - output file
##
function topologicalCommitsOrder {
    project=$1
    logFile=`realpath $2`
    repoPath="/tmp/git/$project"
    if [[ ! -d $repoPath ]]; then
        git clone github.com:$project $repoPath
    fi

    cd $repoPath
    git log --topo-order --reverse --pretty="%H"  > $logFile
    cd -
}

[[ $verbose -eq "0" ]] || echo "## verbose=$verbose, appDB=$appDB, outputDir=$outputDir, project: $project, apiKey=$githubAPIKey, Leftovers: $@"

appDB="$appDB/databases/graph.db"
echo "Using appDB $appDB"

smellsDir="$outputDir/smells"
[[ $verbose -eq "0" ]] || echo "## Temporary results for Tandoori will be in $smellsDir"
echo "# Cleaning previous smells"
rm -rf $smellsDir
echo "# Parsing project database to find smells"
tandooriQuery "$appDB"  "$smellsDir"

devDir="$outputDir/devs"
devFile="$devDir/COMMITS.csv" # TODO: Can we define a custom output file?
[[ $verbose -eq "0" ]] || echo "## Temporary results for devs will be in $devDir"
if [[ -f $devFile ]];then 
    echo "# Developpers already present in $devFile"
else
    echo "# Retrieving project developers profiles on github"
    findDevelopers "$project" "$githubAPIKey" "$devDir"
fi

echo "# Merging developers with smells files"
addDeveloperRow "$smellsDir" "$devFile"

echo "# Creating log file with topological order"
timestamp=$(date +"%Y-%m-%d_%H-%M-%S")
logFile="/tmp/commits-$timestamp"
topologicalCommitsOrder $project $logFile

echo "# Generating global metrics file"
metricsDir="$outputDir/metrics"
metricsCalculation "$smellsDir" "$metricsDir" "$devFile" "$project" "$logFile" "$appDB"

echo "Done; output can be find in $metricsDir"
