package c8y.trackeragent.utils;

import static com.cumulocity.model.authentication.CumulocityCredentials.Builder.cumulocityCredentials;
import static com.google.common.collect.FluentIterable.from;

import java.util.concurrent.Callable;

import c8y.trackeragent.DeviceManagedObject;
import c8y.trackeragent.TrackerPlatform;
import c8y.trackeragent.devicebootstrap.DeviceCredentials;
import c8y.trackeragent.devicebootstrap.DeviceCredentialsRepository;
import c8y.trackeragent.exception.SDKExceptions;

import com.cumulocity.agent.server.context.DeviceContextService;
import com.cumulocity.agent.server.repository.InventoryRepository;
import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.ClientConfiguration;
import com.cumulocity.sdk.client.PlatformImpl;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TrackerPlatformProvider {

    private final DeviceCredentialsRepository deviceCredentialsRepository;
    private final Cache<PlatformKey, TrackerPlatform> cache;
    private final TrackerConfiguration config;
    private final Object lock = new Object();
    private final DeviceContextService contextService;
    private final InventoryRepository inventoryRepository;
    private final String agentUser;
    private final String agentPassword;

    public TrackerPlatformProvider(TrackerConfiguration config, DeviceCredentialsRepository deviceCredentialsRepository,
            DeviceContextService contextService, InventoryRepository inventoryRepository, String agentUser, String agentPassword) {
        this.config = config;
        this.deviceCredentialsRepository = deviceCredentialsRepository;
        this.cache = CacheBuilder.newBuilder().build();
        this.contextService = contextService;
        this.inventoryRepository = inventoryRepository;
        this.agentUser = agentUser;
        this.agentPassword = agentPassword;
    }

    public TrackerPlatform getDevicePlatformForTenant(final String tenantId) {
        return from(cache.asMap().values()).filter(new Predicate<TrackerPlatform>() {
            public boolean apply(TrackerPlatform platform) {
                return tenantId.equals(platform.getTenantId());
            }
        }).last().orNull();
    }
    
    public TrackerPlatform getDevicePlatform(final String imei) {
        if (imei == null) {
            throw new IllegalArgumentException("Imei must not be null!");
        }
        return getPlatform(new PlatformKey(imei));
    }

    public TrackerPlatform getBootstrapPlatform() {
        return getPlatform(new PlatformKey(null));
    }

    private TrackerPlatform getPlatform(final PlatformKey key) {
        try {
            return cache.get(key, new Callable<TrackerPlatform>() {

                @Override
                public TrackerPlatform call() throws Exception {
                    return createPlatform(key);
                }

            });
        } catch (Exception e) {
            throw SDKExceptions.narrow(e, "Can't access device platform for " + key);
        }
    }

    private TrackerPlatform createPlatform(PlatformKey key) {
        if (key.isBootstrap()) {
            return createBootstrapPlatform();
        } else {
            return createDevicePlatform(key.getImei());
        }
    }

    private TrackerPlatform createDevicePlatform(String imei) {
        DeviceCredentials deviceCredentials = deviceCredentialsRepository.getCredentials(imei);
        String tenantId = deviceCredentials.getTenant();
        CumulocityCredentials credentials = cumulocityCredentials(deviceCredentials.getUsername(), deviceCredentials.getPassword()).withTenantId(tenantId).build();
        PlatformImpl platform = c8yPlatform(credentials);
        TrackerPlatform trackerPlatform = new TrackerPlatform(platform);
        setupAgent(trackerPlatform);
        return trackerPlatform;
    }

    private TrackerPlatform createBootstrapPlatform() {
        CumulocityCredentials credentials = cumulocityCredentials(config.getBootstrapUser(), config.getBootstrapPassword()).withTenantId(config.getBootstrapTenant()).build();
        PlatformImpl paltform = c8yPlatform(credentials);
        return new TrackerPlatform(paltform);
    }

    private PlatformImpl c8yPlatform(CumulocityCredentials credentials) {
        PlatformImpl platform = new PlatformImpl(config.getPlatformHost(), credentials, new ClientConfiguration(null, false));
        platform.setForceInitialHost(config.getForceInitialHost());
        return platform;
    }

    private void setupAgent(TrackerPlatform platform) {
        synchronized (lock) {
            DeviceManagedObject deviceManagedObject = new DeviceManagedObject(platform, contextService, inventoryRepository, agentUser, agentPassword);
            ManagedObjectRepresentation agentMo = deviceManagedObject.assureTrackerAgentExisting();
            platform.setAgent(agentMo);
        }
    }

    private static class PlatformKey {

        private final String imei;

        PlatformKey(String imei) {
            this.imei = imei;
        }

        String getImei() {
            return imei;
        }

        boolean isBootstrap() {
            return imei == null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((imei == null) ? 0 : imei.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PlatformKey other = (PlatformKey) obj;
            if (imei == null) {
                if (other.imei != null)
                    return false;
            } else if (!imei.equals(other.imei))
                return false;
            return true;
        }
        
        @Override
        public String toString() {
            return isBootstrap() ? "bootstrap" : "imei: " + imei;
        }
    }
}