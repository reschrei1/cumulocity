package c8y.trackeragent.service;

import static com.cumulocity.model.event.CumulocityAlarmStatuses.ACTIVE;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import c8y.trackeragent.TrackerDevice;
import c8y.trackeragent.context.ReportContext;

import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

@Component
public class AlarmService {
    
    private static Logger logger = LoggerFactory.getLogger(AlarmService.class);

    private final AlarmMappingService alarmMappingService;
    
    @Autowired
    public AlarmService(AlarmMappingService alarmMappingService) {
        this.alarmMappingService = alarmMappingService;
    }

    public AlarmRepresentation createAlarm(ReportContext reportCtx, AlarmType alarmType, TrackerDevice device) {
        AlarmRepresentation alarm = newAlarm(device);
        populateAlarm(alarm, reportCtx, alarmType);
        logger.info("Create alarm {}.", alarm);
        device.createAlarm(alarm);
        return alarm;
    }
        
    private void populateAlarm(AlarmRepresentation alarm, ReportContext reportCtx, AlarmType alarmType) {
        alarm.setType(alarmMappingService.getType(alarmType.name()));
        Object[] args = alarmType.getTextArgs(alarm, reportCtx);
        alarm.setText(alarmMappingService.getText(alarmType.name(), args));
        alarm.setSeverity(alarmMappingService.getSeverity(alarmType.name()));
    }

    public void clearAlarm(ReportContext reportCtx, AlarmType alarmType, TrackerDevice device) {
        String type = alarmMappingService.getType(alarmType.name());
        AlarmRepresentation alarm = device.findActiveAlarm(type);
        if (alarm != null) {
            logger.info("Clear alarm {}.", alarm);
            device.clearAlarm(alarm);
        }
    }
    
    private AlarmRepresentation newAlarm(TrackerDevice device) {
        AlarmRepresentation alarm = new AlarmRepresentation();
        ManagedObjectRepresentation source = new ManagedObjectRepresentation();
        source.setId(device.getGId());
        alarm.setSource(source);
        alarm.setTime(new Date());
        alarm.setStatus(ACTIVE.toString());
        return alarm;
    }

    public void populateLocationEventByAlarm(EventRepresentation event, AlarmRepresentation alarm) {
        populateLocationEventByAlarms(event, Collections.singletonList(alarm));
    }

    public void populateLocationEventByAlarms(EventRepresentation event, Collection<AlarmRepresentation> alarms) {
        String text = Joiner.on("|").join(Iterables.transform(alarms, ALARM_TO_TEXT));
        event.setText(text);
    }
    
    private static Function<AlarmRepresentation, String> ALARM_TO_TEXT = new Function<AlarmRepresentation, String>() {

        @Override
        public String apply(@Nullable AlarmRepresentation input) {
            return input.getText();
        }
    };

}