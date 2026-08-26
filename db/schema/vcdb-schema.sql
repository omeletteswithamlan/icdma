--
-- PostgreSQL database dump
--

\restrict 0TtVOPIsb76Ogqy60wQ9YiZDbMpFltjx9bajrTMsoMmGF6CRZUc6GonxwgFpg2P

-- Dumped from database version 14.24 (Homebrew)
-- Dumped by pg_dump version 14.24 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activity; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.activity (
    activityid bigint NOT NULL,
    projectid integer NOT NULL,
    description character varying(200),
    duration integer NOT NULL,
    csidivisionid integer NOT NULL,
    responsibilityid integer,
    code character varying(100)
);


ALTER TABLE public.activity OWNER TO construction;

--
-- Name: activity_activityid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.activity_activityid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.activity_activityid_seq OWNER TO construction;

--
-- Name: activity_activityid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.activity_activityid_seq OWNED BY public.activity.activityid;


--
-- Name: activitymedia; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.activitymedia (
    activitymediaid bigint NOT NULL,
    activityid integer NOT NULL,
    mediaskillid integer NOT NULL
);


ALTER TABLE public.activitymedia OWNER TO construction;

--
-- Name: activitymedia_activitymediaid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.activitymedia_activitymediaid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.activitymedia_activitymediaid_seq OWNER TO construction;

--
-- Name: activitymedia_activitymediaid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.activitymedia_activitymediaid_seq OWNED BY public.activitymedia.activitymediaid;


--
-- Name: constraints; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.constraints (
    constraintid bigint NOT NULL,
    fromactivityid integer NOT NULL,
    toactivityid integer NOT NULL,
    length integer NOT NULL,
    soft boolean NOT NULL
);


ALTER TABLE public.constraints OWNER TO construction;

--
-- Name: constraints_constraintid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.constraints_constraintid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.constraints_constraintid_seq OWNER TO construction;

--
-- Name: constraints_constraintid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.constraints_constraintid_seq OWNED BY public.constraints.constraintid;


--
-- Name: csidivision; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.csidivision (
    csiid integer NOT NULL,
    name character varying(250) NOT NULL,
    description character varying(1000)
);


ALTER TABLE public.csidivision OWNER TO construction;

--
-- Name: csidivision_csiid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.csidivision_csiid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.csidivision_csiid_seq OWNER TO construction;

--
-- Name: csidivision_csiid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.csidivision_csiid_seq OWNED BY public.csidivision.csiid;


--
-- Name: driving_material; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.driving_material (
    driving_materialid integer NOT NULL,
    activityid integer NOT NULL,
    materialid integer NOT NULL
);


ALTER TABLE public.driving_material OWNER TO construction;

--
-- Name: driving_material_driving_materialid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.driving_material_driving_materialid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.driving_material_driving_materialid_seq OWNER TO construction;

--
-- Name: driving_material_driving_materialid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.driving_material_driving_materialid_seq OWNED BY public.driving_material.driving_materialid;


--
-- Name: history; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.history (
    historyid bigint NOT NULL,
    projectid integer NOT NULL,
    date timestamp without time zone DEFAULT now() NOT NULL,
    skillid integer NOT NULL
);


ALTER TABLE public.history OWNER TO construction;

--
-- Name: history_historyid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.history_historyid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.history_historyid_seq OWNER TO construction;

--
-- Name: history_historyid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.history_historyid_seq OWNED BY public.history.historyid;


--
-- Name: historyactivity; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historyactivity (
    historyactivityid bigint NOT NULL,
    historytimeid integer NOT NULL,
    activityid integer NOT NULL,
    starttime integer NOT NULL,
    endtime integer NOT NULL
);


ALTER TABLE public.historyactivity OWNER TO construction;

--
-- Name: historyactivity_historyactivityid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historyactivity_historyactivityid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historyactivity_historyactivityid_seq OWNER TO construction;

--
-- Name: historyactivity_historyactivityid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historyactivity_historyactivityid_seq OWNED BY public.historyactivity.historyactivityid;


--
-- Name: historyevent; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historyevent (
    historyeventid integer NOT NULL,
    historytimeid integer NOT NULL,
    ruleid integer NOT NULL,
    description character varying(200)
);


ALTER TABLE public.historyevent OWNER TO construction;

--
-- Name: historyevent_historyeventid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historyevent_historyeventid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historyevent_historyeventid_seq OWNER TO construction;

--
-- Name: historyevent_historyeventid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historyevent_historyeventid_seq OWNED BY public.historyevent.historyeventid;


--
-- Name: historylaborallocation; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historylaborallocation (
    historylaborallocationid bigint NOT NULL,
    historylaborcrewallocationid integer NOT NULL,
    laborid integer NOT NULL,
    quantity integer NOT NULL
);


ALTER TABLE public.historylaborallocation OWNER TO construction;

--
-- Name: historylaborallocation_historylaborallocationid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historylaborallocation_historylaborallocationid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historylaborallocation_historylaborallocationid_seq OWNER TO construction;

