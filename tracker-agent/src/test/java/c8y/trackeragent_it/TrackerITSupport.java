package c8y.trackeragent_it;

import static com.cumulocity.model.authentication.CumulocityCredentials.Builder.cumulocityCredentials;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cumulocity.agent.server.context.DeviceContextService;
import com.cumulocity.agent.server.repository.InventoryRepository;
import com.cumulocity.model.ID;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.model.event.CumulocityAlarmStatuses;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.PlatformImpl;
import com.cumulocity.sdk.client.alarm.AlarmFilter;
import com.cumulocity.sdk.client.event.EventFilter;

import c8y.trackeragent.TrackerAgent;
import c8y.trackeragent.TrackerPlatform;
import c8y.trackeragent.configuration.TrackerConfiguration;
import c8y.trackeragent.device.TrackerDevice;
import c8y.trackeragent.devicebootstrap.DeviceCredentialsRepository;
import c8y.trackeragent.protocol.TrackingProtocol;
import c8y.trackeragent.service.AlarmMappingService;
import c8y.trackeragent.service.AlarmType;
import c8y.trackeragent.utils.message.TrackerMessage;
import c8y.trackeragent_it.config.ServerConfiguration;
import c8y.trackeragent_it.config.TestConfiguration;
import c8y.trackeragent_it.service.Bootstraper;
import c8y.trackeragent_it.service.NewDeviceRequestService;
import c8y.trackeragent_it.service.SocketWritter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ServerConfiguration.class, TestConfiguration.class })
public abstract class TrackerITSupport {

    @Autowired
    protected TrackerConfiguration trackerAgentConfig;
    
    @Autowired
    protected TestSettings testSettings;

    @Autowired
    protected TrackerAgent trackerAgent;

    @Autowired
    protected AlarmMappingService alarmMappingService;

    @Autowired
    @Deprecated
    protected DeviceContextService contextService;

    @Autowired
    protected InventoryRepository inventoryRepository;

    @Autowired
    protected DeviceCredentialsRepository deviceCredentialsRepository;
    
    protected TrackerPlatform trackerPlatform;
    
    protected Bootstraper bootstraper;
    protected SocketWritter socketWriter;

    @Before
    public void baseSetUp() throws Exception {
        trackerPlatform = trackerPlatform(testSettings);
        Thread.sleep(200);// avoid address already in use error
        System.out.println(testSettings);
        System.out.println(trackerAgentConfig);
        socketWriter = new SocketWritter(testSettings, trackerAgentConfig.getPort(getTrackerProtocol()));
        NewDeviceRequestService newDeviceRequestService = new NewDeviceRequestService(trackerPlatform.getPlatformParameters(), testSettings);
        bootstraper = new Bootstraper(testSettings, socketWriter, newDeviceRequestService);
        bootstraper.deleteExistingAgentRequest();
    }

    protected abstract TrackingProtocol getTrackerProtocol();

    protected String writeInNewConnection(TrackerMessage... deviceMessages) throws Exception {
        return socketWriter.writeInNewConnection(deviceMessages);
    }
    
    protected ManagedObjectRepresentation getDeviceMO(String imei) {
        GId gid = getGId(imei);
        return trackerPlatform.getInventoryApi().get(gid);
    }

    private GId getGId(String imei) {
        ID id = TrackerDevice.imeiAsId(imei);
        ExternalIDRepresentation eir = trackerPlatform.getIdentityApi().getExternalId(id);
        return eir.getManagedObject().getId();
    }

    protected void bootstrapDevice(String imei, TrackerMessage deviceMessage) throws Exception {
        bootstraper.bootstrapDevice(imei, deviceMessage);
    }

    protected AlarmRepresentation findAlarm(String imei, AlarmType alarmType) {
        GId gId = getGId(imei);
        String type = alarmMappingService.getType(alarmType.name());
        AlarmFilter filter = new AlarmFilter().bySource(gId).byStatus(CumulocityAlarmStatuses.ACTIVE).byType(type);
        List<AlarmRepresentation> alarms = trackerPlatform.getAlarmApi().getAlarmsByFilter(filter).get().getAlarms();
        return alarms.isEmpty() ? null : alarms.get(0);
    }
    
    protected EventRepresentation findLastEvent(String imei, String type) {
        GId gId = getGId(imei);
        Date fromDate = new Date(0);
        Date toDate = new DateTime().plusDays(1).toDate();
        EventFilter filter = new EventFilter().bySource(gId).byType(type).byDate(fromDate, toDate);
        List<EventRepresentation> events = trackerPlatform.getEventApi().getEventsByFilter(filter).get().getEvents();
        return events.isEmpty() ? null : events.get(0);
    }
    
    public static TrackerPlatform trackerPlatform(TestSettings testSettings) {
        return new TrackerPlatform(platform(testSettings));
    }
    
    public static PlatformImpl platform(TestSettings testSettings) {
        CumulocityCredentials credentials = cumulocityCredentials(testSettings.getC8yUser(), testSettings.getC8yPassword())
                .withTenantId(testSettings.getC8yTenant()).build();
        return new PlatformImpl(testSettings.getC8yHost(), credentials);
    }

}
