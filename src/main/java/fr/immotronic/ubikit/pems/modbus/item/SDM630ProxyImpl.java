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

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;

import fr.immotronic.ubikit.pems.modbus.DeviceProfile;
import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.event.out.SDM630DataEvent;
import fr.immotronic.ubikit.pems.modbus.impl.LC;
import fr.immotronic.ubikit.pems.modbus.impl.PemLauncher;
import fr.immotronic.ubikit.pems.modbus.item.data.SDM630ProxyData;
import fr.immotronic.ubikit.pems.modbus.item.data.SDM630Data;

public final class SDM630ProxyImpl extends ModbusDevice
{	
	private final static DecimalFormat df = new DecimalFormat("#######.###");
	
	public enum ConnectionType
	{
		_1PHASE,
		_3PHASE_3WIRE,
		_3PHASE_4WIRE,
		NOT_SPECIFIED
	}
	
	private static final int[] start_addrs = new int[] { 0x00, 0x2e, 0xc8, 0xe0, 0xea, 0x14e };
	private static final int[] quantities = new int[] { 44, 62, 8, 2, 36, 8 };
	
	private final RawSlaveProxy rawProxy;
	private SDM630ProxyData lastData = null;
	private Date lastDataDate = null;
	
	/**
	 * Construct a new SDM630ProxyImpl with given properties.
	 * @param unitID Device unit identifier
	 * @param userProperties Should at least contains a key "connection"
	 * @param configuration Device configuration
	 */
	public SDM630ProxyImpl(String unitID, JSONObject userProperties, JSONObject configuration)
	{
		this(RawSlaveProxy.makeModbusID(), unitID, userProperties, configuration);
	}
	
	/**
	 * Construct a new SDM630ProxyImpl with given properties.
	 * @param unitID Device unit identifier
	 * @param userProperties Should at least contains a key "connection"
	 * @param automaticReadingFrequencyInMinutes Frequency at which perform auto reading.
	 */
	public SDM630ProxyImpl(String unitID, JSONObject userProperties, int automaticReadingFrequencyInMinutes)
	{
		this(RawSlaveProxy.makeModbusID(), unitID, userProperties, generateDefaultConfiguration(automaticReadingFrequencyInMinutes));
	}
	
	/**
	 * Construct a new SDM630ProxyImpl with given properties.
	 * @param itemUID Device Unique identifier
	 * @param unitID Device unit identifier
	 * @param userProperties Should at least contains a key "connection"
	 * @param configuration Device configuration
	 */
	public SDM630ProxyImpl(String itemUID, String unitID, JSONObject userProperties, JSONObject configuration)
	{
		super(itemUID, PhysicalEnvironmentItem.Type.OTHER, false, null);
		addCapability(DeviceProfile.SDM630.name());
		
		rawProxy = new RawSlaveProxy(this, unitID, LinkType.Serial, configuration);
		
		setPropertiesFromJSONObject(userProperties);
		setPropertyValue(RawSlaveProxy.Property.unitID.name(), unitID);
		setPropertyValue(RawSlaveProxy.Property.autoReadingFrequency.name(), String.valueOf(rawProxy.getAutomaticReadingFrequencyInMinutes()));
	}
	
	public void updateData()
	{
		for (int i = 0; i < start_addrs.length; i++)
		{
			rawProxy.read(DataType.INPUT_REGISTERS, start_addrs[i], quantities[i]);
		}
	}

	protected void newIncomingData(ModbusRequest data)
	{
		Logger.debug(LC.gi(), this, "Received new modbus data: "+data.toString());
		try
		{
			int startingAddress = data.getStartingAddress();
			int quantity = data.getQuantity();
			
			if (startingAddress == start_addrs[0]) {
				lastData = new SDM630ProxyData();
			}
			
			if (data.getDataType() == DataType.INPUT_REGISTERS)
			{
				short[] response = data.getDataAsShortArray();
				if (response != null)
				{
					lastData.fillData(response, startingAddress, quantity);
				}
				else {
					Logger.debug(LC.gi(), this, "newIncomingData(): The received response seems to have null data. This is not expected.");
				}
			}
			
			if (startingAddress == start_addrs[start_addrs.length-1])
			{
				// We receive a complete set of data. We sent them to the higher level.
				lastDataDate = new Date();
				PemLauncher.getHigherEventGate().postEvent(new SDM630DataEvent(getUID(), getDataAsJSON(), lastDataDate));
			}
		}
		catch(Exception e)
		{
			Logger.error(LC.gi(), this, "newIncomingData(): An unexpected error happened when handling a response data.", e);
		}
	}
	
	@Override
	public DeviceProfile getProfile()
	{
		return DeviceProfile.SDM630;
	}

