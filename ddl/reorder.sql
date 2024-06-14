

UPDATE xdcr_sites SET site_order = 4 WHERE site_order = 1;
UPDATE xdcr_deployments SET deployment_version = 42;

exec getSiteData 'SHED' 'SHED' 'SHED' 'SHED';
