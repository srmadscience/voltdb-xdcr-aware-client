package org.voltdb.xdcrclient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.voltcore.logging.VoltLogger;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;
import org.voltdb.client.ProcedureCallback;
import org.voltdb.voltutil.stats.SafeHistogramCache;

import com.sun.swing.internal.plaf.synth.resources.synth_zh_CN;

public class XDCRAwareClient {

    public static final int DEPLOYMENT_CHANGE_REDO_MS = 10000;

    /**
     * Logger.
     */
    protected static final VoltLogger logger = new VoltLogger("XDCRAwareClient");

    /**
     * Inner class that runs in the background. Responsible for checking for
     * metadata updates and maintaining connectivity.
     */
    XDCRAwareClientDaemon m_daemon = new XDCRAwareClientDaemon(this);

    /**
     * Array containing sites in order of precedence.
     */
    XDCRSite[] m_sites = null;

    /**
     * How long m_daemon sleeps before waking up
     */
    long m_sleepMs = 1000;

    /**
     * How often we check for meta data changes
     */
    long m_metaDataCheckIntervalMs = 60000;

    /**
     * How often we check for deployment changes
     */
    long CONFIG_CHECK_INTERVAL_MS = 60000;

    /**
     * Last time we updated meta data
     */
    long m_lastMetaDataUpdate = Long.MIN_VALUE;

    /**
     * Name of deployment
     */
    String m_deploymentName = null;

    /**
     * Version of deployment. Note that we will update metadata if this changes
     * for any reason and in any direction.
     */
    long m_deploymentVersion = Long.MIN_VALUE;

    /**
     * Last time we checked for a new deployment
     */
    long m_deploymentVersionCheckTime = Long.MIN_VALUE;

    /**
     * Comma delimited list of hostnames to speak to to get started
     */
    String m_bootstrapHosts = null;

    /**
     * m_port id for bootstrap hosts
     */
    int m_bootStrapPort = 21212;

    int m_attempts;

    int m_backoff_ms;

    int m_timeoutMs;

    XCDRClientStats m_stats = null;

    ArrayList<XDCRFutureInvocation> futureRetryInvocations = new ArrayList<XDCRFutureInvocation>();

    ArrayList<XDCRFutureInvocation> futureDeploymentChangeInvocations = new ArrayList<XDCRFutureInvocation>();

    int m_lastDeploymentId = -1;

    long m_lastDeploymentChangeTime = System.currentTimeMillis() - DEPLOYMENT_CHANGE_REDO_MS;

    XDCREventListener m_listener;

    BufferedWriter w;

    SafeHistogramCache shc = SafeHistogramCache.getInstance();

    public XDCRAwareClient(String deploymentName, String bootstrapHosts, int bootstrapPort, int attempts, int backoffMs,
            int timeoutMs) throws Exception {

        SimpleDateFormat sdfDate = new SimpleDateFormat("ddHHmmss");
        Date now = new Date();
        String filename = "xdcrclient_" + sdfDate.format(now) + ".log";

        String filedir = "/Users/drolfe/Desktop/EclipseWorkspace/voltdb-xdcr-aware-client/testlogs";

        w = new BufferedWriter(new FileWriter(filedir + File.separator + filename));

        m_deploymentName = deploymentName;
        m_bootstrapHosts = bootstrapHosts;
        m_bootStrapPort = bootstrapPort;

        m_attempts = attempts;
        m_backoff_ms = backoffMs;
        m_timeoutMs = timeoutMs;

        m_stats = new XCDRClientStats(attempts);

        // Attempt to get meta data - abort if not available
        if (!checkForDeploymentChanges()) {
            throw new Exception("No database is contactable");
        }

        // Log in..
        m_daemon.doScheduledTasks();

        Thread thread = new Thread(m_daemon);
        thread.start();

    }

    public boolean callProcedure(ProcedureCallback callback, java.lang.String procName, java.lang.Object... parameters)
            throws Exception {
        return callProcedure(callback, -1, procName, parameters);
    }

