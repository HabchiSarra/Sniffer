This repository embed every tool needed to analyse projects using the smell Sniffer.

# Content 
## CommitLooper

First Step of the analysis, calling SmellDetector on each commit of the selected applications.

## SmellDetector

Main tool used for creating smell databases with application analysis and querying databases content.

Transformation: `GitHub` -> `Neo4j` -> (`CSV` | `Java stream`)

## GitHubMiner

Retrieve data about the GitHub project associated with the android application (e.g. developers, issues, commits, ...).

## CSVSmellTracker

Perform metrics calculation about the given project (e.g. the number of introduced smells per commit, per devs, ...)

Transformation: `CSV` -> `CSV`

*This project is deprecated as it was underperforming by transforming smells CSV files to metrics CSV files.*

## SmellTracker

New module handling all analysis (currently only commits and smells) from Paprika databases
and inserting the results in a `PostgreSQL` or an `sqlite` database. 

Transformation: `Java stream` -> `PostgreSQL`

## Scripts

Selection of **deprecated** scripts assembling the old tools together.
See in [the folder](scripts) for more information.

# Build

To build all elements of this repository use the command `./gradlew packages`.