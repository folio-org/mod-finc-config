\set schema_name :user_name:schema_base

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE ROLE :schema_name PASSWORD :'user_name' NOSUPERUSER NOCREATEDB INHERIT LOGIN;
GRANT :schema_name TO CURRENT_USER;
CREATE SCHEMA :schema_name AUTHORIZATION :schema_name;

CREATE TABLE IF NOT EXISTS :schema_name.metadata_sources (
    _id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jsonb JSONB NOT NULL
);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA :schema_name TO :schema_name;