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

public class XDCRUnplannedOutage {

    private long m_outageStartTimeMS = 0;
    private long m_outageEndTimeMS = 0;

    public XDCRUnplannedOutage(long outageStartTimeMS) {
        super();
        this.m_outageStartTimeMS = outageStartTimeMS;
        this.m_outageEndTimeMS = outageStartTimeMS + (5 * 300 * 1000);
    }

    public boolean outageInEffect(long projectedTimeMS) {

        if (projectedTimeMS >= m_outageStartTimeMS && projectedTimeMS <= m_outageEndTimeMS) {
            return true;
        }

        return false;
    }

    public void outageOver() {
        m_outageEndTimeMS = System.currentTimeMillis() - 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "XDCRUnplannedOutage [m_outageStartTimeMS=" + m_outageStartTimeMS + ", m_outageEndTimeMS=" + m_outageEndTimeMS
                + " duration=" + (m_outageEndTimeMS - m_outageStartTimeMS) + "]";
    }

}
