# Tandoori package

This project will use github repositories (self cloned) and Paprika database
to generate a postgres (or sqlite) database containing all smells, developers of each project
 and metrics about them.

# Databases 

## Manage Postgres database

### Configure

You will need a Postgresql database on you host to use this project.
To ensure that you will be able to connect to your postgres database, please use the following:

    $ sudo su - postgres
    $ psql
    postgres=> CREATE DATABASE tandoori;
    > CREATE DATABASE
    postgres=> CREATE USER tandoori WITH PASSWORD 'tandoori';
    > CREATE ROLE
    postgres=> GRANT ALL PRIVILEGES ON DATABASE tandoori to tandoori;
    > GRANT


### Dump, clean and restore

During the insertion process, Tandoori will create a schema named 'tandoori'.

1. You can then dump it using: `pg_dump --schema tandoori tandoori > my_dump.sql`...
2. ...And restore it whenever you want using `psql postgres://localhost:5432/tandoori -U tandoori < my_dump.sql`
3. You can also remove all data by deleting the tandoori schema: `tandoori=> DROP SCHEMA tandoori CASCADE;`

# Usage

If you built a complete jar using `./gradlew shadowJar`, you will be able to perform both
single app analysis and multi apps parallel analysis.

```
    # Analyzing a single application from a local repository
    java -jar TandooriApproach.jar singleAnalysis -n packlist -r ./repositories/packlist -db paprika_db/packlist/databases/graph.db -u nbossard/packlist

    # If your given repository path is not found on the file system,
    # Tandoori will look for it on github (i.e. the following command will try to clone git@github.com:nbossard/packlist)
    java -jar TandooriApproach.jar singleAnalysis -n packlist -r nbossard/packlist -db paprika_db/packlist/databases/graph.dbé -u nbossard/packlist
```

# Known issues

## Performance

While trying to analyze multiple applications, we currently suffer a severe memory leak (300 apps uses up to 600Go of RAM).

## JGit and local Git usage

Since *JGit* API is not complete or too weird to be used easily, we chose to
perform out git operations using both *JGit* and system calls to your local *Git* program.

*JGit* is used in the case of:

- Cloning remote repository
- Calling for the git log in order to process the commits
- Fetching detailed info about commits (message, author, parents, date, ...)

System calls to *Git* are used in the cases of:

- Parse file renamings in a given commit
- Parse diff stats in a given commit

## Some trouble with Spoon

Some of the tools we use are not perfect, thus we have some false positive on smell introduction,
e.g. `compareTo#com.nbossard.packlist.model.Item` (smell n°29) (https://pastebin.com/9khcXdp9).

In this case spoon is identifying a non existing method in the class `Item`.

## Some trouble with Paprika

The smell definition of Paprika may create some smells being refactored and introduced
repetitively. e.g. `openNewTripFragment#com.nbossard.packlist.gui.MainActivityForTest`
or `openNewTripFragment#com.nbossard.packlist.gui.MainActivityForTest`. (smell n°21 and 44 respectively)
