package com.cumulocity.tixi.server.services.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import c8y.IsDevice;

import com.cumulocity.agent.server.context.DeviceContextService;
import com.cumulocity.agent.server.repository.IdentityRepository;
import com.cumulocity.agent.server.repository.InventoryRepository;
import com.cumulocity.model.Agent;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.tixi.server.model.SerialNumber;
import com.cumulocity.tixi.server.model.txml.LogDefinition;
import com.cumulocity.tixi.server.model.txml.LogDefinitionItem;
import com.cumulocity.tixi.server.model.txml.LogDefinitionItemSet;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TixiLogDefinitionHandler extends TixiHandler<LogDefinition> {

	private static final Logger logger = LoggerFactory.getLogger(TixiLogDefinitionHandler.class);

	@Autowired
	public TixiLogDefinitionHandler(DeviceContextService deviceContextService, IdentityRepository identityRepository, InventoryRepository inventoryRepository,
	        MeasurementApi measurementApi, LogDefinitionRegister logDefinitionRegister) {
		super(deviceContextService, identityRepository, inventoryRepository, measurementApi, logDefinitionRegister);
	}

	public void handle(LogDefinition logDefinition) {
		logDefinitionRegister.register(logDefinition);
		for (LogDefinitionItemSet itemSet : logDefinition.getItemSets().values()) {
			for (LogDefinitionItem logDefinitionItem : itemSet.getItems().values()) {
				if (isDevicePath(logDefinitionItem)) {
					handleDeviceItem(logDefinitionItem);
				}
			}
		}
	}

	private void handleDeviceItem(LogDefinitionItem logDefinitionItem) {
		String agentId = logDefinitionItem.getPath().getAgentId();
		SerialNumber agentSerial = new SerialNumber(agentId);
		GId agentGId = identityRepository.find(agentSerial);
		if (agentGId == null) {
			agentGId = registerAgent(agentSerial);
		}
		String deviceId = logDefinitionItem.getPath().getDeviceId();
		SerialNumber deviceSerial = new SerialNumber(deviceId);
		GId deviceGId = identityRepository.find(deviceSerial);
		if (deviceGId == null) {
			deviceGId = registerDevice(agentGId, deviceSerial);
		}
	}

	private GId registerDevice(GId agentId, SerialNumber deviceSerial) {
		ManagedObjectRepresentation managedObjectRepresentation = new ManagedObjectRepresentation();
		managedObjectRepresentation.set(new IsDevice());
		final ManagedObjectRepresentation managedObject = inventoryRepository.save(managedObjectRepresentation);
		identityRepository.save(managedObject.getId(), deviceSerial);
		inventoryRepository.bindToAgent(agentId, managedObject.getId());
		return managedObject.getId();
	}

	private GId registerAgent(SerialNumber agentSerial) {
		ManagedObjectRepresentation managedObjectRepresentation = new ManagedObjectRepresentation();
		managedObjectRepresentation.set(new IsDevice());
		managedObjectRepresentation.set(new Agent());
		final ManagedObjectRepresentation managedObject = inventoryRepository.save(managedObjectRepresentation);
		identityRepository.save(managedObject.getId(), agentSerial);
		return managedObject.getId();
	}
}
