package c8y.trackeragent.protocol.queclink;

import c8y.Hardware;
import c8y.Tracking;
import c8y.trackeragent.TrackerAgent;
import c8y.trackeragent.context.OperationContext;
import c8y.trackeragent.context.ReportContext;
import c8y.trackeragent.device.TrackerDevice;
import c8y.trackeragent.protocol.queclink.parser.QueclinkDeviceSetting;
import c8y.trackeragent.server.TestConnectionDetails;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;

import static c8y.trackeragent.protocol.TrackingProtocol.QUECLINK;

public class QueclinkDeviceSettingTest {

    private TrackerAgent trackerAgent = mock(TrackerAgent.class);
    private TrackerDevice trackerDevice = mock(TrackerDevice.class);
    private ManagedObjectRepresentation managedObject = mock(ManagedObjectRepresentation.class);
    private TestConnectionDetails connectionDetails;
    
    private Tracking tracking = new Tracking();
    
    
    /*
     * Commands to device
     */
    public final String[] nonMovementReportInterval = {
        "AT+GTNMD=gl300,E,,,,300,300,,,,,,,,0001$AT+GTRTO=%s,3,,,,,,0001$", //specific to gl200, gl300, together with reboot command
        "AT+GTNMD=gl500,E,,,,300,,,,0001$AT+GTRTO=%s,3,,,,,,0001$", // specific to gl50x, together with reboot command
    };

    
    /*
     * Responds from device
     */
    public final String queclinkDataStr1 = "+RESP:GTFRI,300400,860599001073709,,0,0,1,0,0.0,215,1.9,24.950449,60.193629,20160919101701,0244,0091,0D9F,ABEE,,95,20160921072832,F510$";
    public final String queclinkDataStr2 = "+RESP:GTCTN,400201,860599001073710,GL500,0,0,2,27.4,20,0,0.2,41,29.5,24.950604,60.193672,20160922113923,0244,0091,0D9F,ABEE,,,,20160922114321,2AF6$";
    public final String queclinkDataStr3 = "+RESP:GTSTT,1F0101,860599001073711,1G1JC5444R7252367,,16,0,4.3,92,70.0,121.354335,31.222073,20090214013254,0460,0000,18d8,6141,00,20090214093254,11F0$";
    public final String[] queclinkDataStr = {queclinkDataStr1, queclinkDataStr2, queclinkDataStr3};
    public final String[] types = {"queclink_gl300", "queclink_gl505", "queclink_gv500"};

    public final String[] IMEI = {"860599001073709", "860599001073710", "860599001073711"};
    
    public final Hardware queclinkHardware1 = new Hardware("Queclink", IMEI[0], "04.00");
    public final Hardware queclinkHardware2 = new Hardware("Queclink", IMEI[1], "02.01");
    public final Hardware queclinkHardware3 = new Hardware("Queclink", IMEI[2], "01.01");
    public final Hardware[] queclinkHardwares = {queclinkHardware1, queclinkHardware2, queclinkHardware3};
    
    public QueclinkDeviceSetting queclinkDeviceSetting = new QueclinkDeviceSetting(trackerAgent);
    
    @Before
    public void setup() {
        when(trackerAgent.getOrCreateTrackerDevice(anyString())).thenReturn(trackerDevice);
        when(trackerDevice.getManagedObject()).thenReturn(managedObject);
        
    }
    
    @Test
    public void testTypeAfterParse() {
        for (int i = 0; i < queclinkDataStr.length; ++i) {
            String[] queclinkData = queclinkDataStr[i].split(QUECLINK.getFieldSeparator());
            String imei = queclinkDeviceSetting.parse(queclinkData);
            connectionDetails  = new TestConnectionDetails();
            connectionDetails.setImei(imei);
            queclinkDeviceSetting.onParsed(new ReportContext(connectionDetails, queclinkData));
            
            assertEquals(IMEI[i], imei);
            verify(trackerAgent).getOrCreateTrackerDevice(IMEI[i]);
            verify(managedObject).setType(types[i]);
        } 
    }
    
    @Test
    public void testHardwareAfterParse() {
        for (int i = 0; i < queclinkDataStr.length; ++i) {
            String[] queclinkData = queclinkDataStr[i].split(QUECLINK.getFieldSeparator());
            String imei = queclinkDeviceSetting.parse(queclinkData);
            connectionDetails  = new TestConnectionDetails();
            connectionDetails.setImei(imei);
            queclinkDeviceSetting.onParsed(new ReportContext(connectionDetails, queclinkData));
            
            assertEquals(IMEI[i], imei);
            verify(trackerAgent).getOrCreateTrackerDevice(IMEI[i]);
            verify(managedObject).set(queclinkHardwares[i]);
        }
    }
    
    @Test
    public void setNonMovementReportInterval() {
        
        OperationContext operationCtx;
        OperationRepresentation operation = new OperationRepresentation();
        operation.set(tracking);
        
        for (int i =0; i < nonMovementReportInterval.length; ++i) {
            connectionDetails = new TestConnectionDetails();
            operationCtx = new OperationContext(connectionDetails, operation);
            String deviceCommand = queclinkDeviceSetting.translate(operationCtx);
            assertEquals(nonMovementReportInterval[i], deviceCommand);
        }
    }
}
