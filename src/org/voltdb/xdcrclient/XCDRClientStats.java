package org.voltdb.xdcrclient;

import java.util.ArrayList;
import java.util.Arrays;

public class XCDRClientStats {

   
    long[] successOnAttempt = null;
    long[] failOnAttempt = null;   
    long timeouts = 0;

    ArrayList<XDCRProcedureInvocation> failures = new ArrayList<XDCRProcedureInvocation>();

    public XCDRClientStats(int attempts) {
        super();
        this.successOnAttempt = new long[attempts];
        this.failOnAttempt = new long[attempts];

        for (int i = 0; i < successOnAttempt.length; i++) {
            successOnAttempt[i] = 0;
            failOnAttempt[i] = 0;
        }
    }

    public void reportSuccess(int attempt) {
        synchronized (successOnAttempt) {
            successOnAttempt[attempt-1]++;
        }
    }

    public void reportFailure(XDCRProcedureInvocation thatWhichFailed) {
        synchronized (failOnAttempt) {
            failOnAttempt[thatWhichFailed.getCurrentAttempt()-1]++;
            failures.add(thatWhichFailed);
        }
    }

    public void reportTimeout(XDCRProcedureInvocation thatWhichFailed) {
        synchronized (failOnAttempt) {
            timeouts++;
            failOnAttempt[thatWhichFailed.getCurrentAttempt()-1]++;
            failures.add(thatWhichFailed);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();

        sb.append("XCDRClientStats [successOnAttempt=");
        sb.append(Arrays.toString(successOnAttempt));
        sb.append(", failOnAttempt=");
        sb.append(Arrays.toString(failOnAttempt));
        sb.append(", timeouts=");
        sb.append(timeouts);
        sb.append(", failures="+failures.size());
        
//        for (int i=0; i < failures.size(); i++) {
//            sb.append(System.lineSeparator());
//            sb.append(failures.get(i));
//        }
        sb.append("]");

        return sb.toString();
    }

}
