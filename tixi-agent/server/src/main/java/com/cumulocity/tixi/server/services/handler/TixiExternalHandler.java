package com.cumulocity.tixi.server.services.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cumulocity.agent.server.context.DeviceContextService;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.tixi.server.model.SerialNumber;
import com.cumulocity.tixi.server.model.txml.External;
import com.cumulocity.tixi.server.model.txml.External.Bus;
import com.cumulocity.tixi.server.model.txml.External.Device;
import com.cumulocity.tixi.server.services.DeviceService;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TixiExternalHandler extends TixiHandler {

	private static final Logger logger = LoggerFactory.getLogger(TixiExternalHandler.class);
	
	private final Map<SerialNumber, ManagedObjectRepresentation> persistedAgents = new HashMap<>();
	private final Map<SerialNumber, ManagedObjectRepresentation> persistedDevices = new HashMap<>();

	@Autowired
    public TixiExternalHandler(DeviceContextService contextService, DeviceService deviceService, LogDefinitionRegister logDefinitionRegister) {
        super(contextService, deviceService, logDefinitionRegister);
    }

	public void handle(External external) {
		logger.info("Process external file.");
		for (Bus bus : external.getBuses()) {
			logger.info("Process bus {}.", bus);
			for (Device device : bus.getDevices()) {
				handleDevice(bus, device);
			}
			logger.info("External bus {} processed.", bus);
		}
		logger.info("External file processed.");
	}

	private void handleDevice(Bus bus, Device device) {
		logger.debug("Process external device: {} on bus: {}.", device, bus);
		String id = GId.asString(tixiAgentId);
		SerialNumber agentSerial = new SerialNumber(bus.getName() + "_" + id);
		ManagedObjectRepresentation agentRep = persistedAgents.get(agentSerial);
		if (agentRep == null) {
			agentRep = deviceService.saveAgentIfNotExists(agentSerial.getValue(), bus.getName(), agentSerial, tixiAgentId);
			persistedAgents.put(agentSerial, agentRep);
		}
		SerialNumber deviceSerial = new SerialNumber(device.getName() + "_" + id);
		ManagedObjectRepresentation deviceRep = persistedDevices.get(deviceSerial);
		if (deviceRep == null) {
			deviceRep = deviceService.saveDeviceIfNotExists(deviceSerial, device.getName(), agentRep.getId());
			persistedDevices.put(deviceSerial, deviceRep);
		}
		logger.debug("Device processed.");
	}

}