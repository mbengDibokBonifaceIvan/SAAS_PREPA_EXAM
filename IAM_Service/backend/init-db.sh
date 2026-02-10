#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    CREATE DATABASE keycloak_db;
    CREATE DATABASE iam_logic_db;
    CREATE DATABASE notification_db;
EOSQL