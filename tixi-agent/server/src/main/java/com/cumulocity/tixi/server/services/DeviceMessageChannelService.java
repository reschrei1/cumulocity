package com.cumulocity.tixi.server.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import c8y.inject.DeviceScope;

import com.cumulocity.tixi.server.model.TixiRequestType;
import com.cumulocity.tixi.server.resources.TixiRequest;
import com.cumulocity.tixi.server.services.MessageChannel.MessageChannelListener;

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
        flushRequests();
    }

    public void send(TixiRequestType requestType) {
        send(requestFactory.create(requestType));
    }

    public void registerMessageOutput(MessageChannel<TixiRequest> output) {
        log.info("Registred new output");
        this.output = output;
        flushRequests();
    }

    private void flushRequests() {

        if (output == null) {
            log.debug("no output defined");
            return;
        }
        TixiRequest request;
        while ((request = requestQueue.poll()) != null) {
            log.debug("Send new tixi request {}.", request);
            output.send(new MessageChannelListener<TixiRequest>() {

                @Override
                public void close(){
                    output = null;
                }

                @Override
                public void failed(TixiRequest message) {
                    requestQueue.add(message);

                }
            }, request);
        }
    }
}