--
-- Name: historylaborallocation_historylaborallocationid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historylaborallocation_historylaborallocationid_seq OWNED BY public.historylaborallocation.historylaborallocationid;


--
-- Name: historylaborcrewallocation; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historylaborcrewallocation (
    historylaborcrewallocationid bigint NOT NULL,
    historytimeid integer NOT NULL,
    laborcrewid integer NOT NULL,
    activityid integer NOT NULL,
    hours integer NOT NULL,
    days integer NOT NULL,
    wage double precision NOT NULL
);


ALTER TABLE public.historylaborcrewallocation OWNER TO construction;

--
-- Name: historylaborcrewallocation_historylaborcrewallocationid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historylaborcrewallocation_historylaborcrewallocationid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historylaborcrewallocation_historylaborcrewallocationid_seq OWNER TO construction;

--
-- Name: historylaborcrewallocation_historylaborcrewallocationid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historylaborcrewallocation_historylaborcrewallocationid_seq OWNED BY public.historylaborcrewallocation.historylaborcrewallocationid;


--
-- Name: historymaterialallocation; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historymaterialallocation (
    historymaterialallocationid bigint NOT NULL,
    historytimeid integer NOT NULL,
    materialid integer NOT NULL,
    activityid integer,
    quantity double precision NOT NULL
);


ALTER TABLE public.historymaterialallocation OWNER TO construction;

--
-- Name: historymaterialallocation_historymaterialallocationid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historymaterialallocation_historymaterialallocationid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historymaterialallocation_historymaterialallocationid_seq OWNER TO construction;

--
-- Name: historymaterialallocation_historymaterialallocationid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historymaterialallocation_historymaterialallocationid_seq OWNED BY public.historymaterialallocation.historymaterialallocationid;


--
-- Name: historyquerydays; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historyquerydays (
    historytimeid integer NOT NULL,
    length integer NOT NULL,
    quantity integer NOT NULL
);


ALTER TABLE public.historyquerydays OWNER TO construction;

--
-- Name: historyqueryresults; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historyqueryresults (
    historyqueryresultsid bigint NOT NULL,
    historytimeid integer NOT NULL,
    cost double precision NOT NULL,
    quantity integer NOT NULL
);


ALTER TABLE public.historyqueryresults OWNER TO construction;

--
-- Name: historyqueryresults_historyqueryresultsid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historyqueryresults_historyqueryresultsid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historyqueryresults_historyqueryresultsid_seq OWNER TO construction;

--
-- Name: historyqueryresults_historyqueryresultsid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historyqueryresults_historyqueryresultsid_seq OWNED BY public.historyqueryresults.historyqueryresultsid;


--
-- Name: historytime; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historytime (
    historytimeid bigint NOT NULL,
    historyid integer NOT NULL,
    "time" integer NOT NULL,
    date timestamp without time zone DEFAULT now() NOT NULL,
    lasthistorytimeid integer,
    spaceused double precision,
    materialcost double precision,
    laborcost double precision,
    indirectcost double precision
);


ALTER TABLE public.historytime OWNER TO construction;

--
-- Name: historytime_historytimeid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historytime_historytimeid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historytime_historytimeid_seq OWNER TO construction;

--
-- Name: historytime_historytimeid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historytime_historytimeid_seq OWNED BY public.historytime.historytimeid;


--
-- Name: historyvariable; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.historyvariable (
    historyvariableid bigint NOT NULL,
    historytimeid integer NOT NULL,
    activityid integer,
    variableid integer NOT NULL,
    state character varying(100) NOT NULL
);


ALTER TABLE public.historyvariable OWNER TO construction;

--
-- Name: historyvariable_historyvariableid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.historyvariable_historyvariableid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.historyvariable_historyvariableid_seq OWNER TO construction;

--
-- Name: historyvariable_historyvariableid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.historyvariable_historyvariableid_seq OWNED BY public.historyvariable.historyvariableid;


--
-- Name: labor; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.labor (
    laborid bigint NOT NULL,
    description character varying(200),
    unitcost double precision NOT NULL
);


ALTER TABLE public.labor OWNER TO construction;

--
-- Name: labor_copy; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.labor_copy (
    laborid bigint,
    description character varying(200),
    unitcost double precision
);


ALTER TABLE public.labor_copy OWNER TO construction;

--
-- Name: labor_laborid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.labor_laborid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.labor_laborid_seq OWNER TO construction;

--
-- Name: labor_laborid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.labor_laborid_seq OWNED BY public.labor.laborid;


--
-- Name: laborcrew; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.laborcrew (
    laborcrewid bigint NOT NULL,
    description character varying(100)
);


ALTER TABLE public.laborcrew OWNER TO construction;

--
-- Name: laborcrew_laborcrewid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.laborcrew_laborcrewid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.laborcrew_laborcrewid_seq OWNER TO construction;

--
-- Name: laborcrew_laborcrewid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.laborcrew_laborcrewid_seq OWNED BY public.laborcrew.laborcrewid;


