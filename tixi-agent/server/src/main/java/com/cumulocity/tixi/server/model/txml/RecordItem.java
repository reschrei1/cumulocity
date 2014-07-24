package com.cumulocity.tixi.server.model.txml;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.cumulocity.tixi.server.components.txml.TXMLValueAdaper;

public class RecordItem extends LogBaseItem {

	@XmlAttribute
	@XmlJavaTypeAdapter(TXMLValueAdaper.class)
	private BigDecimal value;
	
	public RecordItem() {}
	
	public RecordItem(String id, BigDecimal value) {
	    super(id);
	    this.value = value;
    }
	
	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	@Override
    public String toString() {
	    return String.format("LogItem [id=%s, value=%s]", id, value);
    }

	@Override
    public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((id == null) ? 0 : id.hashCode());
	    result = prime * result + ((value == null) ? 0 : value.hashCode());
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
	    RecordItem other = (RecordItem) obj;
	    if (id == null) {
		    if (other.id != null)
			    return false;
	    } else if (!id.equals(other.id))
		    return false;
	    if (value == null) {
		    if (other.value != null)
			    return false;
	    } else if (!value.equals(other.value))
		    return false;
	    return true;
    }
}