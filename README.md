This repository embeds all the tools needed to analyse projects using the smell Sniffer.

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

Before performing any operation on the project, initialize the submodules using the command `git submodule init && git submodule update`.

To build all artifacts of this repository use the command `./gradlew packages`.


# Workflow Details

This section will explain the whole process of this toolkit, and propose a few ways of integrating new code smell definitions or datasources.

## Detailed process

The process can be cut down as follows:
1. The script `projectLooper.sh` clones all input projects, and starts the script`commitLooper.sh` with each project as input.
2. For each commit of the input project, `commitLooper.sh` performs a checkout and calls `SmellDetector.jar` on the source code files.
3. The `SmellDetector.jar` analyses the source code of each commit by going through the following steps: 
    1. Send the source code in our `Spoon` processors to generate an *AST* using `JDT`.
    2. Process the generated *AST* to create a `Paprika` model.
    3. Persist the model with as much metadata as a graph in a `Neo4j` database.
The commits of the same project have all their models stored in one database. That is, by the end of this step, we have a `Neo4J` database per project.
4. We run `SmellTracker.jar` on each project database to fill a `PostgreSQL` database containing valuable data fo all applications.
The `SmellTracker` process is detailed in [this document](./SmellTracker/docs/process.pdf), and can be resumed as follows:
    1. Extract the commits data and order from the *Git* repository and the `Neo4j` database.
    2. Retrieve the branch data and order from the *Git* repository. This step assures the precision of our smell history tracking.
    3. Detect code smells by launching queries defined in `SmellDetector` on the `Neo4J` database.
    4. Based on the extracted commits order and the detected code smells, track the history of each code smell instance and store it in the `PostgreSQL` database.

## Integrate new smells

### Add a new code smell query

The easiest way to integrate a new code smell in this process would be to create a new definition in `SmellDetector`.
This means writing a `Neo4j` query relying on the metadata persisted by `SmellDetector`.
It may require to add some new metadata in the persisted model, but will not be much of a hassle to integrate.

This is most likely the easiest way, but the less sustainable if the `Paprika` model does not fit all your needs.

**TODO: Add links & details**

### Add smells in the SmellDetector database

If your smell detection are performed before persisting the model and already handled by a visitor pattern,
you may want to add this detection before persisting the model, since Spoon is already working with visitor.
It would then be possible to add new metadata in the `SmellDetector` database, to represent the detected code smells.


The point is that you will have less work to do to transform your code smell definition.-
However, if you want to track your new code smells you would be required to write a new detection query for them.
Those queries should be trivial to write though.

**TODO: Add links & details**

### Add a new datasource

In the long run, we want to be datasource agnostic, and have a `SmellTracker` able to interact with multiple code smell detectors.
This will require a rework of the toolkit on the `SmellDetector`, and `SmellTracker` to a certain extent.

Due to the fact that `SmellTracker` is querying all smells at once to track their history, it will not be possible to stream the information through the two tools.
However, we should be able to dot his by integrating multiple code parsers and smell detection tools to create a model
that could be read by `SmellTracker` through an interface defined in `SmellDetector`.

**TODO: Add details**
