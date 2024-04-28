-- as user postgres@postgres
DROP DATABASE IF EXISTS solarlogger;
DROP ROLE IF EXISTS solarlogger;

CREATE ROLE solarlogger WITH
    LOGIN
    NOSUPERUSER
    INHERIT
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    NOBYPASSRLS
    ENCRYPTED PASSWORD 'SCRAM-SHA-256$4096:Ir0WmlSXvhpTYAxF+leR0Q==$gUWw3976mpnf6FuZ+xgvdYB5IxInXsu5Lbijvtn4S2U=:Ft6PFA64pcv/v+FzjqSTx3Uw2f3JwXQ5sB5oNX9tlxc=';

ALTER USER solarlogger WITH PASSWORD 'Log and roll;-)';

GRANT pg_read_server_files TO solarlogger;

CREATE DATABASE solarlogger
    WITH
    OWNER = solarlogger
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    LOCALE_PROVIDER = 'libc'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;
