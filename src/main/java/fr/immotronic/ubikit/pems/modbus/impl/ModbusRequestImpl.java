/*
 * Copyright (c) Immotronic, 2013
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of snp-modbus, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package fr.immotronic.ubikit.pems.modbus.impl;

import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.DataType;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.LinkType;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;

public final class ModbusRequestImpl implements ModbusRequest
{
	private final RawSlaveProxy slaveProxy;
	private final ModbusBridge bridge;
	private final Type request;
	private final DataType dataType;
	private final int startingAddress;
	private final int quantity;
	
	private byte[] dataC = null;
	private short[] dataR = null;
	
	private ModbusRequestImpl(RawSlaveProxy slaveProxy, ModbusBridge bridge, Type request, DataType dataType, int startingAddress, int quantity, byte[] dataC, short[] dataR)
	{
		this.slaveProxy = slaveProxy;
		this.bridge = bridge;
		this.request = request;
		this.dataType = dataType;
		this.startingAddress = startingAddress;
		this.quantity = quantity;
		this.dataC = dataC;
		this.dataR = dataR;
	}
	
	public static ModbusRequest createReadRequest(RawSlaveProxy slaveProxy, ModbusBridge bridge, DataType dataType, int startingAddress, int quantity)
	{
		if (slaveProxy == null)
			return null;
		
		return new ModbusRequestImpl(slaveProxy, bridge, Type.READ, dataType, startingAddress, quantity, null, null);
	}
	
	public static ModbusRequest createWriteCoilsRequest(RawSlaveProxy slaveProxy, ModbusBridge bridge, int startingAddress, byte[] data)
	{
		if (slaveProxy == null || data == null || data.length == 0)
			return null;
		
		return new ModbusRequestImpl(slaveProxy, bridge, Type.WRITE, DataType.COILS, startingAddress, 0, data, null);
	}
	
	public static ModbusRequest createWriteRegistersRequest(RawSlaveProxy slaveProxy, ModbusBridge bridge, int startingAddress, short[] data)
	{
		if (slaveProxy == null || data == null || data.length == 0)
			return null;
		
		return new ModbusRequestImpl(slaveProxy, bridge, Type.WRITE, DataType.HOLDING_REGISTERS, startingAddress, 0, null, data);
	}
	
	protected RawSlaveProxy getSlaveProxy() {
		return slaveProxy;
	}
	
	protected ModbusBridge getBridge() {
		return bridge;
	}

	public Type getRequestType() {
		return request;
	}
	
	public DataType getDataType() {
		return dataType;
	}

	public int getStartingAddress() {
		return startingAddress;
	}

	public int getQuantity() {
		return quantity;
	}
	
	public byte[] getDataAsByteArray() {
		return dataC;
	}
	
	public short[] getDataAsShortArray() {
		return dataR;
	}
	
	protected void setData(byte[] data) {
		if (dataType == DataType.COILS || dataType == DataType.DISCRETE_INPUTS) {
			this.dataC = data;
		}
	}
	
	protected void setData(short[] data) {
		if (dataType == DataType.INPUT_REGISTERS || dataType == DataType.HOLDING_REGISTERS) {
			this.dataR = data;
		}
	}
	
	@Override
	public String toString()
	{
		String result = "Request ";
		if (slaveProxy.getLinkType() == LinkType.Serial){
			result += "(dest: unitID="+slaveProxy.getAddress()+"): ";
		}
		else {
			result += "(dest: TCP/UDP addr="+slaveProxy.getAddress()+"): ";
		}
		result += "type="+request+", table="+dataType+", startingAddress="+startingAddress+", quantity="+quantity;
		return result;
	}
	
}
