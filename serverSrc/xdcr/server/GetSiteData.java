package xdcr.server;
 //     org.voltdb.xdcrsvr.

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class GetSiteData extends VoltProcedure {
    
    public static final SQLStmt getDeployment = new SQLStmt("SELECT * FROM xdcr_deployments          WHERE deployment_name = ?;");
    public static final SQLStmt getSites = new SQLStmt("SELECT * FROM xdcr_sites                WHERE deployment_name = ? ORDER BY site_order;");
    public static final SQLStmt getSiteHosts = new SQLStmt("SELECT * FROM xdcr_site_hosts           WHERE deployment_name = ? ORDER BY site_name,site_hostname ;");
    public static final SQLStmt getPlannedOutages = new SQLStmt("SELECT * FROM xdcr_site_planned_outages WHERE deployment_name = ? ORDER BY site_name,outage_start ;");

    public VoltTable[] run(String deploymentName) throws VoltAbortException {
        
        voltQueueSQL(getDeployment,deploymentName ); 
        voltQueueSQL(getSites,deploymentName ); 
        voltQueueSQL(getSiteHosts,deploymentName ); 
        voltQueueSQL(getPlannedOutages,deploymentName ); 
        
       return  voltExecuteSQL(true);
   
    }
}