	@Override
	public String getAddress()
	{
		return rawProxy.getAddress();
	}

	@Override
	public LinkType getLinkType()
	{
		return rawProxy.getLinkType();
	}

	@Override
	public JSONObject getConfiguration()
	{
		return rawProxy.getConfiguration();
	}
	
	public ConnectionType getConnectionType()
	{
		String connection = getPropertyValue("connection");
		if (connection != null)
			return ConnectionType.valueOf("_"+connection);
		else
			return ConnectionType.NOT_SPECIFIED;
	}

	@Override
	public void updateConfiguration(JSONObject configuration)
	{
		rawProxy.updateConfiguration(configuration);
		int frequency = rawProxy.getAutomaticReadingFrequencyInMinutes();
		if (frequency > 0)
			setPropertyValue(RawSlaveProxy.Property.autoReadingFrequency.name(), String.valueOf(rawProxy.getAutomaticReadingFrequencyInMinutes()));
	}
	
	@Override
	public JSONObject getDataAsJSON()
	{
		return (JSONObject) JSONObject.wrap(lastData.getValue());
	}
	
	@Override
	public Map<String, Float> getValue()
	{
		if (lastData != null) {
			return Collections.unmodifiableMap(lastData.getValue());
		}
		
		return null;
	}

	@Override
	public JSONObject getValueAsJSON()
	{
		JSONObject res = new JSONObject();
		JSONObject o = null;
		
		try 
		{
			if(lastDataDate == null) 
			{
				res.put("noDataAvailable", true);
				return res;
			}
			
			Map<String, Float> values = lastData.getValue();
			for (Entry<String, Float> value : values.entrySet())
			{
				int valueIndex = Integer.parseInt(value.getKey());
				
				o = new JSONObject();
				o.put(JSONValueField.value.name(), value.getValue());
				o.put(JSONValueField.uiValue.name(), df.format(value.getValue()));
				o.put(JSONValueField.unit.name(), " "+SDM630Data.getValue(valueIndex).getUnit());
				o.put(JSONValueField.timestamp.name(), lastDataDate.getTime());
				res.put(String.valueOf(SDM630Data.getValue(valueIndex).toString()), o);
			}
		}
		catch (JSONException e) {
			Logger.error(LC.gi(), this, "toJSON(): An exception while building a JSON view of a EnoceanData SHOULD never happen. Check the code !", e);
			return null;
		}
		
		return res;
	}

	@Override
	protected void propertiesHaveBeenUpdated(String[] properties)
	{
		PemLauncher.updateDeviceProperties(this, properties);
	}

	@Override
	protected void terminate()
	{
		rawProxy.terminate();
	}
	
	/**
	 * Returns a JSONObject containing the configuration to read automatically all relevant data
	 * every <i>automaticReadingFrequencyInSeconds</i> seconds.
	 * @param automaticReadingFrequencyInSeconds Frequency at which automatic read is done.
	 * @return a JSONObject containing the configuration to read automatically all relevant data
	 * every <i>automaticReadingFrequencyInSeconds</i> seconds.
	 */
	private static JSONObject generateDefaultConfiguration(int automaticReadingFrequencyInMinutes)
	{
		if (automaticReadingFrequencyInMinutes > 0)
		{
			try
			{
				JSONArray transactions = new JSONArray();
				for (int i = 0; i < start_addrs.length; i++)
				{
					JSONObject transaction = new JSONObject();
					transaction.put(RawSlaveProxy.Configuration.TRANSACTIONS_TYPE.toString(), DataType.INPUT_REGISTERS.name());
					transaction.put(RawSlaveProxy.Configuration.TRANSACTIONS_START_ADDR.toString(), start_addrs[i]);
					transaction.put(RawSlaveProxy.Configuration.TRANSACTIONS_QUANTITY.toString(), quantities[i]);
					transactions.put(transaction);
				}
				
				JSONObject autoReading = new JSONObject();
				autoReading.put(RawSlaveProxy.Configuration.FREQUENCY.toString(), automaticReadingFrequencyInMinutes);
				autoReading.put(RawSlaveProxy.Configuration.ON_CLOCK_TICK.toString(), true);
				autoReading.put(RawSlaveProxy.Configuration.TRANSACTIONS.toString(), transactions);
				
				JSONArray autoReadings = new JSONArray();
				autoReadings.put(autoReading);
				
				JSONObject configuration = new JSONObject();
				configuration.put(RawSlaveProxy.Configuration.AUTO_READING.toString(), autoReadings);
				return configuration;
			}
			catch (JSONException e)
			{
				Logger.error(LC.gi(), null, "SDM630ProxyImpl.defaultConfiguration(): An error happenned when building the JSONObject containing default configuration.");
				return null;
			}
		}
		return null;
	}
	
}
