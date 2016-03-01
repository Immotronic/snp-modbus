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

import org.osgi.framework.BundleContext;
import org.ubikit.Logger;
import org.ubikit.event.EventGate;
import org.ubikit.pem.event.HardwareLinkStatusEvent;
import org.ubikit.service.PhysicalEnvironmentModelService.HardwareLinkStatus;

import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.DataType;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.LinkType;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.facade.ModbusSerialMaster;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.util.BitVector;
import net.wimpi.modbus.util.SerialParameters;

public final class SerialBridge extends ModbusBridge
{
	private static volatile SerialBridge INSTANCE = null;
	
	protected static void init(ScheduledExecutorService executor, BundleContext bc, EventGate higherLevelEventGate, boolean isSerialLineConnectionEnabled)
	{
		if (INSTANCE == null)
		{
			synchronized(SerialBridge.class)
			{
				if (INSTANCE == null) {
					INSTANCE = new SerialBridge(executor, bc, higherLevelEventGate, isSerialLineConnectionEnabled);
				}
			}
		}
	}
	
	protected static SerialBridge getInstance()
	{
		return INSTANCE;
	}
	
	private final boolean isSerialLineConnectionEnabled;
	private final ModbusSerialMaster serialMaster;
	private final BlockingQueue<ModbusRequest> modbusRequestsQueue;
	
	private final EventGate higherLevelEventGate;
	private boolean isConnected = false;
	
