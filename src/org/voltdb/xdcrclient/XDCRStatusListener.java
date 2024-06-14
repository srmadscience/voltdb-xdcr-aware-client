/**
 * 
 */
package org.voltdb.xdcrclient;

import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.ProcedureCallback;

/**
 * @author drolfe
 *
 */
public class XDCRStatusListener extends ClientStatusListenerExt {

 boolean backpressure = false;
 

    @Override
    public void backpressure(boolean status) {
        backpressure = status;     
    }

//    @Override
//    public void connectionCreated(String hostname, int port, AutoConnectionStatus status) {
//        // TODO Auto-generated method stub
//        super.connectionCreated(hostname, port, status);
//    }
//
//    @Override
//    public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
//        // TODO Auto-generated method stub
//        super.connectionLost(hostname, port, connectionsLeft, cause);
//    }

    @Override
    public void lateProcedureResponse(ClientResponse r, String hostname, int port) {
    }

    @Override
    public void uncaughtException(ProcedureCallback callback, ClientResponse r, Throwable e) {
    }

    /**
     * @return the backpressure
     */
    public boolean isBackpressure() {
        return backpressure;
    }

  
}
