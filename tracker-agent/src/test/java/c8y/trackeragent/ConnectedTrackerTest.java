/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package c8y.trackeragent;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.cumulocity.sdk.client.SDKException;

import c8y.trackeragent.context.OperationContext;
import c8y.trackeragent.context.ReportContext;
import c8y.trackeragent.devicebootstrap.DeviceBootstrapProcessor;
import c8y.trackeragent.devicebootstrap.DeviceCredentials;
import c8y.trackeragent.devicebootstrap.DeviceCredentialsRepository;
import c8y.trackeragent.exception.UnknownDeviceException;
import c8y.trackeragent.exception.UnknownTenantException;
import c8y.trackeragent.protocol.gl200.GL200Constants;
import c8y.trackeragent.service.TrackerDeviceContextService;

public class ConnectedTrackerTest {
    
    public static final String REPORT1 = "field1|field2";
    public static final String REPORT2 = "field3|field4";
    private static final Charset CHARSET = Charset.forName("US-ASCII");

    private TrackerDeviceContextService contextService = mock(TrackerDeviceContextService.class);
    private Socket client = mock(Socket.class);
    private BufferedInputStream in = mock(BufferedInputStream.class);
    private OutputStream out = mock(OutputStream.class);
    private Translator translator = mock(Translator.class);
    private Parser parser = mock(Parser.class);
    private DeviceBootstrapProcessor bootstrapProcessor = mock(DeviceBootstrapProcessor.class);
    private DeviceCredentialsRepository credentialsRepository = mock(DeviceCredentialsRepository.class);
    private ConnectedTracker<Fragment> tracker;

    @Before
    public void setup() throws Exception {
        ConnectionRegistry.instance().remove("imei");
        tracker = new ConnectedTracker<Fragment>(
        		GL200Constants.REPORT_SEP, 
        		GL200Constants.FIELD_SEP,
        		asList(translator, parser),
        		bootstrapProcessor,
        		credentialsRepository,
        		contextService);
        tracker.init(client, in);
        tracker.setOut(out);
    }

    @Test
    public void singleReportProcessing() throws Exception {
    	when(credentialsRepository.getDeviceCredentials("imei")).thenReturn(DeviceCredentials.forDevice("imei", "tenant"));
    	when(credentialsRepository.getAgentCredentials("tenant")).thenReturn(DeviceCredentials.forAgent("tenant", "user", "password"));
        String[] dummyReport = null;
        when(parser.parse(dummyReport)).thenReturn("imei");
        when(parser.onParsed(new ReportContext(dummyReport, "imei", null))).thenReturn(true);

        tracker.processReport(dummyReport);

        verify(parser).parse(dummyReport);
        verifyZeroInteractions(translator);
        assertEquals(tracker, ConnectionRegistry.instance().get("imei"));
    }
    
    @Test
    public void singleReportProcessingForNewImei() throws SDKException {
    	when(credentialsRepository.getDeviceCredentials("imei")).thenThrow(UnknownDeviceException.forImei("imei"));
    	when(credentialsRepository.getAgentCredentials("tenant")).thenThrow(UnknownTenantException.forTenantId("tenant"));
        String[] dummyReport = null;
        when(parser.parse(dummyReport)).thenReturn("imei");
        
        tracker.processReport(dummyReport);
        
        assertThat(ConnectionRegistry.instance()).isEmpty();
        verify(bootstrapProcessor).tryAccessDeviceCredentials("imei");
        verifyZeroInteractions(translator);
    }

    @Test
    public void continuousReportProcessing() throws IOException, SDKException {
    	when(credentialsRepository.getDeviceCredentials("imei")).thenReturn(DeviceCredentials.forDevice("imei", "tenant"));
    	when(credentialsRepository.getAgentCredentials("tenant")).thenReturn(DeviceCredentials.forAgent("tenant", "user", "password"));    	
        when(parser.parse(any(String[].class))).thenReturn("imei");

        String reports = REPORT1 + GL200Constants.REPORT_SEP + REPORT2 + GL200Constants.REPORT_SEP;

        ByteArrayInputStream is = null;
        try {
            is = new ByteArrayInputStream(reports.getBytes(CHARSET));
            tracker.processReports(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

        verify(parser).parse(REPORT1.split(GL200Constants.FIELD_SEP));
        verify(parser).parse(REPORT2.split(GL200Constants.FIELD_SEP));
        verifyZeroInteractions(translator);
    }

    @Test
    public void testExecute() throws IOException {
    	when(credentialsRepository.getDeviceCredentials("imei")).thenThrow(UnknownDeviceException.forImei("imei"));
    	when(credentialsRepository.getAgentCredentials("tenant")).thenThrow(UnknownTenantException.forTenantId("tenant"));    	
        String translation = "translation";

        OperationContext operation = mock(OperationContext.class);
        when(translator.translate(operation)).thenReturn(translation);

        tracker.execute(operation);

        verifyZeroInteractions(parser);
        verify(translator).translate(operation);
        verify(out).write(translation.getBytes(CHARSET));
    }
}
