package c8y.trackeragent_it.simulator;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cumulocity.rest.representation.devicebootstrap.NewDeviceRequestRepresentation;
import com.cumulocity.sdk.client.PlatformImpl;

import c8y.trackeragent.TrackerPlatform;
import c8y.trackeragent.protocol.telic.TelicDeviceMessages;
import c8y.trackeragent.utils.Positions;
import c8y.trackeragent.utils.message.TrackerMessage;
import c8y.trackeragent_it.SocketWriter;
import c8y.trackeragent_it.TestSettings;
import c8y.trackeragent_it.TrackerITSupport;
import c8y.trackeragent_it.config.TestConfiguration;
import c8y.trackeragent_it.service.Bootstraper;
import c8y.trackeragent_it.service.NewDeviceRequestService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class })
public class LoadIT {
    
    @Autowired
    private TestSettings testSettings;
    
    private TelicDeviceMessages deviceMessages = new TelicDeviceMessages();

    private Bootstraper bootstraper;
    
    private NewDeviceRequestService newDeviceRequestService;
    
    @Before
    public void init() {
        PlatformImpl platform = TrackerITSupport.platform(testSettings);
        newDeviceRequestService = new NewDeviceRequestService(platform, testSettings);
        SocketWriter socketWriter = new SocketWriter(testSettings, 9090);
        bootstraper = new Bootstraper(testSettings, socketWriter);
    }
    
    @Test
    public void shouldCreateNewDeviceRequest() throws Exception {
        newDeviceRequestService.create("abc");
        
        assertThat(newDeviceRequestService.get("abc")).isNotNull();
        assertThat(newDeviceRequestService.get("cde")).isNull();
        
        assertThat(newDeviceRequestService.exists("abc")).isTrue();
        assertThat(newDeviceRequestService.exists("cde")).isFalse();
        
        newDeviceRequestService.delete("abc");
        
        assertThat(newDeviceRequestService.get("abc")).isNull();
        assertThat(newDeviceRequestService.exists("abc")).isFalse();
    }
    
    @Test
    public void shouldBootstrapMultiplyTelicDevices() throws Exception {
        int imeiStart = 100020;
        int imeiStop =  100021;
        for(int imei = imeiStart; imei <= imeiStop; imei++) {
            bootstrapDevice(bootstraper, "" + imei);
        }
    }

    private void bootstrapDevice(Bootstraper bootstraper, String imei) throws Exception {
        TrackerMessage message = deviceMessages.positionUpdate(imei, Positions.ZERO);
        bootstraper.bootstrapDevice(imei, message);
    }
    
    

}
