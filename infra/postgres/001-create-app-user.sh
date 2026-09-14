#!/bin/sh
set -eu

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=app_password="$CRMIX_DB_PASSWORD" <<'SQL'
DO $do$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'crmix_app') THEN
    CREATE ROLE crmix_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
  END IF;
END
$do$;
SELECT format('ALTER ROLE crmix_app PASSWORD %L', :'app_password') \gexec
GRANT CONNECT ON DATABASE crmix TO crmix_app;
GRANT USAGE, CREATE ON SCHEMA public TO crmix_app;
SQL
