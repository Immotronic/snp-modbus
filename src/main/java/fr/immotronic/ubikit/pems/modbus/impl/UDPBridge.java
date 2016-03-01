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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.DataType;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.LinkType;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.facade.ModbusUDPMaster;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.util.BitVector;

public final class UDPBridge extends ModbusBridge
{
	private static UDPLinkInterface udpLinkInterface = null;
	private final ModbusUDPMaster udpMaster;
	
	protected UDPBridge(String address, int port, ScheduledExecutorService executor)
	{	
		assert (address != null) : "UDPBridge() : Device address can't be null.";
		
		udpMaster = new ModbusUDPMaster(address, port);
		
		if (udpLinkInterface == null)
		{
			synchronized(udpLinkInterface)
			{
				if (udpLinkInterface == null)
				{
					udpLinkInterface = new UDPLinkInterface();
					executor.execute(udpLinkInterface);
				}
			}
		}
	}
	
	/**
	 * Try to open a connection via the UDP Interface. If the connection is already
	 * opened, it simply returns without doing anything.
	 * An Exception is thrown when the connection attempt fails.
	 * @throws Exception
	 */
	@Override
	protected void openConnection() throws Exception
	{
		udpMaster.connect();
	}

	@Override
	public void terminate()
	{
		udpMaster.disconnect();
	}
	
	protected boolean executeDataRequest(ModbusRequestImpl request)
	{
		try
		{
			if (request.getRequestType() == ModbusRequest.Type.READ)
			{
				BitVector inputData = null;
				InputRegister[] inputRegisters = null;
				
				switch(request.getDataType())
				{
					case DISCRETE_INPUTS:	
						inputData = udpMaster.readInputDiscretes(request.getStartingAddress(), request.getQuantity());
						break;
					case COILS:
						inputData = udpMaster.readCoils(request.getStartingAddress(), request.getQuantity());
						break;
					case INPUT_REGISTERS:
						inputRegisters = udpMaster.readInputRegisters(request.getStartingAddress(), request.getQuantity());
						break;
					case HOLDING_REGISTERS:
						inputRegisters = udpMaster.readMultipleRegisters(request.getStartingAddress(), request.getQuantity());
						break;
				}
				
				if (inputData != null) {
					request.setData(inputData.getBytes());
					return true;
				}
				else if (inputRegisters != null) {
					request.setData(convertToShortArray(inputRegisters));
					return true;
				}
				else {
					Logger.debug(LC.gi(), null, "executeDataRequest(): Received data is null, modbus request failed ("+request.toString()+")");
					return false;
				}
			}
			else
			{
				switch(request.getDataType())
				{
					case COILS:
						udpMaster.writeMultipleCoils(request.getStartingAddress(), BitVector.createBitVector(request.getDataAsByteArray()));
						return true;
					case HOLDING_REGISTERS:
						udpMaster.writeMultipleRegisters(request.getStartingAddress(), convertToRegister(request.getDataAsShortArray()));
						return true;
				}
			}
			
			return false;
		}
		catch(ModbusException e)
		{
			Logger.error(LC.gi(), null, "executeDataRequest(): UDP Request Failed ("+request.toString()+")", e);
			return false;
		}
	}
	
	private static class UDPLinkInterface implements Runnable
	{
		private boolean running;
		private final BlockingQueue<ModbusRequest> modbusRequestsQueue;
		
		public UDPLinkInterface()
		{
			modbusRequestsQueue = new ArrayBlockingQueue<ModbusRequest>(50);
			running = true;
		}
		
		public boolean offerDataRequest(ModbusRequest dataRequest)
		{
			try 
			{
				if (!modbusRequestsQueue.offer(dataRequest, 100, TimeUnit.MILLISECONDS))
				{
					Logger.warn(LC.gi(), this, "offerDataRequest(): Request data Queue is full, this request will not be treated.");
					return false;
				}
				Logger.debug(LC.gi(), this, "A new data request is queued.");
				return true;
			} 
			catch (InterruptedException e)
			{
				Logger.debug(LC.gi(), this, "offerDataRequest(): Interrupted while offering a new request.");
				return false;
			}
		}
		
		@Override
		public void run()
		{
			while(!Thread.currentThread().isInterrupted() && running)
			{
				ModbusRequestImpl request = null;
				
				try
				{
					request = (ModbusRequestImpl) modbusRequestsQueue.take();
				} 
				catch (InterruptedException e) {
					running = false;
				}
				  
				if (request != null)
				{
					boolean connectionOpened = false;
					int maxAttempts = 3;
					
					int attempts = 0;
					while (!connectionOpened && attempts < maxAttempts)
					{
						try
						{
							attempts++;
							request.getBridge().openConnection();
							connectionOpened = true;
						}
						catch (Exception e)
						{
							continue;
						}
					}
					
					if (!connectionOpened) {
						Logger.error(LC.gi(), this, "UDPLinkInterface: An attempt to open a UDP connection failed. Request is discarded.");
						continue;
					}
					
					boolean result = ((UDPBridge) request.getBridge()).executeDataRequest(request);
					
					if (result && request.getRequestType() == ModbusRequest.Type.READ) {
						request.getSlaveProxy().newIncomingData(request.getBridge(), request);
					}
					
					Logger.debug(LC.gi(), this, "Modbus data has been handled with success.");
				}
			}
		}
	}
	
	@Override
	public boolean offerDataRequest(ModbusRequest dataRequest)
	{
		ModbusRequestImpl r = (ModbusRequestImpl) dataRequest;
		if (r.getSlaveProxy().getLinkType() == LinkType.UDP)
		{
			return udpLinkInterface.offerDataRequest(dataRequest);
		}
		
		return false;
	}
	
	@Override
	public boolean offerDataRequests(Collection<ModbusRequest> dataRequests)
	{
		boolean result = true;
		for (ModbusRequest request : dataRequests) {
			result &= offerDataRequest(request);
		}
		return result;
	}


	@Override
	public boolean read(RawSlaveProxy slaveProxy, DataType dataType, int startingAddress, int quantity)
	{
		ModbusRequest dataRequest = ModbusRequestImpl.createReadRequest(slaveProxy, this, dataType, startingAddress, quantity);
		return offerDataRequest(dataRequest);
	}


	@Override
	public boolean writeCoils(RawSlaveProxy slaveProxy, int startingAddress, byte[] data)
	{
		ModbusRequest dataRequest = ModbusRequestImpl.createWriteCoilsRequest(slaveProxy, this, startingAddress, data);
		return offerDataRequest(dataRequest);
	}


	@Override
	public boolean writeRegisters(RawSlaveProxy slaveProxy, int startingAddress, short[] data)
	{
		ModbusRequest dataRequest = ModbusRequestImpl.createWriteRegistersRequest(slaveProxy, this, startingAddress, data);
		return offerDataRequest(dataRequest);
	}

}
