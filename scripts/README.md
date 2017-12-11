

# metricLoop.sh

Loop over the given applications to query the `devNote.sh` script.

This script will need a valid __github API key__ in the file __./githubKey__

The script will look for databases in a __databases__ dir, final outputs in __output__ 
and write temporary results in __/tmp/tandoori__.

Example usage: `./metricLoop.sh ../allApps.csv`

# devNote.sh

This is the main script of the Tandoori approach, working on only one project at a time 
(see [metricLoop.sh](#metricLoop.sh) to load multiple projects).

It will :

1. Query the smells from our Tandoori database (require __Tandoori.jar__).
1. Retrieve every commits of the current project if __COMMITS.csv__ is absent (require __GitMiner.jar__).
1. Merge the developers identity from the smells into the __COMMITS.csv__ file.
1. Retrieve the project commits in [topological order](https://git-scm.com/docs/git-log#git-log---topo-order) 
(will clone the project & use `git log`).
1. Calculate the metrics from all previous information sources (require __MetricsCalculator.jar__).

Example usage: `./devNote.sh -d "$dbDir/$appName" -p "$appPath" -k "$(cat $githubKeyFile)" -o "$tmpOutput"`

# complementaryDataLoop.sh

This script will loop over all already analyzed projects to add the following data:

- Process the number of method and classes (Using __Tandoori.jar__).
- Sort the project's commits by type (`feat,fix,docs,style,refactor,perf,tests,chores`).

# group.py

This script will take every project from the __output__ folder
 (using __metrics-perDev-perCommit-perSmell.csv__ and __$project-commits-results__ as input)
 and group the tagged commits with their respective number of smell _introduction_, _refactor_, _deletion_, _refactor+deletion_.
 
The result will be written in two new files (__completeTags.csv__ and __completeTags-onlyTagged.csv__).

# packResults.sh

Creates 2 tar archives from the __output__ directory, in the current folder.

- _results_: Full content of the directory.
- _metrics_: Only embed the content of __metrics__ dirs and __commits__ files.

The result will be stored in the __packages__ directory.
