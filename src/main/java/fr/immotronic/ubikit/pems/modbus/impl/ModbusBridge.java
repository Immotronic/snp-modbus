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

import java.util.Collection;

import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleRegister;

import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.DataType;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;

public abstract class ModbusBridge
{	
	protected abstract void openConnection() throws Exception;
	
	public abstract void terminate();
	
	public abstract boolean offerDataRequest(ModbusRequest dataRequest);
	
	public abstract boolean offerDataRequests(Collection<ModbusRequest> dataRequest);
	
	public abstract boolean read(RawSlaveProxy slaveProxy, DataType dataType, int startingAddress, int quantity);
	
	public abstract boolean writeCoils(RawSlaveProxy slaveProxy, int startingAddress, byte[] data);
	
	public abstract boolean writeRegisters(RawSlaveProxy slaveProxy, int startingAddress, short[] data);

	protected static short[] convertToShortArray(InputRegister[] inputRegisters)
	{
		if (inputRegisters == null)
			return null;
		
		short[] array = new short[inputRegisters.length];
		for(int i = 0; i < inputRegisters.length; i++) {
			array[i] = inputRegisters[i].toShort();
		}
		return array;
	}
	
	protected static Register[] convertToRegister(short[] array)
	{
		if (array == null)
			return null;
		
		Register[] registers = new Register[array.length];
		for(int i = 0; i < array.length; i++) {
			registers[i] = new SimpleRegister(array[i]);
		}
		return registers;
	}
}
