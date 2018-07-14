This repository embed every tool needed to analyse projects using Tandoori.

# Content 

## Tandoori / Harissa (Previously Paprika)

Main tool used for creating databases with application analysis and querying databases content.

## GitMiner / HarissaGitHub

Retrieve data about the GitHub project associated with the android application (e.g. developers, issues, commits, ...).

## MetricsCalculator / HarissaMetrics

Perform metrics calculation about the given project (e.g. the number of introduced smells per commit, per devs, ...)

## Tandoori

New module handling all analysis (currently only commits and smells) from Paprika databases and inserting the results in a `PostgreSQL` or an `sqlite` database. 

## Scripts

Selection of script assembling all the _Tandoori/Harissa_ tools together. See in [the folder](scripts) for more information.


# Build

To build all elements of this repository use the command `./gradlew packages`.