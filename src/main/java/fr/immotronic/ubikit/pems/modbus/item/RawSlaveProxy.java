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

package fr.immotronic.ubikit.pems.modbus.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;

import fr.immotronic.ubikit.pems.modbus.DeviceProfile;
import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.event.out.RawDataEvent;
import fr.immotronic.ubikit.pems.modbus.impl.AutomaticReadingManager;
import fr.immotronic.ubikit.pems.modbus.impl.BridgeFactory;
import fr.immotronic.ubikit.pems.modbus.impl.LC;
import fr.immotronic.ubikit.pems.modbus.impl.ModbusBridge;
import fr.immotronic.ubikit.pems.modbus.impl.ModbusRequestImpl;
import fr.immotronic.ubikit.pems.modbus.impl.PemLauncher;

public final class RawSlaveProxy extends ModbusDevice
{
	public enum Field
	{
		UID,
		PROFILE,
		ADDRESS,
		LINKTYPE,
		PROPERTIES,
		CONFIGURATION
	}
	
	protected enum Property
	{
		unitID,
		autoReadingFrequency
	}
	
	protected enum Configuration
	{
		AUTO_READING("auto_reading"),
		FREQUENCY("frequency"),
		ON_CLOCK_TICK("on_clock_tick"),
		TRANSACTIONS("transactions"),
		TRANSACTIONS_TYPE("type"),
		TRANSACTIONS_START_ADDR("start_addr"),
		TRANSACTIONS_QUANTITY("quantity");
		
		private final String name;
		
		Configuration(String name) {
			this.name = name;
		}
		
		public String toString() {
			return name;
		}
	}
	
	private String address;
	private ModbusBridge bridge;
	private LinkType linkType;
	private JSONObject configuration;
	private Boolean automaticReadingOn;
	private int automaticReadingFrequencyInMinutes = 0;
	
	private ModbusDevice linkedDevice = null;
	
	public static String makeModbusID()
	{
		return "MODBUS"+String.format("%012x", System.currentTimeMillis());
	}
	
	public RawSlaveProxy(String address, LinkType linkType, JSONObject userProperties, JSONObject configuration)
	{
		this(makeModbusID(), address, linkType, userProperties, configuration);
	}
	
	public RawSlaveProxy(String slaveUID, String address, LinkType linkType, JSONObject userProperties, JSONObject configuration)
	{
		super(slaveUID, PhysicalEnvironmentItem.Type.OTHER, false, null);
		this.setPropertiesFromJSONObject(userProperties);
		addCapability(DeviceProfile.RAW_SLAVE.name());
		
		this.address = address;
		this.automaticReadingOn = false;
		this.linkType = linkType;
		
		bridge = BridgeFactory.createBridge(address, linkType);
		updateConfiguration(configuration);
	}
	
	protected RawSlaveProxy(ModbusDevice linkedDevice, String address, LinkType linkType, JSONObject configuration)
	{
		super(linkedDevice.getUID()+"_RAW", PhysicalEnvironmentItem.Type.OTHER, false, null);
		
		this.address = address;
		this.automaticReadingOn = false;
		this.linkType = linkType;
		this.linkedDevice = linkedDevice;
		
		bridge = BridgeFactory.createBridge(address, linkType);
		updateConfiguration(configuration);
	}
	
	@Override
	public String getAddress()
	{
		return address;
	}
	
	@Override
	public LinkType getLinkType()
	{
		return linkType;
	}

	@Override
	public JSONObject getConfiguration()
	{
		return configuration;
	}
	
	@Override
	public void updateConfiguration(JSONObject configuration)
	{
		stopAutomaticReading();
		automaticReadingFrequencyInMinutes = 0;
		
		if (configuration == null) {
			this.configuration = null;
			return;
		}

		if (configuration.has(Configuration.AUTO_READING.toString()))
		{
			try
			{
				JSONArray autoReadings = configuration.getJSONArray(Configuration.AUTO_READING.toString());
				for (int i = 0; i < autoReadings.length(); i++)
				{
					Collection<ModbusRequest> requests = new ArrayList<ModbusRequest>();
					JSONObject reading = autoReadings.getJSONObject(i);
					int frequency = reading.getInt(Configuration.FREQUENCY.toString());
					boolean onClockTick = reading.getBoolean(Configuration.ON_CLOCK_TICK.toString());
					JSONArray transactions = reading.getJSONArray(Configuration.TRANSACTIONS.toString());
					for (int j = 0; j < transactions.length(); j++)
					{
						JSONObject t = transactions.getJSONObject(j);
						DataType dataType = DataType.valueOf(t.getString(Configuration.TRANSACTIONS_TYPE.toString()));
						int startingAddress = t.getInt(Configuration.TRANSACTIONS_START_ADDR.toString());
						int quantity = t.getInt(Configuration.TRANSACTIONS_QUANTITY.toString());
						ModbusRequest request = ModbusRequestImpl.createReadRequest(this, bridge, dataType, startingAddress, quantity);
						requests.add(request);
					}
					
					if (!requests.isEmpty()) {
						if (automaticReadingFrequencyInMinutes == 0) {
							automaticReadingFrequencyInMinutes = frequency;
						}
						else {
							automaticReadingFrequencyInMinutes = -1;
						}
						startAutomaticReading(requests, TimeUnit.SECONDS.convert(frequency, TimeUnit.MINUTES), onClockTick);
					}
				}
			}
			catch(JSONException e)
			{
				Logger.warn(LC.gi(), this, "updateConfiguration(): Transmitted JSONObject contains invalid data.");
				stopAutomaticReading();
				return;
			}
			catch(IllegalArgumentException e)
			{
				Logger.warn(LC.gi(), this, "updateConfiguration(): An invalid Type as been set for an automatic transaction.");
				Logger.debug(LC.gi(), this, configuration.toString());
				stopAutomaticReading();
				return;
			}
		}
		
		this.configuration = configuration;
	}
	