    /**
     * Make multiple attempts to call a procedure until we run out of time or
     * attempts...
     * 
     * @param callback
     *            The actual callback we want executed.
     * @param retryInXMSIfDeploymentChanges
     *            A non zero value means we want to call this procedure twice if
     *            there is a deployment change, as a site may have gone down.
     * @param procName
     * @param parameters
     * @throws Exception
     */
    public boolean callProcedure(ProcedureCallback callback, int retryInXMSIfDeploymentChanges,
            java.lang.String procName, java.lang.Object... parameters) throws Exception {

        XCDRRetryCallback xCallback;

        // If this is the first time we've been called for this invocation wrap
        // the call in
        // an XCDRRetryCallback...
        if (callback instanceof XCDRRetryCallback) {
            xCallback = (XCDRRetryCallback) callback;
        } else {
            xCallback = new XCDRRetryCallback(callback, procName, parameters, this, m_attempts, m_backoff_ms,
                    m_timeoutMs, m_stats, retryInXMSIfDeploymentChanges);
        }

        if (retryInXMSIfDeploymentChanges > 0) {
            XCDRRetryCallback deploymentChangeCallback = new XCDRRetryCallback(callback, procName, parameters, this,
                    m_attempts, m_backoff_ms, m_timeoutMs, m_stats, retryInXMSIfDeploymentChanges);
            callProcedureInFutureIfDeploymentChanges(deploymentChangeCallback);
        }

        // See if there are any usable clients...
        XDCRClientWithSitename c = getBestClient();

        if (c == null) {
            throw new XDCRInvocationException(xCallback.getInvocation(), "No Clients Available");
        }

        // Callback needs to know which site is in use so blame can be allocated
        xCallback.noteStartOfCall(c.getSitename());

        try {
            boolean procedureQueued = c.getClient().callProcedureWithTimeout(xCallback, m_timeoutMs, procName,
                    parameters);

            if (!procedureQueued) {
                XDCRSyntheticResponse fakeResponse = new XDCRSyntheticResponse(null, XDCRInvocationFailure.BUSY_SIGNAL,
                        "BUSY_SIGNAL to " + c.getSitename());
                xCallback.clientCallback(fakeResponse);
            }
        } catch (NoConnectionsException e) {
            XDCRSyntheticResponse fakeResponse = new XDCRSyntheticResponse(e, XDCRInvocationFailure.NO_CONNECTIONS,
                    "NO_CONNECTIONS to " + c.getSitename());
            xCallback.clientCallback(fakeResponse);
        } catch (IOException e) {
            XDCRSyntheticResponse fakeResponse = new XDCRSyntheticResponse(e, XDCRInvocationFailure.IO_EXCEPTION,
                    "IO_EXCEPTION with " + c.getSitename());
            xCallback.clientCallback(fakeResponse);
        }

        return true;

    }

    /**
     * @param deploymentName
     * @param bootstrapHosts
     * @param bootStrapport
     */
    boolean checkForDeploymentChanges() {

        boolean ok = true;

        if (m_deploymentVersionCheckTime + CONFIG_CHECK_INTERVAL_MS < System.currentTimeMillis()) {

            m_deploymentVersionCheckTime = System.currentTimeMillis();

            try {
                Client bootStrapClient = connectVoltDB(m_bootstrapHosts, m_bootStrapPort);
                ClientResponse cr = bootStrapClient.callProcedure("GetSiteData", m_deploymentName);

                VoltTable deploymentTable = cr.getResults()[0];
                VoltTable siteTable = cr.getResults()[1];
                VoltTable siteHostTable = cr.getResults()[2];
                VoltTable siteOutageTable = cr.getResults()[3];

                if (deploymentTable.advanceRow()) {

                    synchronized (m_daemon) {

                        long latestDeploymentVersion = deploymentTable.getLong("deployment_version");
                        if (m_deploymentVersion != latestDeploymentVersion) {

                            m_lastDeploymentId = -1;
                            m_deploymentVersion = latestDeploymentVersion;
                            m_lastDeploymentChangeTime = System.currentTimeMillis();
                            ;

                            // Drain old m_sites table
                            drainAndCloseAllConnections();

                            m_sites = new XDCRSite[siteTable.getRowCount()];

                            if (m_sites.length == 0) {
                                throw new Exception("No Sites found");
                            }

                            while (siteTable.advanceRow()) {
                                m_sites[siteTable.getActiveRowIndex()] = new XDCRSite(siteTable.getString("site_name"));

                                siteHostTable.resetRowPosition();
                                while (siteHostTable.advanceRow()) {

                                    if (siteHostTable.getString("site_name")
                                            .equalsIgnoreCase(m_sites[siteTable.getActiveRowIndex()].getSiteName())) {

                                        m_sites[siteTable.getActiveRowIndex()].addDeploymentHost(
                                                new XDCRHost(siteHostTable.getString("site_hostname"),
                                                        (int) siteHostTable.getLong("site_port")));

                                    }

                                }

                                siteOutageTable.resetRowPosition();
                                while (siteOutageTable.advanceRow()) {
                                    if (siteOutageTable.getString("site_name")
                                            .equalsIgnoreCase(m_sites[siteTable.getActiveRowIndex()].getSiteName())) {
                                        m_sites[siteTable.getActiveRowIndex()].addPlannedOutage(new XDCRPlannedOutage(
                                                siteOutageTable.getTimestampAsTimestamp("outage_start"),
                                                siteOutageTable.getTimestampAsTimestamp("outage_end")));

                                    }

                                }

                            }

                            bootStrapClient.close();

                        }
                    }

                } else {
                    ok = false;
                    throw new Exception("No Sites found");
                }

            } catch (Exception e) {
                ok = false;
                logger.error(e);
            }
        }

        return ok;
    }