--
-- Name: laborcrewentry; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.laborcrewentry (
    laborcrewentryid bigint NOT NULL,
    laborcrewid integer NOT NULL,
    laborid integer NOT NULL,
    amount integer NOT NULL
);


ALTER TABLE public.laborcrewentry OWNER TO construction;

--
-- Name: laborcrewentry_laborcrewentryid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.laborcrewentry_laborcrewentryid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.laborcrewentry_laborcrewentryid_seq OWNER TO construction;

--
-- Name: laborcrewentry_laborcrewentryid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.laborcrewentry_laborcrewentryid_seq OWNED BY public.laborcrewentry.laborcrewentryid;


--
-- Name: laborcrewuse; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.laborcrewuse (
    laborcrewuseid bigint NOT NULL,
    laborcrewid integer NOT NULL,
    activityid integer NOT NULL,
    description character varying(200)
);


ALTER TABLE public.laborcrewuse OWNER TO construction;

--
-- Name: laborcrewuse_laborcrewuseid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.laborcrewuse_laborcrewuseid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.laborcrewuse_laborcrewuseid_seq OWNER TO construction;

--
-- Name: laborcrewuse_laborcrewuseid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.laborcrewuse_laborcrewuseid_seq OWNED BY public.laborcrewuse.laborcrewuseid;


--
-- Name: labormedia; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.labormedia (
    labormediaid bigint NOT NULL,
    laborid integer NOT NULL,
    mediaskillid integer NOT NULL
);


ALTER TABLE public.labormedia OWNER TO construction;

--
-- Name: labormedia_labormediaid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.labormedia_labormediaid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.labormedia_labormediaid_seq OWNER TO construction;

--
-- Name: labormedia_labormediaid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.labormedia_labormediaid_seq OWNED BY public.labormedia.labormediaid;


--
-- Name: laboruse; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.laboruse (
    laboruseid bigint NOT NULL,
    activityid integer NOT NULL,
    laborid integer NOT NULL,
    quantity_skilled integer NOT NULL,
    quantity_unskilled integer NOT NULL,
    description character varying(200)
);


ALTER TABLE public.laboruse OWNER TO construction;

--
-- Name: laboruse_laboruseid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.laboruse_laboruseid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.laboruse_laboruseid_seq OWNER TO construction;

--
-- Name: laboruse_laboruseid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.laboruse_laboruseid_seq OWNED BY public.laboruse.laboruseid;


--
-- Name: laborvariable; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.laborvariable (
    laborvariableid bigint NOT NULL,
    laborid integer NOT NULL,
    laborcrewid integer NOT NULL,
    variableid integer NOT NULL,
    projectid integer NOT NULL
);


ALTER TABLE public.laborvariable OWNER TO construction;

--
-- Name: laborvariable_laborvariableid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.laborvariable_laborvariableid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.laborvariable_laborvariableid_seq OWNER TO construction;

--
-- Name: laborvariable_laborvariableid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.laborvariable_laborvariableid_seq OWNED BY public.laborvariable.laborvariableid;


--
-- Name: material; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.material (
    materialid bigint NOT NULL,
    description character varying(200),
    unitcost double precision NOT NULL,
    area double precision NOT NULL,
    perishable boolean
);


ALTER TABLE public.material OWNER TO construction;

--
-- Name: material_copy; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.material_copy (
    materialid bigint,
    description character varying(200),
    unitcost double precision,
    area double precision,
    perishable boolean
);


ALTER TABLE public.material_copy OWNER TO construction;

--
-- Name: material_materialid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.material_materialid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.material_materialid_seq OWNER TO construction;

--
-- Name: material_materialid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.material_materialid_seq OWNED BY public.material.materialid;


--
-- Name: materialmedia; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.materialmedia (
    materialmediaid bigint NOT NULL,
    materialid integer NOT NULL,
    mediaskillid integer NOT NULL
);


ALTER TABLE public.materialmedia OWNER TO construction;

--
-- Name: materialmedia_materialmediaid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.materialmedia_materialmediaid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.materialmedia_materialmediaid_seq OWNER TO construction;

--
-- Name: materialmedia_materialmediaid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.materialmedia_materialmediaid_seq OWNED BY public.materialmedia.materialmediaid;


--
-- Name: materialnew; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.materialnew (
    materialid character(10),
    description character(100),
    unitcost integer,
    area character(5),
    perishable character(1)
);


ALTER TABLE public.materialnew OWNER TO construction;

--
-- Name: materialuse; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.materialuse (
    materialuseid bigint NOT NULL,
    activityid integer NOT NULL,
    materialid integer NOT NULL,
    quantity double precision NOT NULL,
    description character varying(200)
);


ALTER TABLE public.materialuse OWNER TO construction;

--
-- Name: materialuse_copy; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.materialuse_copy (
    materialuseid bigint,
    activityid integer,
    materialid integer,
    quantity double precision,
    description character varying(200)
);


ALTER TABLE public.materialuse_copy OWNER TO construction;

--
-- Name: materialuse_materialuseid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.materialuse_materialuseid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.materialuse_materialuseid_seq OWNER TO construction;

