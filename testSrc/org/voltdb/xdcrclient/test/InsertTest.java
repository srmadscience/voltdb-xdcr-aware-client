package org.voltdb.xdcrclient.test;

import java.util.Random;

import org.voltdb.xdcrclient.XDCRAwareClient;

public class InsertTest {

    public static void main(String[] args) {

        Random r = new Random(42);
        
        XDCRAwareClient c;
        try {
            c = new XDCRAwareClient("SHED", "192.168.0.16", 21212,1,100,System.currentTimeMillis() + 100000);
            
            long lastStatsReport = System.currentTimeMillis();
            int lastI = 0;

            CountErrorCallback cec  = new CountErrorCallback();
            
            for (int i = 0; i < 1000000; i++) {

        
                c.callProcedure(cec, "xdcr_arbitrary_transactions.DELETE", i+"" );
         
            }

            
            c.drain();
            
            System.out.println("deletes done, errors =" + cec.getErrorCount());
                        Thread.sleep(10000);
            System.out.println("starting upserts");
            
 
          
            for (int i = 0; i < 1000000; i++) {

                ComplainOnErrorCallback coec = new ComplainOnErrorCallback();

                //c.callProcedure(coec, "GetSiteData", "SHED");
                c.callProcedure(coec, "xdcr_arbitrary_transactions.INSERT", i+"", "Row " + i);
                               // System.out.println(cr.getResults()[0].toFormattedString());
                
                // exec @Statistics DRConsumerNode 1;
                // exec @Statistics DRPRODUCERPARTITION 1;
                //DRPRODUCERNODE
                // 
                
                if (i == 200000) {
                    System.out.println("NOW");
                    Thread.sleep(10000);
                }
            
   
                
                try {
                    if (r.nextInt(10) == 0 ) {
                    Thread.sleep(1);}
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            c.drain();
            System.out.println("upserts done, errors =" + cec.getErrorCount());
            System.out.println(c.toString());
            
            c.close();
            

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            c.close();

        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

}
