/**
 * This file is part of OGEMA.
 *
 * OGEMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OGEMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OGEMA. If not, see <http://www.gnu.org/licenses/>.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.09.13 at 08:01:54 AM CEST 
//

package org.ogema.serialization.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ScheduleResource complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScheduleResource">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.ogema-source.net/REST}Resource">
 *       &lt;sequence>
 *         &lt;element name="lastUpdateTime" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="lastCalculationTime" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="entry" type="{http://www.ogema-source.net/REST}SampledValue" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScheduleResource", propOrder = { "interpolationMode", "lastUpdateTime", "lastCalculationTime",
		"start", "end", "entry" })
@XmlSeeAlso( { StringSchedule.class, BooleanSchedule.class, IntegerSchedule.class, OpaqueSchedule.class,
		TimeSchedule.class, FloatSchedule.class })
public abstract class ScheduleResource extends Resource {

	protected String interpolationMode;
	protected Long lastUpdateTime;
	protected Long lastCalculationTime;
	protected Long start;
	protected Long end;
	@XmlElement(required = true)
	protected List<SampledValue> entry;

	/**
	 * Ruft den Wert der interpolationMode-Eigenschaft ab.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getInterpolationMode() {
		return interpolationMode;
	}

	/**
	 * Legt den Wert der interpolationMode-Eigenschaft fest.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setInterpolationMode(String value) {
		this.interpolationMode = value;
	}

	/**
	 * Gets the value of the lastUpdateTime property.
	 * 
	 * @return possible object is {@link Long }
	 * 
	 */
	public Long getLastUpdateTime() {
		return lastUpdateTime;
	}

	/**
	 * Sets the value of the lastUpdateTime property.
	 * 
	 * @param value
	 *            allowed object is {@link Long }
	 * 
	 */
	public void setLastUpdateTime(Long value) {
		this.lastUpdateTime = value;
	}

	/**
	 * Gets the value of the lastCalculationTime property.
	 * 
	 * @return possible object is {@link Long }
	 * 
	 */
	public Long getLastCalculationTime() {
		return lastCalculationTime;
	}

	/**
	 * Sets the value of the lastCalculationTime property.
	 * 
	 * @param value
	 *            allowed object is {@link Long }
	 * 
	 */
	public void setLastCalculationTime(Long value) {
		this.lastCalculationTime = value;
	}

	public Long getStart() {
		return start;
	}

	public void setStart(Long start) {
		this.start = start;
	}

	public Long getEnd() {
		return end;
	}

	public void setEnd(Long end) {
		this.end = end;
	}

	/**
	 * Gets the value of the entry property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the entry property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getEntry().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link SampledValue }
	 * 
	 * 
	 */
	public List<SampledValue> getEntry() {
		if (entry == null) {
			entry = new ArrayList<>();
		}
		return this.entry;
	}
}
