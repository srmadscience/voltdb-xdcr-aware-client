load classes ../jars/xdcr-aware-server.jar;

CREATE PROCEDURE
   PARTITION ON TABLE xdcr_deployments COLUMN deployment_name
   FROM CLASS xdcr.server.GetSiteData;

