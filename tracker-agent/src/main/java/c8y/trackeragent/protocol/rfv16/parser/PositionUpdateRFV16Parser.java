package c8y.trackeragent.protocol.rfv16.parser;

import static c8y.trackeragent.protocol.rfv16.RFV16Constants.CONNECTION_PARAM_CONTROL_COMMANDS_SENT;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.sdk.client.SDKException;

import c8y.Position;
import c8y.SpeedMeasurement;
import c8y.trackeragent.Parser;
import c8y.trackeragent.TrackerAgent;
import c8y.trackeragent.TrackerDevice;
import c8y.trackeragent.context.ReportContext;
import c8y.trackeragent.protocol.rfv16.RFV16Constants;
import c8y.trackeragent.protocol.rfv16.message.RFV16ServerMessages;
import c8y.trackeragent.service.AlarmService;
import c8y.trackeragent.service.MeasurementService;
import c8y.trackeragent.utils.TrackerConfiguration;
import c8y.trackeragent.utils.message.TrackerMessage;

@Component
public class PositionUpdateRFV16Parser extends RFV16Parser implements Parser {
    
    private static Logger logger = LoggerFactory.getLogger(PositionUpdateRFV16Parser.class);
    
    private final MeasurementService measurementService;
    private final AlarmService alarmService;
    private final TrackerConfiguration config;
    
    @Autowired
    public PositionUpdateRFV16Parser(TrackerAgent trackerAgent, RFV16ServerMessages serverMessages, 
            MeasurementService measurementService, AlarmService alarmService, TrackerConfiguration config) {
        super(trackerAgent, serverMessages);
        this.measurementService = measurementService;
        this.alarmService = alarmService;
        this.config = config;
    }

    @Override
    public boolean onParsed(ReportContext reportCtx) throws SDKException {
        if (!isV1Report(reportCtx)) {
            return false;
        }
        if(!RFV16Constants.DATE_EFFECTIVE_MARK.equals(reportCtx.getEntry(4))) {
            logger.debug("Not valid position report: {}", reportCtx);
            return true;
        }
        logger.debug("Process V1 report", reportCtx);
        processPositionReport(reportCtx);
        return true;
    }

    private void processPositionReport(ReportContext reportCtx) {
    	reportCtx.setConnectionParam(RFV16Constants.CONNECTION_PARAM_MAKER, reportCtx.getEntry(0));
        TrackerDevice device = trackerAgent.getOrCreateTrackerDevice(reportCtx.getImei());
        Position position = getPosition(reportCtx);
        logger.debug("Update position for imei: {} to: {}.", reportCtx.getImei(), position);
        
        SpeedMeasurement speed = createSpeedMeasurement(reportCtx, device);
        
        EventRepresentation event = device.aLocationUpdateEvent();
        if (speed != null) {
            event.set(speed);
        }
        
        Collection<AlarmRepresentation> alarms = createAlarms(reportCtx, device);
        
        if (!alarms.isEmpty()) {
            alarmService.populateLocationEventByAlarms(event, alarms);
        }
        
        device.setPosition(event, position);
        if (!reportCtx.isConnectionFlagOn(CONNECTION_PARAM_CONTROL_COMMANDS_SENT)) {
            sendControllCommands(reportCtx);
        }
    }

    private SpeedMeasurement createSpeedMeasurement(ReportContext reportCtx, TrackerDevice device) {
        SpeedMeasurement speed = null;
        BigDecimal speedValue = getSpeed(reportCtx);
        if (speedValue != null) {
            speed = measurementService.createSpeedMeasurement(speedValue, device);
        }
        return speed;
    }

    private Collection<AlarmRepresentation> createAlarms(ReportContext reportCtx, TrackerDevice device) {
        String status = reportCtx.getEntry(12);
        Collection<AlarmRepresentation> alarms = new ArrayList<AlarmRepresentation>();
        Collection<RFV16AlarmType> alarmTypes = AlarmTypeDecoder.getAlarmTypes(status);
        logger.debug("Read status {} as alarms {} for device {}", status, reportCtx.getImei(), alarmTypes);
        for (RFV16AlarmType alarmType : alarmTypes) {
            AlarmRepresentation alarm = alarmService.createAlarm(reportCtx, alarmType, device);
            alarms.add(alarm);
        }
        return alarms;
    }

    private void sendControllCommands(ReportContext reportCtx) {
        String maker = reportCtx.getEntry(0);
        TrackerMessage reportMonitoringCommand = serverMessages.reportMonitoringCommand(
                maker, reportCtx.getImei(), config.getRfv16LocationReportTimeInterval().toString());
        reportCtx.writeOut(reportMonitoringCommand);
    }

    private boolean isV1Report(ReportContext reportCtx) {
        return reportCtx.getNumberOfEntries() == 13 
                && RFV16Constants.MESSAGE_TYPE_V1.equals(reportCtx.getEntry(2))
                && RFV16Constants.DATE_EFFECTIVE_MARK.equals(reportCtx.getEntry(4));
    }
}    