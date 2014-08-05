package com.cumulocity.tekelec.server.main;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.cumulocity.model.authentication.CumulocityCredentials;

public class CredentialsManager {

    private static final String COMMON_PROPS_LOCATION = "/etc/tekelec/cumulocity.properties";
    private static final String DEVICE_PROPS_LOCATION = "/etc/tekelec/device-%s.properties";
    
    private static final String DEFAULT_HOST = "http://integration.cumulocity.com";
    private static final String DEFAULT_BOOTSTRAP_TENANT = "management";
    private static final String DEFAULT_BOOTSTRAP_USER = "devicebootstrap";
    private static final String DEFAULT_BOOTSTRAP_PASSWORD = "Fhdt1bb1f";

    public static CredentialsManager credentialsManagerFor(String imei) {
        String deviceProperties = String.format(DEVICE_PROPS_LOCATION, imei);
        return new CredentialsManager(COMMON_PROPS_LOCATION, deviceProperties);
    }

    private final String host;
    private final String devicePropsFile;

    private final CumulocityCredentials deviceCredentials;
    private final CumulocityCredentials bootstrapCredentials;

    public CredentialsManager(String commonPropsFile, String devicePropsFile) {
        this.devicePropsFile = devicePropsFile;
        Properties commonProps = PropUtils.fromFile(commonPropsFile);
        Properties deviceProps = PropUtils.fromFile(devicePropsFile);
        this.deviceCredentials = initDeviceCredentials(deviceProps);
        this.bootstrapCredentials = initBootstrapCredentials();
        this.host = commonProps.getProperty("host", DEFAULT_HOST);
    }

    private static CumulocityCredentials initBootstrapCredentials() {
        return new CumulocityCredentials(DEFAULT_BOOTSTRAP_TENANT, DEFAULT_BOOTSTRAP_USER, DEFAULT_BOOTSTRAP_PASSWORD, null);
    }

    private static CumulocityCredentials initDeviceCredentials(Properties deviceProps) {
        String user = deviceProps.getProperty("user");
        String password = deviceProps.getProperty("password");
        if(user == null || password == null) {
            return null;
        } else {
            return new CumulocityCredentials(deviceProps.getProperty("tenant"), user, password, null);
        }
    }

    public String getHost() {
        return host;
    }

    public CumulocityCredentials getDeviceCredentials() {
        return deviceCredentials;
    }

    public CumulocityCredentials getBootstrapCredentials() {
        return bootstrapCredentials;
    }

    public void saveDeviceCredentials(CumulocityCredentials cumulocityCredentials) {
        File file = new File(devicePropsFile);
        try {
            file.createNewFile();
        } catch (IOException ex) {
            throw new RuntimeException("Cant create file " + devicePropsFile, ex);
        }
        Properties props = new Properties();
        props.setProperty("user", cumulocityCredentials.getUsername());
        props.setProperty("password", cumulocityCredentials.getPassword());
        props.setProperty("tenant", cumulocityCredentials.getTenantId());
        PropUtils.toFile(props, devicePropsFile);
    }
}
