package com.cumulocity.tixi.server.services;

import static com.cumulocity.tixi.server.model.TixiRequestType.EXTERNAL_DATABASE;
import static com.cumulocity.tixi.server.model.TixiRequestType.LOG_DEFINITION;
import static com.cumulocity.tixi.server.resources.TixiRequest.statusOK;
import static org.joda.time.DateTimeConstants.MILLIS_PER_SECOND;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import c8y.inject.DeviceScope;

import com.cumulocity.tixi.server.model.TixiRequestType;
import com.cumulocity.tixi.server.resources.TixiRequest;

@Component
@DeviceScope
public class DeviceMessageChannelService {

    private static final Logger log = LoggerFactory.getLogger(DeviceMessageChannelService.class);

    private BlockingQueue<TixiRequest> requestQueue = new LinkedBlockingQueue<TixiRequest>();

    private TixiRequestFactory requestFactory;

    private volatile MessageChannel<TixiRequest> output;

    protected DeviceMessageChannelService() {
    }

    @Autowired
    public DeviceMessageChannelService(TixiRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

    public void send(TixiRequest tixiRequest) {
        log.debug("Enqueued tixiRequest {}.", tixiRequest);
        try {
            requestQueue.put(tixiRequest);
        } catch (InterruptedException e) {
            log.warn("Enqueu  tixi request failed", e);
        }
    }

    public void send(TixiRequestType requestType) {
        send(requestFactory.create(requestType));
    }

    public void registerMessageOutput(MessageChannel<TixiRequest> output) {
        log.info("Registred new output");
        this.output = output;
        send(statusOK());
        send(EXTERNAL_DATABASE);
        send(LOG_DEFINITION);
    }

    @Scheduled(fixedDelay = 5 * MILLIS_PER_SECOND)
    private void flushRequests() {
        if (output == null) {
            log.debug("no output defined");
            return;
        }
        TixiRequest request = requestQueue.poll();
        if (request != null) {
            log.debug("Send new tixi request {}.", request);
            output.send(new MessageChannelContext() {

                @Override
                public void close() throws IOException {
                    output = null;
                }
            }, request);
        }
    }
}