    public void reportStatus(String siteName, byte status) {

        if (status != ClientResponse.SUCCESS) {

            int siteArrayIndex = getSitePositionInArray(siteName);
            assert siteArrayIndex > -1 : "Site not found";

            m_sites[siteArrayIndex].reportsError(status);

        }

    }

    private int getSitePositionInArray(String siteName) {

        for (int i = 0; i < m_sites.length; i++) {
            if (m_sites[i].getSiteName().equalsIgnoreCase(siteName)) {
                return i;
            }
        }
        return -1;
    }

    public XDCRClientWithSitename getBestClient() {

        synchronized (m_daemon) {

            if (m_sites != null) {
                for (int i = 0; i < m_sites.length; i++) {
                    if (m_sites[i].isUp()) {
                        XDCRClientWithSitename client = new XDCRClientWithSitename(m_sites[i].getSiteName(),
                                m_sites[i].getVoltClient());

                        if (m_lastDeploymentId == -1) {
                            logger.info("Picking deployment " + m_sites[i].getSiteName());
                            if (m_listener != null) {
                                m_listener.deploymentHasBeenChosen(m_sites[i].getSiteName());
                            }
                        } else {
                            if (i != m_lastDeploymentId) {
                                logger.info("Switching from  deployment " + m_sites[m_lastDeploymentId].getSiteName()
                                        + " to " + m_sites[i].getSiteName());
                                if (m_listener != null) {
                                    m_listener.deploymentHasBeenChosen(m_sites[i].getSiteName());
                                }
                            }

                        }
                        m_lastDeploymentId = i;
                        return client;
                    }
                }
            }
        }
        return null;

    }

    void lookAfterConnections() {

        if (m_sites != null) {
            for (int i = 0; i < m_sites.length; i++) {
                m_sites[i].checkConnections();
            }
        }

    }

