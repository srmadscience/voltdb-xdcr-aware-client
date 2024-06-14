package org.voltdb.xdcrclient;

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

import org.voltcore.logging.VoltLogger;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcedureCallback;

public class XCDRRetryCallback implements ProcedureCallback {

    /**
     * Logger.
     */
    protected static final VoltLogger logger = new VoltLogger("XCDRRetryCallback");

    /**
     * Object holding procedure name, params etc.
     */
    XDCRProcedureInvocation m_invocation = null;

    /**
     * The volt Client we'll use to insert the missing row
     */
    XDCRAwareClient m_voltClient = null;

    /**
     * What the user was trying to do when they issued this call..
     */
    ProcedureCallback m_originalCallback = null;

    XCDRClientStats m_stats = null;

    long m_retryIfDeploymentChanges;

    /**
     * Callback that will make multiple attempts to retry if needed.
     * 
     * @param m_originalCallback
     * @param procname
     * @param procArgs
     * @param theClient
     * @param attempts
     * @param backoffMs
     * @param retryIfDeploymentChanges
     * @param sellByDateMs
     * @param m_stats
     */
    public XCDRRetryCallback(ProcedureCallback originalCallback, String procname, Object[] procArgs,
            XDCRAwareClient theClient, int attempts, int backoffMs, long timeoutMs, XCDRClientStats stats,
            int retryIfDeploymentChanges) {

        super();

        long sellByDateMs = System.currentTimeMillis() + timeoutMs;

        if (retryIfDeploymentChanges > 0) {
            // It looks like we'll be conditionally executed in the future.
            // Tweak
            // date accordingly.
            sellByDateMs += retryIfDeploymentChanges;
        }

        m_invocation = new XDCRProcedureInvocation(procname, procArgs, attempts, backoffMs, sellByDateMs);

        m_originalCallback = originalCallback;
        m_voltClient = theClient;
        m_invocation.incAttempts();
        m_stats = stats;
        m_retryIfDeploymentChanges = retryIfDeploymentChanges;

    }

    @Override
    public void clientCallback(ClientResponse latestResponse) throws Exception {

        m_voltClient.reportStatus(m_invocation.getLastSiteName(), latestResponse.getStatus());

        if (latestResponse.getStatus() == ClientResponse.SUCCESS) {

            m_stats.reportSuccess(m_invocation.getCurrentAttempt());
            m_originalCallback.clientCallback(latestResponse);

        } else {

            logger.error("Error Code " + latestResponse.getStatus() + ":" + latestResponse.getStatusString());

            if (latestResponse instanceof XDCRSyntheticResponse) {
                // We failed when we tried to queue this originally. inc
                // Attempts
                // so the stats make sense...
                m_invocation.incAttempts();
            }

            m_invocation.reportFailure(m_invocation.getLastSiteName(), latestResponse.getStatus());

            if (m_invocation.isPastSellByMs()) {

                XDCRTimeoutException tException = new XDCRTimeoutException(m_invocation,
                        latestResponse.getStatusString());
                m_stats.reportTimeout(m_invocation);
                throw (tException);

            } else if (m_invocation.isOutOfAttempts()) {

                XDCRInvocationException tException = new XDCRInvocationException(m_invocation, "Out of Attempts");
                m_stats.reportFailure(m_invocation);
                throw (tException);

            }

            m_invocation.incAttempts();
            m_voltClient.callProcedureInFuture(this);

        }

    }

    /**
     * @param m_siteName
     *            the m_siteName to set
     */
    public void noteStartOfCall(String siteName) {
        m_invocation.setLastSiteName(siteName);
        m_invocation.setLastCallStartMs(System.currentTimeMillis());
    }

    /**
     * Return invocation object.
     * 
     * @return
     */
    public XDCRProcedureInvocation getInvocation() {
        return m_invocation;
    }

    /**
     * @return the m_retryIfDeploymentChanges
     */
    public long getRetryIfDeploymentChanges() {
        return m_retryIfDeploymentChanges;
    }

}