--
-- Name: materialuse_materialuseid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.materialuse_materialuseid_seq OWNED BY public.materialuse.materialuseid;


--
-- Name: materialvariable; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.materialvariable (
    materialvariableid bigint NOT NULL,
    materialid integer NOT NULL,
    variableid integer NOT NULL,
    projectid integer NOT NULL
);


ALTER TABLE public.materialvariable OWNER TO construction;

--
-- Name: materialvariable_materialvariableid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.materialvariable_materialvariableid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.materialvariable_materialvariableid_seq OWNER TO construction;

--
-- Name: materialvariable_materialvariableid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.materialvariable_materialvariableid_seq OWNED BY public.materialvariable.materialvariableid;


--
-- Name: media; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.media (
    mediaid bigint NOT NULL,
    objtype character varying(10) NOT NULL,
    path character varying(1000)
);


ALTER TABLE public.media OWNER TO construction;

--
-- Name: media_mediaid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.media_mediaid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.media_mediaid_seq OWNER TO construction;

--
-- Name: media_mediaid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.media_mediaid_seq OWNED BY public.media.mediaid;


--
-- Name: mediaskill; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.mediaskill (
    mediaskillid bigint NOT NULL,
    mediaid integer NOT NULL,
    skillid integer NOT NULL
);


ALTER TABLE public.mediaskill OWNER TO construction;

--
-- Name: mediaskill_mediaskillid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.mediaskill_mediaskillid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.mediaskill_mediaskillid_seq OWNER TO construction;

--
-- Name: mediaskill_mediaskillid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.mediaskill_mediaskillid_seq OWNED BY public.mediaskill.mediaskillid;


--
-- Name: old_history; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_history (
    historyid bigint,
    projectid integer,
    date timestamp without time zone,
    skillid integer
);


ALTER TABLE public.old_history OWNER TO construction;

--
-- Name: old_historyactivity; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historyactivity (
    historyactivityid bigint,
    historytimeid integer,
    activityid integer,
    starttime integer,
    endtime integer
);


ALTER TABLE public.old_historyactivity OWNER TO construction;

--
-- Name: old_historyevent; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historyevent (
    historyeventid integer,
    historytimeid integer,
    ruleid integer,
    description character varying(200)
);


ALTER TABLE public.old_historyevent OWNER TO construction;

--
-- Name: old_historylaborallocation; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historylaborallocation (
    historylaborallocationid bigint,
    historylaborcrewallocationid integer,
    laborid integer,
    quantity integer
);


ALTER TABLE public.old_historylaborallocation OWNER TO construction;

--
-- Name: old_historylaborcrewallocation; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historylaborcrewallocation (
    historylaborcrewallocationid bigint,
    historytimeid integer,
    laborcrewid integer,
    activityid integer,
    hours integer,
    days integer,
    wage double precision
);


ALTER TABLE public.old_historylaborcrewallocation OWNER TO construction;

--
-- Name: old_historymaterialallocation; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historymaterialallocation (
    historymaterialallocationid bigint,
    historytimeid integer,
    materialid integer,
    activityid integer,
    quantity double precision
);


ALTER TABLE public.old_historymaterialallocation OWNER TO construction;

--
-- Name: old_historyqueryresults; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historyqueryresults (
    historyqueryresultsid bigint,
    historytimeid integer,
    cost double precision,
    quantity integer
);


ALTER TABLE public.old_historyqueryresults OWNER TO construction;

--
-- Name: old_historytime; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historytime (
    historytimeid bigint,
    historyid integer,
    "time" integer,
    date timestamp without time zone,
    lasthistorytimeid integer,
    spaceused double precision,
    materialcost double precision,
    laborcost double precision,
    indirectcost double precision
);


ALTER TABLE public.old_historytime OWNER TO construction;

--
-- Name: old_historyvariable; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.old_historyvariable (
    historyvariableid bigint,
    historytimeid integer,
    activityid integer,
    variableid integer,
    state character varying(100)
);


ALTER TABLE public.old_historyvariable OWNER TO construction;

--
-- Name: plannedactivity; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.plannedactivity (
    activityid integer NOT NULL,
    start integer NOT NULL
);


ALTER TABLE public.plannedactivity OWNER TO construction;

--
-- Name: postcondition; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.postcondition (
    postconditionid bigint NOT NULL,
    variableid integer NOT NULL,
    state character varying(200) NOT NULL,
    "time" integer NOT NULL,
    action character varying(3)
);


ALTER TABLE public.postcondition OWNER TO construction;

--
-- Name: postcondition_postconditionid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.postcondition_postconditionid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.postcondition_postconditionid_seq OWNER TO construction;

--
-- Name: postcondition_postconditionid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.postcondition_postconditionid_seq OWNED BY public.postcondition.postconditionid;


--
-- Name: precondition; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.precondition (
    preconditionid bigint NOT NULL,
    variableid integer NOT NULL,
    state character varying(200) NOT NULL,
    action character varying(3) NOT NULL
);


ALTER TABLE public.precondition OWNER TO construction;

