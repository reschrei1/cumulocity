package c8y.trackeragent_it;

import org.junit.Test;

import c8y.trackeragent.TrackerDevice;
import c8y.trackeragent.utils.Devices;
import c8y.trackeragent.utils.Positions;
import c8y.trackeragent.utils.Reports;

public class TelicReportIT extends TrackerITSupport {

    @Test
    public void shouldBootstrapNewDeviceAndThenChangeItsLocation() throws Exception {
        String imei = Devices.randomImei();
        System.out.println("imei " + imei);
        bootstrap(imei);  
        
        //trigger regular report 
        byte[] report = Reports.getTelicReportBytes(imei, Positions.SAMPLE_4);
        writeInNewConnection(report);
        
        Thread.sleep(1000);
        TrackerDevice newDevice = getTrackerDevice(imei);
        Positions.assertEqual(newDevice.getPosition(), Positions.SAMPLE_4);
    }

}
