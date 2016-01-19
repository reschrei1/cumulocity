package c8y.trackeragent.protocol.rfv16.parser;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import c8y.Position;
import c8y.trackeragent.ReportContext;
import c8y.trackeragent.TrackerDevice;
import c8y.trackeragent.protocol.rfv16.RFV16ParserTestSupport;
import c8y.trackeragent.protocol.rfv16.device.RFV16Device;
import c8y.trackeragent.utils.Positions;
import c8y.trackeragent.utils.TK10xCoordinatesTranslator;
import c8y.trackeragent.utils.message.TrackerMessage;

import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;

public class PositionUpdateRFV16ReportTest extends RFV16ParserTestSupport {
    
    PositionUpdateRFV16Parser parser;
    ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
    ArgumentCaptor<EventRepresentation> eventCaptor = ArgumentCaptor.forClass(EventRepresentation.class);
    ArgumentCaptor<RFV16AlarmType> alarmTypeCaptor = ArgumentCaptor.forClass(RFV16AlarmType.class);
    ArgumentCaptor<MeasurementRepresentation> measurementCaptor = ArgumentCaptor.forClass(MeasurementRepresentation.class);
    
    @Before
    public void init() {
        parser = new PositionUpdateRFV16Parser(trackerAgent, serverMessages, measurementService, alarmService);
    }
    
    @Test    
    public void shouldParseImeiFromPositionMessage() throws Exception {
        String imei = parser.parse(deviceMessages.positionUpdate("DB", IMEI,Positions.TK10xSample).asArray());
        
        assertThat(imei).isEqualTo(IMEI);
    }
    
    @Test
    public void shouldProcessPositionUpdateV1() throws Exception {
        TrackerMessage deviceMessage = deviceMessages.positionUpdate("DB", IMEI, Positions.TK10xSample);
        ReportContext reportCtx = new ReportContext(deviceMessage.asArray(), IMEI, out);
        currentDeviceIs(new RFV16Device().setLocationReportInterval("30"));
        
        parser.onParsed(reportCtx);
        
        verify(deviceMock).setPosition(eventCaptor.capture(), positionCaptor.capture());
        assertThat(positionCaptor.getValue()).isEqualTo(TK10xCoordinatesTranslator.parse(Positions.TK10xSample));
        assertOut("*DB,1234567890,D1,010000,30,1#*DB,1234567890,SCF,010000,0,10#");
    }
    
    /**
     * The value is not use yet
     */
    @Test
    public void shouldParseDateTime() throws Exception {
        TrackerMessage deviceMessage = deviceMessages.positionUpdate("DB", IMEI, Positions.TK10xSample);
        ReportContext reportCtx = new ReportContext(deviceMessage.asArray(), IMEI, out);
        DateTime dateTime = RFV16Parser.getDateTime(reportCtx);
        
        assertThat(dateTime).isEqualTo(new DateTime(0));
    }
    
    @Test
    public void shouldCreateAlarmIfPresent() throws Exception {
        TrackerMessage deviceMessage = deviceMessages.positionUpdate("DB", IMEI, Positions.TK10xSample, "FFFDFFFF");
        ReportContext reportCtx = new ReportContext(deviceMessage.asArray(), IMEI, out);
        currentDeviceIs(new RFV16Device().setLocationReportInterval("30"));
        
        parser.onParsed(reportCtx);
        
        verify(alarmService).createRFV16Alarm(any(ReportContext.class), alarmTypeCaptor.capture(), any(TrackerDevice.class));
        assertThat(alarmTypeCaptor.getValue()).isEqualTo(RFV16AlarmType.LOW_BATTERY);
    }
    
    @Test
    public void shouldNotCreateAlarmIfNotPresent() throws Exception {
        TrackerMessage deviceMessage = deviceMessages.positionUpdate("DB", IMEI, Positions.TK10xSample, "FFFFFFFF");
        ReportContext reportCtx = new ReportContext(deviceMessage.asArray(), IMEI, out);
        currentDeviceIs(new RFV16Device().setLocationReportInterval("30"));
        
        parser.onParsed(reportCtx);
        
        verify(alarmService, never()).createRFV16Alarm(any(ReportContext.class), any(RFV16AlarmType.class), any(TrackerDevice.class));
    }

}
