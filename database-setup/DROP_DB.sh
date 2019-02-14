#!/bin/bash

database_name=${1:-}
host=${2:-$PGHOST}
port=${3:-$PGPORT}
database_admin_user=${4:-$PGUSER}
database_user=${5:-ubl}
database_schema_base=${6:-_amsl_discovery_kb}

psql --set=user_name="${database_user}" --set=schema_base="${database_schema_base}" -h ${host} -p ${port} -U ${database_admin_user} -d ${database_name} -f "DROP_DB.sql"