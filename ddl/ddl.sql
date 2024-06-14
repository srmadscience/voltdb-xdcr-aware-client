
DROP PROCEDURE GetSiteData IF EXISTS;


DROP TABLE xdcr_deployments IF EXISTS;

CREATE table xdcr_deployments
(deployment_name varchar(20)not null
,deployment_version smallint not null
,primary key (deployment_name));


PARTITION TABLE xdcr_deployments ON COLUMN deployment_name;

DROP TABLE xdcr_sites IF EXISTS;

CREATE table xdcr_sites
(deployment_name varchar(20)not null
,site_name varchar(20) not null
,site_order smallint not null
,primary key (deployment_name,site_name));

PARTITION TABLE xdcr_sites ON COLUMN deployment_name;

CREATE UNIQUE INDEX sites_order_ix ON xdcr_sites (deployment_name, site_order);


DROP TABLE xdcr_site_hosts IF EXISTS;

create table xdcr_site_hosts
(deployment_name varchar(20)not null
,site_name varchar(20) not null
,site_hostname varchar(80) not null
,site_port     int not null
,primary key (deployment_name,site_name,site_hostname ));

PARTITION TABLE xdcr_site_hosts ON COLUMN deployment_name;

DROP TABLE xdcr_site_planned_outages IF EXISTS;

create table xdcr_site_planned_outages
(deployment_name varchar(20)not null
,site_name varchar(20) not null
,outage_start timestamp not null
,outage_end timestamp not null
,primary key (deployment_name,site_name,outage_start ));

PARTITION TABLE xdcr_site_planned_outages ON COLUMN deployment_name;

DR TABLE xdcr_deployments;
DR TABLE xdcr_sites;
DR TABLE xdcr_site_hosts;
DR TABLE xdcr_site_planned_outages;


load classes ../jars/xdcr-aware-server.jar;

CREATE PROCEDURE 
   PARTITION ON TABLE xdcr_deployments COLUMN deployment_name
   FROM CLASS xdcr.server.GetSiteData;
   

create table xdcr_arbitrary_transactions
(tran_id varchar(20) not null
,payload varchar(1024) not null
,primary key (tran_id));

PARTITION TABLE xdcr_arbitrary_transactions ON COLUMN tran_id;


DR TABLE xdcr_arbitrary_transactions;


