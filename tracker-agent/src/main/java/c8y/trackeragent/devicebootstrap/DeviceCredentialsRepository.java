package c8y.trackeragent.devicebootstrap;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import c8y.trackeragent.configuration.ConfigUtils;
import c8y.trackeragent.exception.UnknownDeviceException;
import c8y.trackeragent.exception.UnknownTenantException;
import c8y.trackeragent.utils.GroupPropertyAccessor;
import c8y.trackeragent.utils.GroupPropertyAccessor.Group;

@Component
public class DeviceCredentialsRepository {

    private static final String TENANT_ENTRY_PREFIX = "tenant-";

	private static final Logger logger = LoggerFactory.getLogger(DeviceCredentialsRepository.class);

    private final Map<String, DeviceCredentials> imei2DeviceCredentials = new ConcurrentHashMap<String, DeviceCredentials>();
    private final Map<String, DeviceCredentials> tenant2AgentCredentials = new ConcurrentHashMap<String, DeviceCredentials>();
    private final GroupPropertyAccessor devicePropertyAccessor;
    private final GroupPropertyAccessor agentPropertyAccessor;
    private final Object lock = new Object();
	private final String devicePropertiesPath;


    public DeviceCredentialsRepository(String devicePropertiesPath) {
    	this.devicePropertiesPath = devicePropertiesPath;
		devicePropertyAccessor = new GroupPropertyAccessor(devicePropertiesPath, asList("tenantId"));
    	agentPropertyAccessor = new GroupPropertyAccessor(devicePropertiesPath, asList("user", "password"));
    	
    }
    public DeviceCredentialsRepository() {
    	this(ConfigUtils.get().getConfigFilePath(ConfigUtils.DEVICES_FILE_NAME));
    }

    public boolean hasDeviceCredentials(String imei) {
        return imei2DeviceCredentials.containsKey(imei);
    }
    
	public boolean hasAgentCredentials(String tenant) {
		return tenant2AgentCredentials.containsKey(tenant);
	}

	public DeviceCredentials getDeviceCredentials(String imei) {
        DeviceCredentials result = imei2DeviceCredentials.get(imei);
        if (result == null) {
            throw UnknownDeviceException.forImei(imei);
        }
        return result.duplicate();
    }
    
	public DeviceCredentials getAgentCredentials(String tenant) {
		DeviceCredentials result = tenant2AgentCredentials.get(tenant);
		if (result == null) {
			throw UnknownTenantException.forTenantId(tenant);
		}
		return result.duplicate();
	}

    
    public List<DeviceCredentials> getAllDeviceCredentials() {
        return new ArrayList<DeviceCredentials>(imei2DeviceCredentials.values());
    }
    
	public List<DeviceCredentials> getAllAgentCredentials() {
		return new ArrayList<DeviceCredentials>(tenant2AgentCredentials.values());
	}

	public void saveDeviceCredentials(DeviceCredentials newCredentials) {
        synchronized (lock) {
            Group group = asDeviceGroup(newCredentials.getImei(), newCredentials);
            if (!group.isFullyInitialized()) {
                throw new IllegalArgumentException("Not fully initialized credentials: " + newCredentials);
            }
            devicePropertyAccessor.write(group);
            imei2DeviceCredentials.put(newCredentials.getImei(), newCredentials);
            logger.info("Credentials for device {} have been written: {}.", newCredentials.getImei(), newCredentials);
        }
    }
    
    public void saveAgentCredentials(DeviceCredentials newCredentials) {
    	synchronized (lock) {
    		Group group = asAgentGroup(newCredentials.getTenant(), newCredentials);
    		if (!group.isFullyInitialized()) {
    			throw new IllegalArgumentException("Not fully initialized credentials: " + newCredentials);
    		}
    		agentPropertyAccessor.write(group);
    		tenant2AgentCredentials.put(newCredentials.getTenant(), newCredentials);
    		logger.info("Credentials for agent of tenant {} have been written: {}.", newCredentials.getTenant(), newCredentials);
    	}
    }

    @PostConstruct
    public void refresh() throws IOException {
    	File deviceProperties = new File(devicePropertiesPath);
    	if (!deviceProperties.exists()) {
    		deviceProperties.createNewFile();
    	}
        devicePropertyAccessor.refresh();
        agentPropertyAccessor.refresh();
        imei2DeviceCredentials.clear();
        tenant2AgentCredentials.clear();
        for (Group group : devicePropertyAccessor.getGroups()) {
            if (group.isFullyInitialized()) {
                imei2DeviceCredentials.put(group.getName(), asDeviceCredentials(group));
            }
        }
        for (Group group : agentPropertyAccessor.getGroups()) {
        	if (group.isFullyInitialized()) {
        		String tenant = groupNameToTenant(group.getName());
        		tenant2AgentCredentials.put(tenant, asAgentCredentials(group));
        	}
        }
    }

    private DeviceCredentials asDeviceCredentials(Group group) {
    	return DeviceCredentials.forDevice(group.getName(), group.get("tenantId"));
    }
        
    private DeviceCredentials asAgentCredentials(Group group) {
    	String tenant = groupNameToTenant(group.getName());
    	return DeviceCredentials.forAgent(tenant, group.get("user"), group.get("password"));
    }

    private Group asDeviceGroup(String imei, DeviceCredentials credentials) {
        Group group = devicePropertyAccessor.createEmptyGroup(imei);
        group.put("tenantId", credentials.getTenant());
        return group;
    }
    
    private Group asAgentGroup(String tenant, DeviceCredentials credentials) {
    	String groupName = tenantToGroupName(tenant);
    	Group group = agentPropertyAccessor.createEmptyGroup(groupName);
    	group.put("user", credentials.getUsername());
    	group.put("password", credentials.getPassword());
    	return group;
    }
	
	public List<DeviceCredentials> getAllDeviceCredentials(String tenant) {
		return from(getAllDeviceCredentials()).filter(DeviceCredentials.hasTenant(tenant)).toList();
	}
	
    private static String tenantToGroupName(String tenant) {
		return TENANT_ENTRY_PREFIX + tenant;
	}
    
    private static String groupNameToTenant(String groupName) {
    	return groupName.replaceFirst(TENANT_ENTRY_PREFIX, "");
    }
}