package c8y.trackeragent.protocol.rfv16;

import java.io.InputStream;
import java.net.Socket;

import c8y.trackeragent.ConnectedTracker;
import c8y.trackeragent.TrackerAgent;
import c8y.trackeragent.protocol.rfv16.message.RFV16ServerMessages;
import c8y.trackeragent.protocol.rfv16.parser.HeartbeatRFV16Parser;
import c8y.trackeragent.protocol.rfv16.parser.PositionUpdateRFV16Parser;
import c8y.trackeragent.service.AlarmService;
import c8y.trackeragent.service.MeasurementService;

import com.cumulocity.agent.server.context.DeviceContextService;

public class ConnectedRFV16Tracker extends ConnectedTracker {
    
    
    public ConnectedRFV16Tracker(Socket client, InputStream bis, TrackerAgent trackerAgent, DeviceContextService contextService) {
        super(client, bis, RFV16Constants.REPORT_SEP, RFV16Constants.FIELD_SEP, trackerAgent, contextService);
        RFV16ServerMessages serverMessages = new RFV16ServerMessages();
        MeasurementService measurementService = new MeasurementService();
        AlarmService alarmService = new AlarmService();
        addFragment(new PositionUpdateRFV16Parser(trackerAgent, serverMessages, measurementService));
        addFragment(new HeartbeatRFV16Parser(trackerAgent, serverMessages, alarmService));
    }
    
    

}