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

