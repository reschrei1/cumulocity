package c8y.remoteaccess.tunnel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.util.Base64;

public class AuthHeaderConfigurator extends ClientEndpointConfig.Configurator {

    private static final Logger logger = LoggerFactory.getLogger(AuthHeaderConfigurator.class);

    private String basicAuth;

    public AuthHeaderConfigurator(String username, String password) {
        this.basicAuth = new String(Base64.encode(username + ":" + password));
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Authorization", Arrays.asList("Basic " + basicAuth));
        logger.debug("Headers: {}", headers);
        super.beforeRequest(headers);
    }
}
