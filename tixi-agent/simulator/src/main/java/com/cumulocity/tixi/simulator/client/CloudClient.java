package com.cumulocity.tixi.simulator.client;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cumulocity.tixi.simulator.config.Main;
import com.cumulocity.tixi.simulator.model.ResponseHandlerFactory;
import com.cumulocity.tixi.simulator.model.TixiCredentials;
import com.cumulocity.tixi.simulator.model.TixiResponse;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class CloudClient {

    private static final Logger logger = LoggerFactory.getLogger(CloudClient.class);

    private String baseUrl;

    private TixiCredentials credentials;

    private ResponseHandlerFactory responseHandlerFactory = new ResponseHandlerFactory();
    
    public CloudClient(String baseUrl) {
        this(baseUrl,null);
    }
    
    public CloudClient(String baseUrl,TixiCredentials tixiCredentials) {
        this.baseUrl = baseUrl;
        credentials = tixiCredentials;
    }

    private Client client = ClientBuilder.newClient()
    		.register(JacksonJsonProvider.class)
    		.register(MultiPartFeature.class)
            .register(SseFeature.class);

    public void sendRegisterRequest() {
        if(credentials ==null) {
            String uri = baseUrl + "/Tixi/register?serial=" + Main.DEVICE_SERIAL;
            logger.info("Send bootstrap request to {}", uri);
    		Response response = client.target(uri).request().get();
            credentials = response.readEntity(TixiCredentials.class);
            logger.info("Bootstraped creentials {}", credentials);
        }else {
            String uri = baseUrl + String.format("/Tixi/register?serial=%s&deviceID=%s&user=%s&password=%s",
                    Main.DEVICE_SERIAL, credentials.deviceID, credentials.user, credentials.password);
            logger.info("Send standard register request to {}", uri);
            Response response = client.target(uri).request().get();
            TixiResponse tixiResponse = response.readEntity(TixiResponse.class);
            credentials.deviceID = tixiResponse.getDeviceID();
            logger.info("registred {}", tixiResponse);
        }
    }

    public void sendOpenChannel() {
        String uri = baseUrl + String.format("/Tixi/openchannel?serial=%s&user=%s&password=%s&deviceID=%s",
                        Main.DEVICE_SERIAL, credentials.user, credentials.password, credentials.deviceID);
        logger.info("Send open channel request to {}", uri);
        Response response = client.target(uri).request().get();
        final ChunkedInput<TixiResponse> chunkedInput =
                response.readEntity(new GenericType<ChunkedInput<TixiResponse>>() {});
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            
            @Override
            public void run() {
                TixiResponse chunk;
                /*ChunkParser parser = ChunkedInput.createParser(" ");
                chunkedInput.setParser(parser);*/
                while ((chunk = chunkedInput.read()) != null) {
                    responseHandlerFactory.getHandler(chunk).handle(CloudClient.this, chunk);
                }
            }
        });
    }

    public void postExternalDatabaseData(TixiResponse response) {
        sendMultipartRequest(response, "external_database.xml.gz");
    }

    public void postLogDefinitionData(TixiResponse response) {
        sendMultipartRequest(response, "log_definition.xml.gz");
    }

    public void postLogFileData() {
        sendMultipartRequest("sample_data.xml.gz");
    }

    private void sendMultipartRequest(String filename) {
        String requestUrl = baseUrl
                + String.format("/Tixi/senddata?serial=%s&deviceID=%s&user=%s&password=%s", Main.DEVICE_SERIAL,
                        credentials.deviceID, credentials.user, credentials.password);
        sendMultipartRequest(requestUrl, filename);

    }

    private void sendMultipartRequest(TixiResponse response, String filename) {
        String requestUrl = baseUrl
                + String.format("/Tixi/senddata?serial=%s&deviceID=%s&user=%s&password=%s&requestId=%s",
                        Main.DEVICE_SERIAL, credentials.deviceID, credentials.user, credentials.password, response.getRequestId());
        sendMultipartRequest(requestUrl, filename);
    }

    private void sendMultipartRequest(String requestUrl, String filename) {
    	logger.info("Send request to {} from file: {}.", requestUrl, filename);
        FormDataMultiPart multipart = null;
        try {
            multipart = new FormDataMultiPart();
            FileDataBodyPart filePart = new FileDataBodyPart("sendfile", getFile(filename));
            MultiPart bodyPart = multipart.bodyPart(filePart);
            WebTarget target = client.target(requestUrl);
            target.request().post(Entity.entity(bodyPart, bodyPart.getMediaType()));
        } finally {
            if (multipart != null) {
                try {
                    multipart.close();
                } catch (IOException e) {
                	logger.error("", e);
                }
            }
        }
    }

    public File getFile(String filename) {
        try {
            return new File(this.getClass().getClassLoader().getResource("requests/" + filename).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException();
        }
    }
}
