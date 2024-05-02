-- as postgres@solarlogger

DROP TABLE IF EXISTS public.daily_yield;
CREATE TABLE public.daily_yield
(
    day        date             NOT NULL,
    inverter   integer          NOT NULL,
    yield      double precision NOT NULL,
    created_at timestamp        NOT NULL default now(),
    updated_at timestamp        NOT NULL default now()
);

ALTER TABLE public.daily_yield
    ADD CONSTRAINT daily_yield_pk
        PRIMARY KEY (day, inverter);

ALTER TABLE IF EXISTS public.daily_yield
    OWNER to solarlogger;

-- initial import
COPY public.daily_yield FROM '.../ertrag.log' DELIMITER ' ';

-- create readonly user fro grafana
REASSIGN OWNED BY readonly TO solarlogger;
DROP OWNED BY readonly;
DROP ROLE IF EXISTS readonly;
CREATE ROLE readonly WITH
    LOGIN
    NOSUPERUSER
    INHERIT
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    NOBYPASSRLS
    ENCRYPTED PASSWORD 'SCRAM-SHA-256$4096:Ir0WmlSXvhpTYAxF+leR0Q==$gUWw3976mpnf6FuZ+xgvdYB5IxInXsu5Lbijvtn4S2U=:Ft6PFA64pcv/v+FzjqSTx3Uw2f3JwXQ5sB5oNX9tlxc=';

ALTER USER solarlogger WITH PASSWORD 'Ich darf nur lesen:-(';

GRANT pg_read_all_data TO readonly;
-- GRANT CONNECT ON DATABASE solarlogger TO readonly;
-- GRANT USAGE ON SCHEMA public TO readonly;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public
--     GRANT SELECT ON TABLES TO readonly;
