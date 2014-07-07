package com.cumulocity.tixi.server.model.txml.log;

import static com.cumulocity.tixi.server.components.txml.TXMLDateAdapter.DATE_FORMAT;

public class LogBuilder {
	
	private final Log log = new Log();
	private LogItemSet itemSet;
	
	public static LogBuilder aLog() {
		return new LogBuilder();
	}
	
	public LogBuilder withNewItemSet(String id, String dateTime) throws Exception {
		itemSet = new LogItemSet(id, DATE_FORMAT.parse(dateTime));
		log.getItemSets().add(itemSet);
		return this;
	}
	
	public LogBuilder withId(String id) {
		log.setId(id);
		return this;
	}
	
	public LogBuilder withItem(String id, String value) {
		LogItem logItem = new LogItem(id, value);
		itemSet.getItems().add(logItem);
		return this;
	}

	
	public Log build() {
		return log;
	}

}