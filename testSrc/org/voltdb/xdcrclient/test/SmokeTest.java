package org.voltdb.xdcrclient.test;

import java.util.Random;

import org.voltdb.xdcrclient.XDCRAwareClient;

public class SmokeTest {

    public static void main(String[] args) {

        Random r = new Random(42);
        
        XDCRAwareClient c;
        try {
            c = new XDCRAwareClient("SHED", "192.168.0.16", 21212,1,100,System.currentTimeMillis() + 100000);
            
            long lastStatsReport = System.currentTimeMillis();
            int lastI = 0;
            

            for (int i = 0; i < 6000000; i++) {

                ComplainOnErrorCallback coec = new ComplainOnErrorCallback();

                //c.callProcedure(coec, "GetSiteData", "SHED");
                c.callProcedure(coec, "xdcr_arbitrary_transactions.UPSERT", r.nextInt(1000000)+"", "Row " + r.nextInt(100000));
                               // System.out.println(cr.getResults()[0].toFormattedString());
                
                // exec @Statistics DRConsumerNode 1;
                // exec @Statistics DRPRODUCERPARTITION 1;
                //DRPRODUCERNODE
                // 
                try {
                    if (r.nextInt(10) == 0 ) {
                    Thread.sleep(1);}
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                if (i % 100000 == 0) {
                    System.out.println("i = " + i);
                }
            }
            
            c.drain();

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
