package com.cumulocity.tixi.server.services;

import static com.google.common.base.Enums.getIfPresent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cumulocity.tixi.server.model.TixiRequestType;
import com.cumulocity.tixi.server.model.txml.LogDefinition;
import com.cumulocity.tixi.server.request.util.RequestIdFactory;
import com.cumulocity.tixi.server.request.util.RequestStorage;
import com.cumulocity.tixi.server.resources.TixiRequest;
import com.google.common.base.Optional;

@Component
public class TixiRequestFactory {

    private final RequestIdFactory requestIdFactory;
    
    private final RequestStorage requestStorage;
    
    @Autowired
    public TixiRequestFactory(RequestIdFactory requestIdFactory, RequestStorage requestStorage) {
        this.requestIdFactory = requestIdFactory;
        this.requestStorage = requestStorage;
    }
    
	public TixiRequest create(String requestType) {
		Optional<TixiRequestType> requestTypeObj = getIfPresent(TixiRequestType.class, requestType);
		if(requestTypeObj.isPresent()) {
			return create(requestTypeObj.get());
		} else {
			throw new RuntimeException("Unknown request type");
		}
    }
    
    public TixiRequest create(TixiRequestType requestType) {
        if (requestType == TixiRequestType.EXTERNAL_DATABASE) {
            return createExternalDBRequest();
        }
        if (requestType == TixiRequestType.LOG_DEFINITION) {
            return createLogDefinitionRequest();
        }
        if (requestType == TixiRequestType.LOG) {
        	return createLogRequest();
        }
        throw new RuntimeException("Unknown request type");
    }
    
    private TixiRequest createLogDefinitionRequest() {
        String requestId = requestIdFactory.get().toString();
        requestStorage.put(requestId, LogDefinition.class);
        return new TixiRequest("TiXML")
        	.set("requestId", requestId)
        	.set("parameter", "[<GetConfig _=\"LOG/LogDefinition\" ver=\"v\"/>]");
    }

    private TixiRequest createExternalDBRequest() {
        String requestId = requestIdFactory.get().toString();
        return new TixiRequest("TiXML")
        	.set("requestId", requestId)
        	.set("parameter", "[<GetConfig _=\"PROCCFG/External\" ver=\"v\"/>]");
    }
    
    private TixiRequest createLogRequest() {
    	String requestId = requestIdFactory.get().toString();
    	return new TixiRequest("TiXML")
	    	.set("requestId", requestId)
	    	.set("parameter", "[<GetConfig _=\"LOG/EventLogging\" ver=\"v\"/>]");
    }
}
