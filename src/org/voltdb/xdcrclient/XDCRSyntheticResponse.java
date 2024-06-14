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


import org.voltdb.VoltTable;
import org.voltdb.client.ClientResponse;

public class XDCRSyntheticResponse implements ClientResponse {
    
    Exception m_exception = null;
    byte m_statusCode = ClientResponse.UNINITIALIZED_APP_STATUS_CODE;
    String m_statusMessage = null;
    
    public XDCRSyntheticResponse(Exception exception, byte statusCode, String statusMessage) {
        super();
        m_exception = exception;
        m_statusCode = statusCode;
        m_statusMessage = statusMessage;
    }


    @Override
    public byte getStatus() {
        
         return m_statusCode;
       
    }
    
    public int getStatusAsInt() {
        return m_statusCode;
    }
    
    @Override
    public String getStatusString() {
          return m_statusMessage;
    }

    
    @Override
    public byte getAppStatus() {
        return 0;
    }

    @Override
    public String getAppStatusString() {
        return null;
    }

    @Override
    public int getClientRoundtrip() {
        return 0;
    }

    @Override
    public long getClientRoundtripNanos() {
        return 0;
    }

    @Override
    public int getClusterRoundtrip() {
        return 0;
    }

    @Override
    public VoltTable[] getResults() {
        return null;
    }



}
