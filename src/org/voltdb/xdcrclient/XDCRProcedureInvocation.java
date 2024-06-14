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


import java.util.ArrayList;
import java.util.Iterator;


/**
 * This class represents a requet to VoltDB, along with a history of
 * what has happened when we tried to run it before.
 * 
 * @author drolfe
 *
 */
public class XDCRProcedureInvocation {

    protected long m_startMs;
    protected String m_procname;
    protected Object[] m_procArgs;  
    protected int m_attempts;
    protected int m_currentAttempt;
    protected int m_backoffMs;
    protected long m_sellByTimeMs;
    protected ArrayList<XDCRInvocationFailure> failureHistory = new ArrayList<XDCRInvocationFailure>(0);
    protected boolean m_ranOutOfTime = false;
    private String m_LastSiteName = "Unfinished";
    private long m_lastCallStartMs = Long.MIN_VALUE;
    
    public XDCRProcedureInvocation(String m_procname, Object[] m_procArgs, int m_attempts, int m_backoffMs,
            long m_sellByTimeMs) {
        
        super();
        m_startMs = System.currentTimeMillis();
        this.m_procname = m_procname;
        this.m_procArgs = m_procArgs;
        this.m_attempts = m_attempts;
        m_currentAttempt = 0;
        this.m_backoffMs = m_backoffMs;
        this.m_sellByTimeMs = m_sellByTimeMs;
    }

    /**
     * @return the startMs
     */
    public long getStartMs() {
        return m_startMs;
    }

    /**
     * @return the procname
     */
    public String getProcname() {
        return m_procname;
    }

    /**
     * @return the procArgs
     */
    public Object[] getProcArgs() {
        return m_procArgs;
    }

    /**
     * @return the attempts
     */
    public int getAttempts() {
        return m_attempts;
    }

    /**
     * @return the currentAttempt
     */
    public int getCurrentAttempt() {
        return m_currentAttempt;
    }

    public boolean isOutOfAttempts() {
        if (m_currentAttempt >= m_attempts) {
            return true;
        }
        
        return false;
    }

    
    public void incAttempts() {
        m_currentAttempt++;
        
    }
    /**
     * @return the backoffMs
     */
    public int getBackoffMs() {
        return m_backoffMs;
    }

    /**
     * @return the sellByTimeMs
     */
    public long getSellByTimeMs() {
        return m_sellByTimeMs;
    }



    public boolean isPastSellByMs() {
        
        if (m_sellByTimeMs < System.currentTimeMillis()) {
            return true;
        }
        
        return false;
    }

    public void reportFailure(String siteName, byte status) {
        XDCRInvocationFailure latestFailure = new XDCRInvocationFailure(siteName, status, System.currentTimeMillis(), (System.currentTimeMillis() - m_lastCallStartMs));
        failureHistory.add(latestFailure);
        
    }

    public void reportSuccess(String siteName) {
        m_LastSiteName = siteName;
    }

    /**
     * @param m_ranOutOfTime the m_ranOutOfTime to set
     */
    public void setRanOutOfTime(boolean m_ranOutOfTime) {
        this.m_ranOutOfTime = m_ranOutOfTime;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(getProcname());
        for (int i = 0; i < m_procArgs.length; i++) {
            sb.append(" ");
            sb.append(m_procArgs[i]);
        }
        
        sb.append(" :");
        sb.append(m_startMs);
        sb.append(' ');
        
        sb.append(m_sellByTimeMs);
        sb.append(' ');
        
        sb.append(m_attempts);
        sb.append(' ');
        
        sb.append(m_currentAttempt);
        sb.append(' ');
        
        sb.append(m_backoffMs);
        sb.append(' ');
        
        sb.append(m_LastSiteName);
        sb.append(' ');
        
        sb.append(m_ranOutOfTime);
        sb.append(' ');
              
        for ( int i=0; i < failureHistory.size(); i++ ) {
            sb.append('[');
            sb.append(failureHistory.get(i));
            sb.append(']');
         }
        
        return sb.toString();


    }

    /**
     * @param m_LastSiteName the m_LastSiteName to set
     */
    public void setLastSiteName(String lastSiteName) {
        this.m_LastSiteName = lastSiteName;
    }

    /**
     * @return lastSiteName
     */
    public String getLastSiteName() {
        return m_LastSiteName;
    }

    /**
     * @param m_lastCallStartMs the m_lastCallStartMs to set
     */
    public void setLastCallStartMs(long lastCallStartMs) {
        this.m_lastCallStartMs = lastCallStartMs;
    }


    
  
    
}