--
-- Name: precondition_preconditionid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.precondition_preconditionid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.precondition_preconditionid_seq OWNER TO construction;

--
-- Name: precondition_preconditionid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.precondition_preconditionid_seq OWNED BY public.precondition.preconditionid;


--
-- Name: project; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.project (
    projectid bigint NOT NULL,
    description character varying(200),
    name character varying(100) NOT NULL,
    space integer NOT NULL,
    overstock_penalty double precision NOT NULL,
    startdate date NOT NULL,
    overhead double precision,
    "interval" integer NOT NULL
);


ALTER TABLE public.project OWNER TO construction;

--
-- Name: project_projectid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.project_projectid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.project_projectid_seq OWNER TO construction;

--
-- Name: project_projectid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.project_projectid_seq OWNED BY public.project.projectid;


--
-- Name: projectmedia; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.projectmedia (
    projectmediaid bigint NOT NULL,
    projectid integer NOT NULL,
    mediaskillid integer NOT NULL
);


ALTER TABLE public.projectmedia OWNER TO construction;

--
-- Name: projectmedia_projectmediaid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.projectmedia_projectmediaid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.projectmedia_projectmediaid_seq OWNER TO construction;

--
-- Name: projectmedia_projectmediaid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.projectmedia_projectmediaid_seq OWNED BY public.projectmedia.projectmediaid;


--
-- Name: projectrule; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.projectrule (
    projectruleid bigint NOT NULL,
    projectid integer NOT NULL,
    ruleid integer NOT NULL,
    ordering integer
);


ALTER TABLE public.projectrule OWNER TO construction;

--
-- Name: projectrule_projectruleid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.projectrule_projectruleid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.projectrule_projectruleid_seq OWNER TO construction;

--
-- Name: projectrule_projectruleid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.projectrule_projectruleid_seq OWNED BY public.projectrule.projectruleid;


--
-- Name: responsibility; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.responsibility (
    responsibilityid integer NOT NULL,
    responsibilityname character varying(100) NOT NULL
);


ALTER TABLE public.responsibility OWNER TO construction;

--
-- Name: responsibility_responsibilityid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.responsibility_responsibilityid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.responsibility_responsibilityid_seq OWNER TO construction;

--
-- Name: responsibility_responsibilityid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.responsibility_responsibilityid_seq OWNED BY public.responsibility.responsibilityid;


--
-- Name: rule; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.rule (
    ruleid bigint NOT NULL,
    description character varying(200) NOT NULL,
    message character varying(1000),
    probability double precision NOT NULL,
    global boolean NOT NULL
);


ALTER TABLE public.rule OWNER TO construction;

--
-- Name: rule_ruleid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.rule_ruleid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rule_ruleid_seq OWNER TO construction;

--
-- Name: rule_ruleid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.rule_ruleid_seq OWNED BY public.rule.ruleid;


--
-- Name: rulemedia; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.rulemedia (
    rulemediaid bigint NOT NULL,
    ruleid integer NOT NULL,
    mediaskillid integer NOT NULL
);


ALTER TABLE public.rulemedia OWNER TO construction;

--
-- Name: rulemedia_rulemediaid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.rulemedia_rulemediaid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rulemedia_rulemediaid_seq OWNER TO construction;

--
-- Name: rulemedia_rulemediaid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.rulemedia_rulemediaid_seq OWNED BY public.rulemedia.rulemediaid;


--
-- Name: rulepostcondition; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.rulepostcondition (
    rulepostconditionid bigint NOT NULL,
    ruleid integer NOT NULL,
    postconditionid integer NOT NULL
);


ALTER TABLE public.rulepostcondition OWNER TO construction;

--
-- Name: rulepostcondition_rulepostconditionid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.rulepostcondition_rulepostconditionid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rulepostcondition_rulepostconditionid_seq OWNER TO construction;

--
-- Name: rulepostcondition_rulepostconditionid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.rulepostcondition_rulepostconditionid_seq OWNED BY public.rulepostcondition.rulepostconditionid;


--
-- Name: ruleprecondition; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.ruleprecondition (
    rulepreconditionid bigint NOT NULL,
    ruleid integer NOT NULL,
    preconditionid integer NOT NULL
);


ALTER TABLE public.ruleprecondition OWNER TO construction;

--
-- Name: ruleprecondition_rulepreconditionid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.ruleprecondition_rulepreconditionid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ruleprecondition_rulepreconditionid_seq OWNER TO construction;

--
-- Name: ruleprecondition_rulepreconditionid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.ruleprecondition_rulepreconditionid_seq OWNED BY public.ruleprecondition.rulepreconditionid;


--
-- Name: ruleresource; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.ruleresource (
    ruleresourceid bigint NOT NULL,
    ruleid integer NOT NULL,
    objtype character varying(10) NOT NULL,
    object bytea NOT NULL
);


ALTER TABLE public.ruleresource OWNER TO construction;

--
-- Name: ruleresource_ruleresourceid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.ruleresource_ruleresourceid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ruleresource_ruleresourceid_seq OWNER TO construction;