	/** 
	 * Create a new SerialBridge. If isSerialLineConnectionEnabled is false, no modbus request will be treated by this SerialBridge.
	 */
	private SerialBridge(ScheduledExecutorService executor, BundleContext bc, EventGate higherLevelEventGate, boolean isSerialLineConnectionEnabled)
	{
		this.isSerialLineConnectionEnabled = isSerialLineConnectionEnabled;
		this.higherLevelEventGate = higherLevelEventGate;
		modbusRequestsQueue = new ArrayBlockingQueue<ModbusRequest>(200);
		
		if  (!isSerialLineConnectionEnabled) {
			serialMaster = null;
			return;
		}
		
		String port = bc.getProperty("fr.immotronic.modbus.port");
		String baudRate = bc.getProperty("fr.immotronic.modbus.baudRate");
		String dataBits = bc.getProperty("fr.immotronic.modbus.dataBits");
		String parity = bc.getProperty("fr.immotronic.modbus.parity");
		String stopBits = bc.getProperty("fr.immotronic.modbus.stopBits");
		String encoding = bc.getProperty("fr.immotronic.modbus.encoding");
		
		assert (port != null && baudRate != null && dataBits != null && parity != null && stopBits != null && encoding != null)
			: "SerialBridge() : Some parameters for serial connection are missing. Please add them in config.properties file.";
				
		SerialParameters parameters = new SerialParameters();
		parameters.setPortName(port);
		parameters.setBaudRate(Integer.parseInt(baudRate));
		parameters.setDatabits(Integer.parseInt(dataBits));
		parameters.setParity(Integer.parseInt(parity));
		parameters.setStopbits(Integer.parseInt(stopBits));
		parameters.setEncoding(encoding);
		parameters.setEcho(false);
		
		serialMaster = new ModbusSerialMaster(parameters);
	
		executor.execute(new SerialLinkInterface(modbusRequestsQueue));
		
		try {
			openConnection();
			isConnected = true;
			higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.CONNECTED, LC.gi().bundleName()));
		} 
		catch (Exception e) {
			isConnected = false;
			higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.DISCONNECTED, LC.gi().bundleName()));
		}
	}
	
	/**
	 * Try to open a connection via the Serial Interface. If the connection is already
	 * opened, it simply returns without doing anything.
	 * An Exception is thrown when the connection attempt fails.
	 * @throws Exception
	 */
	@Override
	protected void openConnection() throws Exception 
	{
		if (serialMaster != null) {
			serialMaster.connect();
		}
	}
	
	protected boolean isConnected()
	{
		return isConnected;
	}

	@Override
	public void terminate()
	{
		
	}
	
	private boolean executeDataRequest(ModbusRequestImpl request)
	{
		try
		{
			int unitIdentifier = Integer.parseInt(request.getSlaveProxy().getAddress());
			if (request.getRequestType() == ModbusRequest.Type.READ)
			{
				BitVector inputData = null;
				InputRegister[] inputRegisters = null;
				
				switch(request.getDataType())
				{
					case DISCRETE_INPUTS:	
						inputData = serialMaster.readInputDiscretes(unitIdentifier, request.getStartingAddress(), request.getQuantity());
						break;
					case COILS:
						inputData = serialMaster.readCoils(unitIdentifier, request.getStartingAddress(), request.getQuantity());
						break;
					case INPUT_REGISTERS:
						inputRegisters = serialMaster.readInputRegisters(unitIdentifier, request.getStartingAddress(), request.getQuantity());
						break;
					case HOLDING_REGISTERS:
						inputRegisters = serialMaster.readMultipleRegisters(unitIdentifier, request.getStartingAddress(), request.getQuantity());
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
					Logger.debug(LC.gi(), this, "Received data is null, modbus request failed ("+request.toString()+")");
					return false;
				}
			}
			else
			{
				switch(request.getDataType())
				{
					case COILS:
						serialMaster.writeMultipleCoils(unitIdentifier, request.getStartingAddress(), BitVector.createBitVector(request.getDataAsByteArray()));
						return true;
					case HOLDING_REGISTERS:
						serialMaster.writeMultipleRegisters(unitIdentifier, request.getStartingAddress(), convertToRegister(request.getDataAsShortArray()));
						return true;
				}
			}
			
			return false;
		}
		catch(NumberFormatException e)
		{
			Logger.error(LC.gi(), this, "SerialBridge : Error when reading Unit Identifier of a Slave Proxy (UID: "+request.getSlaveProxy().getUID()+", Address: "+request.getSlaveProxy().getAddress()+")", e);
			return false;
		}
		catch(ModbusException e)
		{
			Logger.error(LC.gi(), this, "SerialBridge : Request Failed - "+request.toString()+")", e);
			return false;
		}
	}
	
	private class SerialLinkInterface implements Runnable
	{
		private boolean running;
		private final BlockingQueue<ModbusRequest> modbusRequestsQueue;
		
		public SerialLinkInterface(BlockingQueue<ModbusRequest> modbusRequestsQueue)
		{
			this.modbusRequestsQueue = modbusRequestsQueue;
			running = true;
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
					boolean wasConnected = isConnected;
					int maxAttempts = 3;
					
					int attempts = 0;
					do 
					{
						try
						{
							attempts++;
							openConnection();
							isConnected = true;
							
							// If it was disconnected, we send a Connected Event.
							if (!wasConnected)
								higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.CONNECTED, LC.gi().bundleName()));
						}
						catch (Exception e)
						{
							isConnected = false;
							continue;
						}
					}
					while (!isConnected && attempts < maxAttempts);
					
					if (wasConnected && !isConnected) {
						Logger.error(LC.gi(), this, "SerialBridge(): An attempt to open a Serial connection failed. Request is discarded.");
						higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.DISCONNECTED, LC.gi().bundleName()));
						continue;
					}
					
					boolean result = executeDataRequest(request);
					
					if (result && request.getRequestType() == ModbusRequest.Type.READ) {
						request.getSlaveProxy().newIncomingData(SerialBridge.getInstance(), request);
					}
					
					Logger.debug(LC.gi(), this, "Modbus data has been handled with success.");
				}
			}
			
			// At this point, thread has been interrupted. We should close the serial connection.
			if (serialMaster != null) {
				serialMaster.disconnect();
			}
		}
	}
	
	@Override
	public boolean offerDataRequest(ModbusRequest dataRequest)
	{
		// If serial line is not enable, there is nothing to do.
		if (!isSerialLineConnectionEnabled)
			return true;
			
		try 
		{
			ModbusRequestImpl r = (ModbusRequestImpl) dataRequest;
			if (r.getSlaveProxy().getLinkType() == LinkType.Serial)
			{
				if (!modbusRequestsQueue.offer(dataRequest, 100, TimeUnit.MILLISECONDS))
				{
					Logger.warn(LC.gi(), this, "offerDataRequest(): Request data Queue is full, this request will not be treated.");
					return false;
				}
				Logger.debug(LC.gi(), this, "A new data request is queued.");
				return true;
			}
			return false;
		} 
		catch (InterruptedException e)
		{
			Logger.debug(LC.gi(), this, "offerDataRequest(): Interrupted while offering a new request.");
			return false;
		}
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