package com.cumulocity.tixi.server.model.txml.logdefinition;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DataLogging")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataLogging {
	
	@XmlAttribute
	private String id;
	
	@XmlElements({ @XmlElement(name = "DataloggingItem") })
	private List<DataLoggingItem> items = new ArrayList<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<DataLoggingItem> getItems() {
		return items;
	}

	public void setItems(List<DataLoggingItem> items) {
		this.items = items;
	}

	@Override
    public String toString() {
	    return String.format("DataLogging [id=%s, items=%s]", id, items);
    }

	@Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((items == null) ? 0 : items.hashCode());
	    result = prime * result + ((id == null) ? 0 : id.hashCode());
	    return result;
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    DataLogging other = (DataLogging) obj;
	    if (items == null) {
		    if (other.items != null)
			    return false;
	    } else if (!items.equals(other.items))
		    return false;
	    if (id == null) {
		    if (other.id != null)
			    return false;
	    } else if (!id.equals(other.id))
		    return false;
	    return true;
    }
}