package c8y.trackeragent.protocol.coban;

import java.io.InputStream;
import java.net.Socket;

import c8y.trackeragent.ConnectedTracker;
import c8y.trackeragent.TrackerAgent;
import c8y.trackeragent.protocol.coban.message.CobanServerMessages;
import c8y.trackeragent.protocol.coban.parser.HeartbeatCobanParser;
import c8y.trackeragent.protocol.coban.parser.LogonCobanParser;
import c8y.trackeragent.protocol.coban.parser.PositionUpdateCobanParser;
import c8y.trackeragent.protocol.coban.parser.alarm.AlarmCobanParser;
import c8y.trackeragent.utils.message.TrackerMessageFactory;

public class ConnectedCobanTracker extends ConnectedTracker {

    public ConnectedCobanTracker(Socket client, InputStream bis, TrackerAgent trackerAgent) {
        super(client, bis, CobanConstants.REPORT_SEP, CobanConstants.FIELD_SEP, trackerAgent);
        CobanServerMessages serverMessages = new CobanServerMessages();
        addFragment(new LogonCobanParser(trackerAgent, serverMessages));
        addFragment(new HeartbeatCobanParser(trackerAgent, serverMessages));
        addFragment(new PositionUpdateCobanParser(trackerAgent, serverMessages));
        addFragment(new AlarmCobanParser(trackerAgent));
    }

}
