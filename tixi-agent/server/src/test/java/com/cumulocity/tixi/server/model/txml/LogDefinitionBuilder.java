package com.cumulocity.tixi.server.model.txml;

import com.cumulocity.tixi.server.model.txml.LogDefinition;
import com.cumulocity.tixi.server.model.txml.LogDefinitionItem;
import com.cumulocity.tixi.server.model.txml.LogDefinitionItemSet;

public class LogDefinitionBuilder {

	private final LogDefinition result = new LogDefinition();
	private LogDefinitionItemSet logDefinitionItemSet;

	public static LogDefinitionBuilder aLogDefinition() {
		return new LogDefinitionBuilder();
	}

	public LogDefinitionBuilder withNewItemSet(String id) {
		logDefinitionItemSet = new LogDefinitionItemSet(id);
		result.getItemSets().put(id, logDefinitionItemSet);
		return this;
	}

	public LogDefinitionBuilder withItem(LogDefinitionItem dataLoggingItem) {
		logDefinitionItemSet.getItems().put(dataLoggingItem.getId(), dataLoggingItem);
		return this;
	}
	
	public LogDefinitionBuilder withItem(LogDefinitionItemBuilder dataLoggingItem) {
		return withItem(dataLoggingItem.build());
	}

	public LogDefinition build() {
		return result;
	}

}