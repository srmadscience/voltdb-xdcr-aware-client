package org.voltdb.xdcrclient;

import java.io.IOException;

/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


import java.util.ArrayList;
import java.util.Iterator;

import org.voltcore.logging.VoltLogger;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;

public class XDCRSite {

    /**
     * Logger.
     */
    protected static final VoltLogger logger = new VoltLogger("XDCRSite");

    private static final long DB_TIMEOUT_MS = 1000;

    private static final long SERVER_UNAVAILABLE_RETRY_MS = 5 * 60 * 1000;

    private String m_siteName = null;

    private ArrayList<XDCRHost> m_deploymentHosts = new ArrayList<XDCRHost>();

    private ArrayList<XDCRPlannedOutage> m_plannedOutages = new ArrayList<XDCRPlannedOutage>();

    private ArrayList<XDCRUnplannedOutage> m_UnplannedOutages = new ArrayList<XDCRUnplannedOutage>();

    private Client m_voltClient = null;
    
    private long lastServerUnavailableMs = 0;

    boolean m_debug = true;

    public XDCRSite(String siteName) {

        super();
        this.m_siteName = siteName;
    }

    public boolean isPlannedOutageInEffect(long projectedTimeMS) {

        for (int i = 0; i < m_plannedOutages.size(); i++) {
            if (m_plannedOutages.get(i).outageInEffect(projectedTimeMS)) {
                return true;
            }
        }

        return false;
    }

    public boolean isUnplannedOutageInEffect(long projectedTimeMS) {

        for (int i = 0; i < m_UnplannedOutages.size(); i++) {
            
            if (m_UnplannedOutages.get(i).outageInEffect(projectedTimeMS)) {

                return true;
                
             }
        }

        return false;
    }

    /**
     * @return the m_siteName
     */
    public String getSiteName() {
        return m_siteName;
    }

    public void addDeploymentHost(XDCRHost host) {
        m_deploymentHosts.add(host);
    }

    /**
     * @return the m_deploymentHosts
     */
    public XDCRHost[] getDeploymentHosts() {

        XDCRHost[] returnArray = new XDCRHost[m_deploymentHosts.size()];
        return m_deploymentHosts.toArray(returnArray);
    }

    /**
     * @return the m_voltClient
     */
    public Client getVoltClient() {
        return m_voltClient;
    }

    public boolean isUp() {

        if (isPlannedOutageInEffect(System.currentTimeMillis())) {
            return false;
        }

        if (isUnplannedOutageInEffect(System.currentTimeMillis())) {
            return false;
        }

        if (m_voltClient == null) {
            return false;
        }

        if (m_voltClient.getConnectedHostList().size() < 1) {
            return false;
        }

        return true;
    }

    public void addPlannedOutage(XDCRPlannedOutage xdcrPlannedOutage) {
        m_plannedOutages.add(xdcrPlannedOutage);

    }

    public Client connectVoltDB(XDCRHost[] hosts) throws Exception {
        
        if (lastServerUnavailableMs + SERVER_UNAVAILABLE_RETRY_MS > System.currentTimeMillis()) {
            return null;
        }
        
        Client client = null;
        ClientConfig config = null;

        try {
            logger.debug("Logging into VoltDB");

            config = new ClientConfig(); // "admin", "idontknow");
            config.setMaxOutstandingTxns(5000);
            config.setMaxTransactionsPerSecond(200000);
            config.setTopologyChangeAware(true);
            config.setReconnectOnConnectionLoss(true);
            config.setConnectionResponseTimeout(DB_TIMEOUT_MS);
            config.setProcedureCallTimeout(DB_TIMEOUT_MS);
            config.setHeavyweight(true);

            client = ClientFactory.createClient(config);

            for (int i = 0; i < hosts.length; i++) {
                logger.debug("Connect to " + hosts[i].getName() + "...");
                try {
                    client.createConnection(hosts[i].getName(), hosts[i].getPort());
                } catch (Exception e) {
                    logger.error("Connect to " + hosts[i].getName() + " failed:" + e.getMessage());
                }
            }
            
            sanityCheckClient(client);
            
           

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("VoltDB connection failed.." + e.getMessage(), e);
        }

        return client;

    }

