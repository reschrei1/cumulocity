package c8y.trackeragent;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.PlatformImpl;

public class TrackerContextFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackerContextFactory.class);
    
    public static final String PROPS = "/cumulocity.properties";
    public static final String TENANT_ACCESS_REGEXP = "(.*)\\.(host|user|password)";
    private static final Pattern tenantAccessPattern = Pattern.compile(TENANT_ACCESS_REGEXP);

    private final Properties props = new Properties();
    
    public static TrackerContextFactory instance() {
        return new TrackerContextFactory();
    }
    
    public TrackerContext createTrackerContext() throws IOException {
        loadConfiguration();
        Map<String, TenantAccess> tenantAccesses = readTenantAccesses();
        Map<String, Platform> platforms = asPlatforms(tenantAccesses);
        return new TrackerContext(platforms, props);
        
    }

    private Map<String, Platform> asPlatforms(Map<String, TenantAccess> tenantAccesses) {
        Map<String, Platform> result = new HashMap<>();
        for (Entry<String, TenantAccess> tenantAccessEntry : tenantAccesses.entrySet()) {
            TenantAccess tenantAccess = tenantAccessEntry.getValue();
            String tenantId = tenantAccessEntry.getKey();
            if(tenantAccess.isValid()) {
                result.put(tenantId, asPlatform(tenantAccess));
            } else {
                logger.error(format("Missing access information for tenant %s; expected 'host', 'user' and 'password'", tenantId), tenantAccess);                
            }
        }
        return result;
    }

    private Platform asPlatform(TenantAccess tenantAccess) {
        CumulocityCredentials cumulocityCredentials = new CumulocityCredentials(tenantAccess.getUser(), tenantAccess.getPassword());
        return new PlatformImpl(tenantAccess.getHost(), cumulocityCredentials);
    }

    private Map<String, TenantAccess> readTenantAccesses() {
        Map<String, TenantAccess> tenantAccesses = new HashMap<>();
        for (Object configEntry : props.keySet()) {
            tryReadEntryAsTenantAccessInfo(tenantAccesses, String.valueOf(configEntry));
        }
        return tenantAccesses;
    }

    private void loadConfiguration() throws IOException {
        try (InputStream is = TrackerContext.class.getResourceAsStream(PROPS); 
                InputStreamReader ir = new InputStreamReader(is)) {
            props.load(ir);
        }
    }
    
    private void tryReadEntryAsTenantAccessInfo(Map<String, TenantAccess> tenantAccesses, String configEntry) {
        Matcher matcher = tenantAccessPattern.matcher(configEntry);
        if(matcher.matches()) {
            String tenantId = matcher.group(1);
            TenantAccess tenantAccess = getOrCreateTenantAccess(tenantAccesses, tenantId);
            tenantAccess.set(matcher.group(2), props.getProperty(configEntry));
        }
    }

    private TenantAccess getOrCreateTenantAccess(Map<String, TenantAccess> tenantAccesses, String tenantId) {
        TenantAccess tenantAccess = tenantAccesses.get(tenantId);
        if(tenantAccess == null) {
            tenantAccess = new TenantAccess();
            tenantAccesses.put(tenantId, tenantAccess);
        }
        return tenantAccess;
    }
    
    static class TenantAccess {
            
        private final Map<String, String> vals = new HashMap<>();
        
        String getHost() {
            return vals.get("host");
        }
        String getUser() {
            return vals.get("user");
        }
        String getPassword() {
            return vals.get("password");
        }
        boolean isValid() {
            return vals.keySet().size() == 3;
        }
        void set(String paramName, String paramValue) {
            vals.put(paramName, paramValue);
        }
        @Override
        public String toString() {
            return String.format("TenantAccess [vals=%s]", vals);
        }
        
    }
}
