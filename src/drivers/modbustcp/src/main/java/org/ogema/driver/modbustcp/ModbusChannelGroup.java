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
package org.ogema.driver.modbustcp;

import java.util.ArrayList;
import java.util.List;

import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.util.BitVector;

import org.ogema.core.channelmanager.driverspi.SampledValueContainer;
import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.driver.modbustcp.enums.EFunctionCode;
import org.ogema.driver.modbustcp.enums.EPrimaryTable;

/**
 * Represents a group of channels which is used for a multiple read request
 * 
 * @author Marco Mittelsdorf
 */
public class ModbusChannelGroup {
	private static final int INVALID = -1;

	private EPrimaryTable primaryTable;
	private final ArrayList<ModbusChannel> channels;

	/** Start address to read from */
	private int startAddress;

	/** Number of Registers/Coils to be read from startAddress */
	private int count;

	private int unitId;
	private EFunctionCode functionCode;
	private final String samplingGroup;

	public ModbusChannelGroup(String samplingGroup, ArrayList<ModbusChannel> channels) {
		this.samplingGroup = samplingGroup;
		this.channels = channels;
		setPrimaryTable();
		setUnitId();
		setStartAddress();
		setCount();
		setFunctionCode();
	}

	public String getInfo() {
		String info = "SamplingGroup: '" + samplingGroup + "' Channels: ";
		for (ModbusChannel channel : channels) {
			info += channel.getStartAddress() + ":" + channel.getDatatype() + ", ";
		}
		return info;
	}

	private void setFunctionCode() {

		boolean init = false;
		EFunctionCode tempFunctionCode = null;

		for (ModbusChannel channel : channels) {
			if (!init) {
				tempFunctionCode = channel.getFunctionCode();
				init = true;
			}
			else {
				if (!tempFunctionCode.equals(channel.getFunctionCode())) {
					throw new RuntimeException("FunctionCodes of all channels within the samplingGroup '"
							+ samplingGroup + "' are not equal! Change your openmuc config.");
				}
			}
		}

		functionCode = tempFunctionCode;
	}

	/**
	 * Checks if the primary table of all channels of the sampling group is equal and sets the value for the channel
	 * group.
	 */
	private void setPrimaryTable() {

		boolean init = false;
		EPrimaryTable tempPrimaryTable = null;

		for (ModbusChannel channel : channels) {
			if (!init) {
				tempPrimaryTable = channel.getPrimaryTable();
				init = true;
			}
			else {
				if (!tempPrimaryTable.equals(channel.getPrimaryTable())) {
					throw new RuntimeException("Primary tables of all channels within the samplingGroup '"
							+ samplingGroup + "' are not equal! Change your openmuc config.");
				}
			}
		}

		primaryTable = tempPrimaryTable;
	}

	private void setUnitId() {
		int idOfFirstChannel = INVALID;
		for (ModbusChannel channel : channels) {
			if (idOfFirstChannel == INVALID) {
				idOfFirstChannel = channel.getUnitId();
			}
			else {
				if (channel.getUnitId() != idOfFirstChannel) {

					// TODO ???
					// channel 1 device 1 = unitId 1
					// channel 1 device 2 = unitId 2
					// Does openmuc calls the read method for channels of different devices?
					// If so, then the check for UnitID has to be modified. Only channels of the same device
					// need to have the same unitId...
					throw new RuntimeException("UnitIds of all channels within the samplingGroup '" + samplingGroup
							+ "' are not equal! Change your openmuc config.");
				}
			}
		}
		unitId = idOfFirstChannel;
	}

	/**
	 * StartAddress is the smallest channel address of the group
	 */
	private void setStartAddress() {

		startAddress = INVALID;
		for (ModbusChannel channel : channels) {
			if (startAddress == INVALID) {
				startAddress = channel.getStartAddress();
			}
			else {
				startAddress = Math.min(startAddress, channel.getStartAddress());
			}
		}
	}

	/**
	 *
	 */
	private void setCount() {

		int maximumAddress = startAddress;

		for (ModbusChannel channel : channels) {
			maximumAddress = Math.max(maximumAddress, channel.getStartAddress() + channel.getCount());
		}

		count = maximumAddress - startAddress;
	}

	public void setChannelValues(InputRegister[] inputRegisters, Device device, List<SampledValueContainer> containers) {

		for (ModbusChannel channel : channels) {
			// determine start index of the registers which contain the values of the channel
			int registerIndex = channel.getStartAddress() - getStartAddress();
			// create a temporary register array
			InputRegister[] registers = new InputRegister[channel.getCount()];
			// copy relevant registers for the channel
			for (int i = 0; i < channel.getCount(); i++) {
				registers[i] = inputRegisters[registerIndex + i];
			}
			// now we have a register array which contains the value of the channel
			SampledValueContainer container = searchContainer(channel.getChannelAddress(), containers);
			ModbusDriverUtil util = new ModbusDriverUtil();
			long receiveTime = System.currentTimeMillis();

			Value value = util.getRegistersValue(registers, channel);

			// create SampledValue and add to container
			SampledValue sample = new SampledValue(value, receiveTime, Quality.GOOD);
			container.setSampledValue(sample);
		}
	}

	public void setChannelValues(BitVector bitVector, Device device, List<SampledValueContainer> containers) {

		for (ModbusChannel channel : channels) {

			long receiveTime = System.currentTimeMillis();

			// determine start index of the registers which contain the values of the channel
			int index = channel.getStartAddress() - getStartAddress();

			BooleanValue value = new BooleanValue(bitVector.getBit(index));
			SampledValueContainer container = searchContainer(channel.getChannelAddress(), containers);

			// create SampledValue and add to container
			SampledValue sample = new SampledValue(value, receiveTime, Quality.GOOD);
			container.setSampledValue(sample);
		}
	}

	private SampledValueContainer searchContainer(String channelAddress, List<SampledValueContainer> containers) {
		for (SampledValueContainer container : containers) {
			if (container.getChannelLocator().getChannelAddress().toUpperCase().equals(channelAddress.toUpperCase())) {
				return container;
			}
		}
		throw new RuntimeException("No ChannelRecordContainer found for channelAddress " + channelAddress);
	}

	public boolean isEmpty() {
		boolean result = true;
		if (channels.size() != 0) {
			result = false;
		}
		return result;
	}

	public EPrimaryTable getPrimaryTable() {
		return primaryTable;
	}

	public int getStartAddress() {
		return startAddress;
	}

	public int getCount() {
		return count;
	}

	public int getUnitId() {
		return unitId;
	}

	public EFunctionCode getFunctionCode() {
		return functionCode;
	}

	public ArrayList<ModbusChannel> getChannels() {
		return channels;
	}

}