--
-- Name: ruleresource_ruleresourceid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.ruleresource_ruleresourceid_seq OWNED BY public.ruleresource.ruleresourceid;


--
-- Name: skill; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.skill (
    skillid bigint NOT NULL,
    description character varying(100) NOT NULL
);


ALTER TABLE public.skill OWNER TO construction;

--
-- Name: skill_skillid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.skill_skillid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.skill_skillid_seq OWNER TO construction;

--
-- Name: skill_skillid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.skill_skillid_seq OWNED BY public.skill.skillid;


--
-- Name: variable; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.variable (
    variableid bigint NOT NULL,
    label character varying(200) NOT NULL,
    global boolean NOT NULL,
    initialstate character varying(200) NOT NULL,
    discreet boolean NOT NULL
);


ALTER TABLE public.variable OWNER TO construction;

--
-- Name: variable_variableid_seq; Type: SEQUENCE; Schema: public; Owner: construction
--

CREATE SEQUENCE public.variable_variableid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variable_variableid_seq OWNER TO construction;

--
-- Name: variable_variableid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: construction
--

ALTER SEQUENCE public.variable_variableid_seq OWNED BY public.variable.variableid;


--
-- Name: weather; Type: TABLE; Schema: public; Owner: construction
--

CREATE TABLE public.weather (
    month integer,
    low integer,
    high integer,
    precipitation integer
);


ALTER TABLE public.weather OWNER TO construction;

--
-- Name: activity activityid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.activity ALTER COLUMN activityid SET DEFAULT nextval('public.activity_activityid_seq'::regclass);


--
-- Name: activitymedia activitymediaid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.activitymedia ALTER COLUMN activitymediaid SET DEFAULT nextval('public.activitymedia_activitymediaid_seq'::regclass);


--
-- Name: constraints constraintid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.constraints ALTER COLUMN constraintid SET DEFAULT nextval('public.constraints_constraintid_seq'::regclass);


--
-- Name: csidivision csiid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.csidivision ALTER COLUMN csiid SET DEFAULT nextval('public.csidivision_csiid_seq'::regclass);


--
-- Name: driving_material driving_materialid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.driving_material ALTER COLUMN driving_materialid SET DEFAULT nextval('public.driving_material_driving_materialid_seq'::regclass);


--
-- Name: history historyid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.history ALTER COLUMN historyid SET DEFAULT nextval('public.history_historyid_seq'::regclass);


--
-- Name: historyactivity historyactivityid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historyactivity ALTER COLUMN historyactivityid SET DEFAULT nextval('public.historyactivity_historyactivityid_seq'::regclass);


--
-- Name: historyevent historyeventid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historyevent ALTER COLUMN historyeventid SET DEFAULT nextval('public.historyevent_historyeventid_seq'::regclass);


--
-- Name: historylaborallocation historylaborallocationid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historylaborallocation ALTER COLUMN historylaborallocationid SET DEFAULT nextval('public.historylaborallocation_historylaborallocationid_seq'::regclass);


--
-- Name: historylaborcrewallocation historylaborcrewallocationid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historylaborcrewallocation ALTER COLUMN historylaborcrewallocationid SET DEFAULT nextval('public.historylaborcrewallocation_historylaborcrewallocationid_seq'::regclass);


--
-- Name: historymaterialallocation historymaterialallocationid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historymaterialallocation ALTER COLUMN historymaterialallocationid SET DEFAULT nextval('public.historymaterialallocation_historymaterialallocationid_seq'::regclass);


--
-- Name: historyqueryresults historyqueryresultsid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historyqueryresults ALTER COLUMN historyqueryresultsid SET DEFAULT nextval('public.historyqueryresults_historyqueryresultsid_seq'::regclass);


--
-- Name: historytime historytimeid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historytime ALTER COLUMN historytimeid SET DEFAULT nextval('public.historytime_historytimeid_seq'::regclass);


--
-- Name: historyvariable historyvariableid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historyvariable ALTER COLUMN historyvariableid SET DEFAULT nextval('public.historyvariable_historyvariableid_seq'::regclass);


--
-- Name: labor laborid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.labor ALTER COLUMN laborid SET DEFAULT nextval('public.labor_laborid_seq'::regclass);


--
-- Name: laborcrew laborcrewid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.laborcrew ALTER COLUMN laborcrewid SET DEFAULT nextval('public.laborcrew_laborcrewid_seq'::regclass);


--
-- Name: laborcrewentry laborcrewentryid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.laborcrewentry ALTER COLUMN laborcrewentryid SET DEFAULT nextval('public.laborcrewentry_laborcrewentryid_seq'::regclass);


--
-- Name: laborcrewuse laborcrewuseid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.laborcrewuse ALTER COLUMN laborcrewuseid SET DEFAULT nextval('public.laborcrewuse_laborcrewuseid_seq'::regclass);


--
-- Name: labormedia labormediaid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.labormedia ALTER COLUMN labormediaid SET DEFAULT nextval('public.labormedia_labormediaid_seq'::regclass);


--
-- Name: laboruse laboruseid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.laboruse ALTER COLUMN laboruseid SET DEFAULT nextval('public.laboruse_laboruseid_seq'::regclass);