	/**
	 * Return the frequency in minutes at which automatic reading is configured. If value is 0, there is no automatic reading.
	 * If value is -1, there is automatic readings but with different frequencies.
	 * @return
	 */
	protected int getAutomaticReadingFrequencyInMinutes()
	{
		return automaticReadingFrequencyInMinutes;
	}
	
	@Override
	public DeviceProfile getProfile()
	{
		return DeviceProfile.RAW_SLAVE;
	}

	public void read(DataType dataType, int startingAddress, int quantity)
	{
		bridge.read(this, dataType, startingAddress, quantity);
	}
	
	public void writeCoils(int startingAddress, byte[] data)
	{
		bridge.writeCoils(this, startingAddress, data);
	}
	
	public void writeRegisters(int startingAddress, short[] data)
	{
		bridge.writeRegisters(this, startingAddress, data);
	}
	
	public void newIncomingData(ModbusBridge bridge, ModbusRequest data)
	{
		// We check that the data is coming from the good bridge.
		if (!this.bridge.equals(bridge))
			return;
		
		if (linkedDevice == null)
		{
			// If no device are internally linked to this RawSlaveProxy, we simply send the response
			// to the modbus request via an event.
			PemLauncher.getHigherEventGate().postEvent(new RawDataEvent(getUID(), data, new Date()));
		}
		else {
			// A device with a known profile is linked to this RawSlaveProxy, we let it handle the
			// data response.
			switch(linkedDevice.getProfile())
			{
				case SDM630: ((SDM630ProxyImpl) linkedDevice).newIncomingData(data);
			}
		}
	}
	
	private void startAutomaticReading(Collection<ModbusRequest> automaticReadingRequests, long automaticReadingFrequencyInSeconds, boolean onClockTick)
	{
		synchronized(automaticReadingOn)
		{
			if (automaticReadingOn) {
				// Automatic reading is already turn on.
				return;
			}
			
			// Schedule readings to happen on a clock tick.
			long initialDelay = 0;
			if (onClockTick)
			{
				long automaticReadingFrequencyInMilliSeconds = TimeUnit.MILLISECONDS.convert(automaticReadingFrequencyInSeconds, TimeUnit.SECONDS);
				long firstExecutionAt = (System.currentTimeMillis()/automaticReadingFrequencyInMilliSeconds + 1)*automaticReadingFrequencyInMilliSeconds;
				initialDelay = TimeUnit.SECONDS.convert(firstExecutionAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS) + 1;
			}
			
			if (automaticReadingRequests != null && automaticReadingRequests.size() != 0 && automaticReadingFrequencyInSeconds > 0)
			{				
				AutomaticReadingManager.getInstance().scheduleAutomaticReading(this, automaticReadingRequests, initialDelay, automaticReadingFrequencyInSeconds);
				automaticReadingOn = true;
			}
		}
	}
	
	private void stopAutomaticReading()
	{
		synchronized(automaticReadingOn)
		{
			if (!automaticReadingOn) {
				// Automatic reading is off, no need to continue.
				return;
			}
			
			AutomaticReadingManager.getInstance().stopAutomaticReading(this);
			automaticReadingOn = false;
		}
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public JSONObject getValueAsJSON() {
		return null;
	}
	
	@Override
	public JSONObject getDataAsJSON() {
		return null;
	}
	
	@Override
	protected void propertiesHaveBeenUpdated(String[] properties)
	{
		PemLauncher.updateDeviceProperties(this, properties);
	}

	@Override
	protected void terminate()
	{
		stopAutomaticReading();
		bridge.terminate();
	}

}
