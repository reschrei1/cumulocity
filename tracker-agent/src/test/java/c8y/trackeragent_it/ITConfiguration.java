package c8y.trackeragent_it;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import c8y.trackeragent.Main;
import c8y.trackeragent.devicebootstrap.DeviceBinder;
import c8y.trackeragent.server.Servers;

import com.cumulocity.agent.server.feature.ContextFeature;
import com.cumulocity.agent.server.feature.RepositoryFeature;
import com.cumulocity.agent.server.logging.LoggingService;
import com.cumulocity.agent.server.repository.BinariesRepository;
import com.cumulocity.agent.server.repository.DeviceControlRepository;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"c8y.trackeragent"},excludeFilters={
  @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = Main.class)
})
@Import({RepositoryFeature.class, ContextFeature.class})
@PropertySource(value = { "file:/etc/tracker-agent/test.properties", 
        "file:/etc/tracker-agent/tracker-agent-server.properties" })
public class ITConfiguration {

    @Autowired
    Servers servers;
    
    @Autowired
    DeviceBinder deviceBinder;
    
    @Bean
    @Autowired
    public LoggingService loggingService(DeviceControlRepository deviceControl, BinariesRepository binaries, 
            @Value("${C8Y.log.file.path}") String logfile, @Value("${C8Y.log.timestamp.format:}") String timestampFormat,
            @Value("${C8Y.application.id}") String applicationId) {
        return new LoggingService(deviceControl, binaries, logfile, timestampFormat, applicationId);
    }
    
    @PostConstruct
    public void startServer() {
        servers.startAll();
        deviceBinder.init();
    }
}