    void drainAndCloseAllConnections() throws Exception {

        if (m_sites != null) {
            for (int i = 0; i < m_sites.length; i++) {

                if (m_sites[i].getVoltClient() != null) {
                    try {
                        m_sites[i].close();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public static Client connectVoltDB(String hostname, int port) throws Exception {
        Client client = null;
        ClientConfig config = null;

        try {
            logger.debug("Logging into VoltDB");

            config = new ClientConfig(); // "admin", "idontknow");
            config.setMaxOutstandingTxns(20000);
            config.setMaxTransactionsPerSecond(200000);
            config.setTopologyChangeAware(true);
            config.setReconnectOnConnectionLoss(true);

            client = ClientFactory.createClient(config);

            String[] hostnameArray = hostname.split(",");

            for (int i = 0; i < hostnameArray.length; i++) {
                logger.debug("Connect to " + hostnameArray[i] + "...");
                try {
                    client.createConnection(hostnameArray[i], port);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("VoltDB connection failed.." + e.getMessage(), e);
        }

        return client;

    }

    public void close() {
        m_daemon.keepGoing = false;

    }

    class XDCRAwareClientDaemon implements Runnable {

        boolean keepGoing = true;
        XDCRAwareClient xdcrAwareClient = null;
        final int SLEEP_MS = 50;

        public XDCRAwareClientDaemon(XDCRAwareClient xdcrAwareClient) {
            this.xdcrAwareClient = xdcrAwareClient;
        }

        @Override
        public void run() {
            while (keepGoing) {

                doScheduledTasks();
            }

            xdcrAwareClient.close();

        }

        /**
         * 
         */
        void doScheduledTasks() {
            try {
                logger.debug("doScheduledTasks() called");

                xdcrAwareClient.checkForDeploymentChanges();
                xdcrAwareClient.lookAfterConnections();
                xdcrAwareClient.processFutureInvocations(futureRetryInvocations, false);
                xdcrAwareClient.processFutureInvocations(futureDeploymentChangeInvocations, true);

                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }
    }

    public void drain() {
        if (m_sites != null) {

            for (int i = 0; i < m_sites.length; i++) {
                m_sites[i].drain();
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "XDCRAwareClient [m_sites=" + Arrays.toString(m_sites) + ", m_stats=" + m_stats + "]";
    }

    public void callProcedureInFuture(XCDRRetryCallback xcdrRetryCallback) {

        long targetMs = System.currentTimeMillis() + xcdrRetryCallback.getInvocation().getBackoffMs();

        XDCRFutureInvocation fi = new XDCRFutureInvocation(xcdrRetryCallback, xcdrRetryCallback.getInvocation(),
                targetMs);

        synchronized (futureRetryInvocations) {
            futureRetryInvocations.add(fi);
        }

    }

    public void callProcedureInFutureIfDeploymentChanges(XCDRRetryCallback xcdrRetryCallback) {

        long targetMs = System.currentTimeMillis();
        targetMs += xcdrRetryCallback.getRetryIfDeploymentChanges();

        XDCRFutureInvocation fi = new XDCRFutureInvocation(xcdrRetryCallback, xcdrRetryCallback.getInvocation(),
                targetMs);

        synchronized (futureDeploymentChangeInvocations) {
            futureDeploymentChangeInvocations.add(fi);
        }

    }

    public void processFutureInvocations(ArrayList<XDCRFutureInvocation> invocations,
            boolean onlyIfRecentDeploymentChange) {

        final long mustFinishByMs = System.currentTimeMillis() + 100;
        String name = "processFutureInvocations";

        if (onlyIfRecentDeploymentChange) {
            name = name + " (Cluster Changed)";
        } else {
            name = name + " (Node Lost)";
        }

        boolean workToDo = false;

        synchronized (invocations) {

            if (invocations.size() > 0 && invocations.get(0).execTimeMs <= System.currentTimeMillis()) {
                workToDo = true;
                shc.incCounter("processFutureInvocations" + onlyIfRecentDeploymentChange + "_wakeups");
            }
        }

        while (workToDo) {

            XDCRFutureInvocation fi = null;
            XDCRClientWithSitename nextBestClient = this.getBestClient();

            if (nextBestClient != null) {

                synchronized (invocations) {
                    fi = invocations.remove(0);
                }

                if (!onlyIfRecentDeploymentChange || (onlyIfRecentDeploymentChange && deploymentHasRecentlyChanged())) {

                    try {
                        // Retry call - it might work now.
                        try {

                            fi.xcdrRetryCallback.noteStartOfCall(nextBestClient.getSitename());

                            shc.incCounter("processFutureInvocations" + onlyIfRecentDeploymentChange + "_calls");
                            msg(name + ": re-do " + fi.toString());
                            // TODO : needs timeout...

                            nextBestClient.getClient().callProcedure(fi.xcdrRetryCallback,
                                    fi.m_invocation.getProcname(), fi.m_invocation.getProcArgs());

                        } catch (NoConnectionsException e) {
                            XDCRSyntheticResponse fakeResponse = new XDCRSyntheticResponse(e,
                                    XDCRInvocationFailure.NO_CONNECTIONS,
                                    "PFI: NO_CONNECTIONS to " + nextBestClient.getSitename());
                            fi.xcdrRetryCallback.clientCallback(fakeResponse);
                        } catch (IOException e) {
                            XDCRSyntheticResponse fakeResponse = new XDCRSyntheticResponse(e,
                                    XDCRInvocationFailure.IO_EXCEPTION,
                                    "PFI: IO_EXCEPTION with " + nextBestClient.getSitename());
                            fi.xcdrRetryCallback.clientCallback(fakeResponse);

                        }
                    } catch (Exception e) {

                        logger.error(fi);
                        logger.error(e);
                        msg(fi + " " + e.getMessage());
                    }
                } else {
                    shc.incCounter("processFutureInvocations" + onlyIfRecentDeploymentChange + "_discardentry");
                }

                synchronized (invocations) {
                    if (invocations.size() > 0 && invocations.get(0).execTimeMs <= System.currentTimeMillis()
                            && mustFinishByMs > System.currentTimeMillis()) {
                        workToDo = true;
                    } else {
                        workToDo = false;
                    }
                }

            } else {
                workToDo = false;
            }

        }

    }

    private boolean deploymentHasRecentlyChanged() {

        if (m_lastDeploymentChangeTime + DEPLOYMENT_CHANGE_REDO_MS > System.currentTimeMillis()) {
            return true;
        }

        return false;
    }

    /**
     * @param m_listener
     *            the m_listener to set
     */
    public void setListener(XDCREventListener m_listener) {
        this.m_listener = m_listener;
    }

    public void msg(String message) {

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + message);
        try {
            synchronized (w) {
                w.write(strDate + ":" + message + System.lineSeparator());
                w.flush();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public int getTaskCount() {

        int totalTasks = 0;

        synchronized (futureRetryInvocations) {
            totalTasks += futureRetryInvocations.size();
        }

        synchronized (futureRetryInvocations) {
            totalTasks += futureDeploymentChangeInvocations.size();
        }

        return totalTasks;

    }

    public void forgetFutureInvocations() {

        synchronized (futureRetryInvocations) {
            futureRetryInvocations.clear();
        }

        synchronized (futureRetryInvocations) {
            futureDeploymentChangeInvocations.clear();
        }

    }

}
