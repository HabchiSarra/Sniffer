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

This section will explain the whole process of this toolkit, and propose ways of integrating new smells definitions or datasources.

## Detailed process

As far as I can see, this will be quite difficult to integrate with our current workflow, but that could be a fine addition to our modularity capabilities in the long run.

The process can be cut down as follow:
1. The script `projectLooper.sh` will clone all input projects, and start a `commitLooper.sh` with each one as input.
2. The script `commitLooper.sh` will checkout each commit of the input project and call `SmellDetector.jar` with the files as input.
3. The `SmellDetector.jar` will then perform a series of processing to output a `Neo4j` database per application.
    1. Send the source code in our `Spoon` processors to compile it using `JDT`.
    2. Process the generated *AST* to create a `Paprika` model.
    3. Persist the model with as much metadata as a graph in the application's `Neo4j` database.
4. Once the `Neo4j` database is complete, we run `SmellTracker.jar` on each application to fill a `PostgreSQL` database containing valuable data fo all applications.
The `SmellTracker` process is detailed in [this document](./SmellTracker/docs/process.pdf), and can be resumed as follow:
    1. Retrieve the commits data and order from the *Git* repository and the `Neo4j` database.
    2. Retrieve the branch data and order from the *Git* repository. This step enhance the precision of our smell lifecycle detection.
    3. Retrieve the all smells of each type from queries defined in `SmellDetector` and analyze the lifecycle of those smells depending on the detected commits.

## Integrate new smells

### Add a new smell query

The easiest way to integrate a new smell in this process would be to create a new definition in `SmellDetector`.
This means writing a `Neo4j` query relying on the metadata persisted by `SmellDetector`.
It may require to add some new metadata in the persisted model, but won't be much of a hassle to integrate.

This is most likely the easiest way, but the less sustainable if you have lots of already defined smells in your own linter.

**TODO: Add links & details**

### Add smells in the SmellDetector database

If your smell detection are performed before persisting the model and already handled by a visitor pattern,
you may want to add this detection before persisting the model, since spoon is already working with visitor.
It would then be possible to add new metadata in the `SmellDetector` database, to represent the detected smell.


The point is that you will have less work to do to transform you smell definition,
but you would still be required to write a new detection query to look after all the already detected smells.
Those queries should be trivial to write though.

**TODO: Add links & details**

### Add a new datasource

In the long run, we want to be datasource agnostic, and have a `SmellTracker` able to discuss with multiple smell definition sources.
This will require a rework of the toolkit on the `SmellDetector`, and `SmellTracker` to a certain extent.

Due to the fact that `SmellTracker` is querying all smells at once to compute their lifecycle, it will not be possible to stream the information through the
two tools, but we should be able to dot his by integrating multiple code parser and smell detection tools to create a model,
that could be read by `SmellTracker` through an interface defined in `SmellDetector`.

**TODO: Add details**
