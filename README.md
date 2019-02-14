# mod-amsl-discovery

## Introduction

Knowledge base for amsl's discovery module.

This module works tenant-agnostic. That means, its data is not stored separated by tenant.

## Setup

As this module works tenant agnostic, no database schema is created for each tenant on demand. The module uses only one database schema which must be created before deployment.

The folder _database-setup_ contains an _CREATE_DB.sh_ script that creates the database schema.

You can call it as follows:

```
$ CREATE_DB.sh <database name> <database host> <database port> <database admin user> <database user> <database schema name>
```

The parameters

* `database name`: Name of the PostgreSQL database. On an FOLIO vagrant VM this is _okapi_modules_.

* `database host`: Hostname or IP where the database is running, e.g. _localhost_

* `database port`: Database port, e.g. _5432_

* `database admin user`: Username of a role with admin permissions. This user will create a database with corresponding tables and the role which owns the schema

* `database user`: The username of the role which will be created by the _database admin user_. If omitted _ubl_ will be used

* `database schema name`: The name of the schema which will be created by the _database admin user_. If omitted __mod_amsl_discovery_ will be used

For instance, you can call the script as follows:

```
$ CREATE_DB.sh okapi_modules localhost 5432 folio_admin
```

## Run the module

In order to run the module we need to set a few variables either via environment variables or a config file: `DB_HOST, DB_PORT, DB_DATABASE, MOD_USERNAME`

For instance:
`DB_HOST=localhost;DB_PORT=5432;DB_DATABASE=okapi_modules;MOD_USERNAME=ubl`

Here _MOD_USERNAME_ needs to be the same as the _database user_ when setting up the database.

Then, you can start the module as follows:

```
$ env DB_HOST=localhost DB_PORT=5432 DB_DATABASE=okapi_modules MOD_USERNAME=ubl java -jar mod-amsl-discovery-fat.jar
```