--
-- Name: laborvariable laborvariableid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.laborvariable ALTER COLUMN laborvariableid SET DEFAULT nextval('public.laborvariable_laborvariableid_seq'::regclass);


--
-- Name: material materialid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.material ALTER COLUMN materialid SET DEFAULT nextval('public.material_materialid_seq'::regclass);


--
-- Name: materialmedia materialmediaid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.materialmedia ALTER COLUMN materialmediaid SET DEFAULT nextval('public.materialmedia_materialmediaid_seq'::regclass);


--
-- Name: materialuse materialuseid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.materialuse ALTER COLUMN materialuseid SET DEFAULT nextval('public.materialuse_materialuseid_seq'::regclass);


--
-- Name: materialvariable materialvariableid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.materialvariable ALTER COLUMN materialvariableid SET DEFAULT nextval('public.materialvariable_materialvariableid_seq'::regclass);


--
-- Name: media mediaid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.media ALTER COLUMN mediaid SET DEFAULT nextval('public.media_mediaid_seq'::regclass);


--
-- Name: mediaskill mediaskillid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.mediaskill ALTER COLUMN mediaskillid SET DEFAULT nextval('public.mediaskill_mediaskillid_seq'::regclass);


--
-- Name: postcondition postconditionid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.postcondition ALTER COLUMN postconditionid SET DEFAULT nextval('public.postcondition_postconditionid_seq'::regclass);


--
-- Name: precondition preconditionid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.precondition ALTER COLUMN preconditionid SET DEFAULT nextval('public.precondition_preconditionid_seq'::regclass);


--
-- Name: project projectid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.project ALTER COLUMN projectid SET DEFAULT nextval('public.project_projectid_seq'::regclass);


--
-- Name: projectmedia projectmediaid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.projectmedia ALTER COLUMN projectmediaid SET DEFAULT nextval('public.projectmedia_projectmediaid_seq'::regclass);


--
-- Name: projectrule projectruleid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.projectrule ALTER COLUMN projectruleid SET DEFAULT nextval('public.projectrule_projectruleid_seq'::regclass);


--
-- Name: responsibility responsibilityid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.responsibility ALTER COLUMN responsibilityid SET DEFAULT nextval('public.responsibility_responsibilityid_seq'::regclass);


--
-- Name: rule ruleid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.rule ALTER COLUMN ruleid SET DEFAULT nextval('public.rule_ruleid_seq'::regclass);


--
-- Name: rulemedia rulemediaid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.rulemedia ALTER COLUMN rulemediaid SET DEFAULT nextval('public.rulemedia_rulemediaid_seq'::regclass);


--
-- Name: rulepostcondition rulepostconditionid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.rulepostcondition ALTER COLUMN rulepostconditionid SET DEFAULT nextval('public.rulepostcondition_rulepostconditionid_seq'::regclass);


--
-- Name: ruleprecondition rulepreconditionid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.ruleprecondition ALTER COLUMN rulepreconditionid SET DEFAULT nextval('public.ruleprecondition_rulepreconditionid_seq'::regclass);


--
-- Name: ruleresource ruleresourceid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.ruleresource ALTER COLUMN ruleresourceid SET DEFAULT nextval('public.ruleresource_ruleresourceid_seq'::regclass);


--
-- Name: skill skillid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.skill ALTER COLUMN skillid SET DEFAULT nextval('public.skill_skillid_seq'::regclass);


--
-- Name: variable variableid; Type: DEFAULT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.variable ALTER COLUMN variableid SET DEFAULT nextval('public.variable_variableid_seq'::regclass);


--
-- Name: activity activity_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT activity_pkey PRIMARY KEY (activityid);


--
-- Name: constraints constraints_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.constraints
    ADD CONSTRAINT constraints_pkey PRIMARY KEY (constraintid);


--
-- Name: csidivision csidivision_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.csidivision
    ADD CONSTRAINT csidivision_pkey PRIMARY KEY (csiid);


--
-- Name: driving_material driving_material_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.driving_material
    ADD CONSTRAINT driving_material_pkey PRIMARY KEY (driving_materialid);


--
-- Name: historyevent historyevent_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historyevent
    ADD CONSTRAINT historyevent_pkey PRIMARY KEY (historyeventid);


--
-- Name: historyquerydays historyquerydays_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.historyquerydays
    ADD CONSTRAINT historyquerydays_pkey PRIMARY KEY (historytimeid, length);


--
-- Name: labor labor_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.labor
    ADD CONSTRAINT labor_pkey PRIMARY KEY (laborid);


--
-- Name: laboruse laboruse_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.laboruse
    ADD CONSTRAINT laboruse_pkey PRIMARY KEY (laboruseid);


--
-- Name: material material_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.material
    ADD CONSTRAINT material_pkey PRIMARY KEY (materialid);


--
-- Name: materialuse materialuse_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.materialuse
    ADD CONSTRAINT materialuse_pkey PRIMARY KEY (materialuseid);


