package org.voltdb.xdcrclient;

public interface XDCREventListener {

    public void deploymentHasBeenChosen(String deploymentName);
}
