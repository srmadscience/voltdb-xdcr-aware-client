package org.voltdb.xdcrclient;

public class XDCRFutureInvocation {

    XCDRRetryCallback xcdrRetryCallback = null;
    XDCRProcedureInvocation m_invocation = null;
    long execTimeMs;

    public XDCRFutureInvocation(XCDRRetryCallback xcdrRetryCallback, XDCRProcedureInvocation m_invocation,
            long execTimeMs) {
        super();
        
        this.xcdrRetryCallback = xcdrRetryCallback;
        this.m_invocation = m_invocation;
        this.execTimeMs = execTimeMs;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "XDCRFutureInvocation [xcdrRetryCallback=" + xcdrRetryCallback + ", m_invocation=" + m_invocation
                + ", execTimeMs=" + execTimeMs + "]";
    }

}