--
-- Name: plannedactivity plannedactivity_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.plannedactivity
    ADD CONSTRAINT plannedactivity_pkey PRIMARY KEY (activityid);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (projectid);


--
-- Name: responsibility responsibility_pkey; Type: CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.responsibility
    ADD CONSTRAINT responsibility_pkey PRIMARY KEY (responsibilityid);


--
-- Name: activity $1; Type: FK CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT "$1" FOREIGN KEY (csidivisionid) REFERENCES public.csidivision(csiid);


--
-- Name: driving_material $1; Type: FK CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.driving_material
    ADD CONSTRAINT "$1" FOREIGN KEY (activityid) REFERENCES public.activity(activityid);


--
-- Name: plannedactivity $1; Type: FK CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.plannedactivity
    ADD CONSTRAINT "$1" FOREIGN KEY (activityid) REFERENCES public.activity(activityid);


--
-- Name: activity $2; Type: FK CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT "$2" FOREIGN KEY (responsibilityid) REFERENCES public.responsibility(responsibilityid);


--
-- Name: driving_material $2; Type: FK CONSTRAINT; Schema: public; Owner: construction
--

ALTER TABLE ONLY public.driving_material
    ADD CONSTRAINT "$2" FOREIGN KEY (materialid) REFERENCES public.material(materialid);


--
-- Name: TABLE activity; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.activity TO postgres WITH GRANT OPTION;


--
-- Name: TABLE activitymedia; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.activitymedia TO postgres WITH GRANT OPTION;


--
-- Name: TABLE constraints; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.constraints TO postgres WITH GRANT OPTION;


--
-- Name: TABLE csidivision; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.csidivision TO postgres WITH GRANT OPTION;


--
-- Name: TABLE history; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.history TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historyactivity; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historyactivity TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historylaborallocation; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historylaborallocation TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historylaborcrewallocation; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historylaborcrewallocation TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historymaterialallocation; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historymaterialallocation TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historyqueryresults; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historyqueryresults TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historytime; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historytime TO postgres WITH GRANT OPTION;


--
-- Name: TABLE historyvariable; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.historyvariable TO postgres WITH GRANT OPTION;


--
-- Name: TABLE labor; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.labor TO postgres WITH GRANT OPTION;


--
-- Name: TABLE laborcrew; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.laborcrew TO postgres WITH GRANT OPTION;


--
-- Name: TABLE laborcrewentry; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.laborcrewentry TO postgres WITH GRANT OPTION;


--
-- Name: TABLE laborcrewuse; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.laborcrewuse TO postgres WITH GRANT OPTION;


--
-- Name: TABLE labormedia; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.labormedia TO postgres WITH GRANT OPTION;


--
-- Name: TABLE laboruse; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.laboruse TO postgres WITH GRANT OPTION;


--
-- Name: TABLE laborvariable; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.laborvariable TO postgres WITH GRANT OPTION;


--
-- Name: TABLE material; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.material TO postgres WITH GRANT OPTION;


--
-- Name: TABLE materialmedia; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.materialmedia TO postgres WITH GRANT OPTION;


--
-- Name: TABLE materialuse; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.materialuse TO postgres WITH GRANT OPTION;


--
-- Name: TABLE materialvariable; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.materialvariable TO postgres WITH GRANT OPTION;


--
-- Name: TABLE media; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.media TO postgres WITH GRANT OPTION;


--
-- Name: TABLE mediaskill; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.mediaskill TO postgres WITH GRANT OPTION;


--
-- Name: TABLE postcondition; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.postcondition TO postgres WITH GRANT OPTION;


--
-- Name: TABLE precondition; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.precondition TO postgres WITH GRANT OPTION;


--
-- Name: TABLE project; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.project TO postgres WITH GRANT OPTION;


--
-- Name: TABLE projectmedia; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.projectmedia TO postgres WITH GRANT OPTION;


--
-- Name: TABLE projectrule; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.projectrule TO postgres WITH GRANT OPTION;


--
-- Name: TABLE responsibility; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.responsibility TO postgres WITH GRANT OPTION;


--
-- Name: TABLE rule; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.rule TO postgres WITH GRANT OPTION;


--
-- Name: TABLE rulemedia; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.rulemedia TO postgres WITH GRANT OPTION;


--
-- Name: TABLE rulepostcondition; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.rulepostcondition TO postgres WITH GRANT OPTION;


--
-- Name: TABLE ruleprecondition; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.ruleprecondition TO postgres WITH GRANT OPTION;


--
-- Name: TABLE ruleresource; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.ruleresource TO postgres WITH GRANT OPTION;


--
-- Name: TABLE skill; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.skill TO postgres WITH GRANT OPTION;


--
-- Name: TABLE variable; Type: ACL; Schema: public; Owner: construction
--

GRANT ALL ON TABLE public.variable TO postgres WITH GRANT OPTION;


--
-- PostgreSQL database dump complete
--

\unrestrict 0TtVOPIsb76Ogqy60wQ9YiZDbMpFltjx9bajrTMsoMmGF6CRZUc6GonxwgFpg2P

