\set schema_name :user_name:schema_base

DROP SCHEMA IF EXISTS :schema_name CASCADE;
DROP ROLE IF EXISTS :schema_name;