package c8y.trackeragent.protocol.rfv16.parser;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import c8y.trackeragent.context.ReportContext;
import c8y.trackeragent.device.TrackerDevice;
import c8y.trackeragent.protocol.rfv16.RFV16ParserTestSupport;
import c8y.trackeragent.utils.message.TrackerMessage;

import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;

public class DeviceSituationRFV16ParserTest extends RFV16ParserTestSupport {
    
    DeviceSituationRFV16Parser parser;
    ArgumentCaptor<MeasurementRepresentation> measurementCaptor = ArgumentCaptor.forClass(MeasurementRepresentation.class);
    ArgumentCaptor<BigDecimal> numberCaptor = ArgumentCaptor.forClass(BigDecimal.class);
    
    @Before
    public void init() {
        parser = new DeviceSituationRFV16Parser(trackerAgent, serverMessages, measurementService, alarmService);
    }

    @Test
    public void shouldCreateGSMMeasurement() throws Exception {
        processMessage(deviceMessages.deviceSituation("DB", IMEI, 0, null));
        processMessage(deviceMessages.deviceSituation("DB", IMEI, 16, null));
        processMessage(deviceMessages.deviceSituation("DB", IMEI, 31, null));
        
        verify(measurementService, times(3)).createGSMLevelMeasurement(numberCaptor.capture(), any(TrackerDevice.class), any(DateTime.class));
        assertThat(numberCaptor.getAllValues()).containsExactly(new BigDecimal(0), new BigDecimal(51), new BigDecimal(100));
    }
    
    @Test
    public void shouldCreateBatteryMeasurement() throws Exception {
        processMessage(deviceMessages.deviceSituation("DB", IMEI, null, 0));
        processMessage(deviceMessages.deviceSituation("DB", IMEI, null, 3));
        processMessage(deviceMessages.deviceSituation("DB", IMEI, null, 6));
        
        verify(measurementService, times(3)).createPercentageBatteryLevelMeasurement(numberCaptor.capture(), any(TrackerDevice.class), any(DateTime.class));
        assertThat(numberCaptor.getAllValues()).containsExactly(new BigDecimal(0), new BigDecimal(50), new BigDecimal(100));
    }
    
    private void processMessage(TrackerMessage msg) {
        ReportContext reportCtx = new ReportContext(msg.asArray(), IMEI, out);
        parser.onParsed(reportCtx);
    }
}
