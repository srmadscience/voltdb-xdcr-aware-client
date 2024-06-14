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


public class XDCRInvocationFailure {
    
    public static final byte NO_CONNECTIONS  = -50;
    public static final byte IO_EXCEPTION  = -51;
    public static final byte BUSY_SIGNAL  = -52;
    
    
    String m_sitename;
    byte m_errorCode;
    long m_failureMs;
    long m_durationMs;
    
    public XDCRInvocationFailure(String sitename, byte errorCode, long failureMs, long durationMs) {
        super();
        this.m_sitename = sitename;
        this.m_errorCode = errorCode;
        this.m_failureMs = failureMs;
        this.m_durationMs = durationMs;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
       
        StringBuffer sb = new StringBuffer();
        
        sb.append('[');
        sb.append(m_sitename);
        sb.append(' ');
        
        sb.append(m_errorCode);
        sb.append(' ');
        
        sb.append(m_durationMs);
        sb.append(' ');
        
        sb.append(m_failureMs);
        sb.append(']');
        
        
        
        return sb.toString();
    }

}