    private boolean sanityCheckClient(Client client) {
                
        boolean ok = true;
        
        System.out.println("sanityCheckClient for " + this.getSiteName());
        
                try {
                    ClientResponse cr = client.callProcedure("GetSiteData", "FRED"); //TODO
                    if (cr.getStatus() != ClientResponse.SUCCESS ) {
                        System.err.println("Bad=" + cr.getStatus());
                        ok=false;
                        lastServerUnavailableMs = Long.MAX_VALUE - SERVER_UNAVAILABLE_RETRY_MS;
                    }
                    
                } catch (NoConnectionsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    ok=false;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    ok=false;
                } catch (ProcCallException e) {
                    // TODO Auto-generated catch block
                    // ClientStatus.SERVER_UNAVAILABLE ends up here.
                   // e.printStackTrace();
                    ok=false;
                    lastServerUnavailableMs = System.currentTimeMillis();
                    
                    if (! isUnplannedOutageInEffect(System.currentTimeMillis())) {
                        reportsError(ClientResponse.SERVER_UNAVAILABLE);
                    }
                            
                            
                           
                }
                
                
                
            return ok;    

            
    }

    public void checkConnections() {

        if (m_debug) {
            logger.debug("Confirming connection to " + toString());
        }

        if (isPlannedOutageInEffect(System.currentTimeMillis())) {

            if (m_voltClient != null) {

                if (m_debug) {
                    logger.debug("Disconnecting from  connection to " + toString() + " as planned outage starting");
                }

                close();
            }

        } else if (isUnplannedOutageInEffect(System.currentTimeMillis())) {

   
        } else if (m_voltClient == null) {

            try {
                if (m_debug) {
                    logger.debug("Tring to connect to " + toString());
                }
                m_voltClient = connectVoltDB(getDeploymentHosts());
                
            } catch (Exception e) {
                logger.error(e);
            }

        }

    }

    public void close() {
        if (m_voltClient != null) {
            try {
                m_voltClient.drain();
               
            } catch (NoConnectionsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
            
                m_voltClient.close();
            }catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            m_voltClient = null;

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String isUpOrDown = "(Down)";
        
        if (isUp()) {
            isUpOrDown = "(Up)";
        }
        
        return "XDCRSite " + m_siteName + " " + isUpOrDown;
    }

    public void reportsError(byte status) {
        if (status == ClientResponse.SERVER_UNAVAILABLE) {
           
            logger.error(this.toString() + " is unavailable");
            m_UnplannedOutages.add(new XDCRUnplannedOutage(System.currentTimeMillis()));
            lastServerUnavailableMs = System.currentTimeMillis();
            close();
        }

    }
    
    public void showStatus() {
        
        StringBuffer sb = new StringBuffer(toString());
       
        Iterator<XDCRHost> i = m_deploymentHosts.iterator();
        Iterator<XDCRPlannedOutage> i2 = m_plannedOutages.iterator();
        Iterator<XDCRUnplannedOutage> i3 = m_UnplannedOutages.iterator();

        
        sb.append(" hosts=[");       
        while (i.hasNext()) {
            
            sb.append(i.next().m_hostname);
            sb.append(' ');
         }
        
        sb.append("] m_plannedOutages=[");

       while (i2.hasNext()) {
            
            sb.append(i2.next().toString());
            sb.append(' ');
         }
         
       sb.append("] ");
       sb.append("unplannedoutages=[");

      while (i3.hasNext()) {
           
           sb.append(i3.next().toString());
           sb.append(' ');
        }
        
      sb.append(']');
        System.out.println(sb.toString());
        
    }

    public void drain() {
        if (m_voltClient != null) {
            try {
                m_voltClient.drain();
            } catch (NoConnectionsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
        }
        
    }

}
