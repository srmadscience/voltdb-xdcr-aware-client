
DELETE FROM xdcr_deployments;
DELETE FROM xdcr_SITES;
DELETE FROM xdcr_site_hosts;
DELETE FROM xdcr_site_planned_outages;

INSERT INTO xdcr_deployments VALUES ('SHED',2);

INSERT INTO xdcr_sites VALUES ('SHED', 'jersey',1);
INSERT INTO xdcr_site_hosts VALUES ('SHED', 'jersey','192.168.0.14',21212);

INSERT INTO xdcr_sites VALUES ('SHED', 'badger',2);
INSERT INTO xdcr_site_hosts VALUES ('SHED', 'badger','192.168.0.15',21212);

INSERT INTO xdcr_sites VALUES ('SHED', 'rosal',3);
INSERT INTO xdcr_site_hosts VALUES ('SHED', 'rosal','192.168.0.16',21212);

INSERT INTO xdcr_site_planned_outages VALUES ('SHED', 'jersey', NOW, DATEADD(MINUTE,4,NOW));
INSERT INTO xdcr_site_planned_outages VALUES ('SHED', 'badger', NOW, DATEADD(MINUTE,3,NOW));

exec GetSiteData 'SHED' ;
