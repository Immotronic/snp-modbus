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

import java.io.IOException;
import java.util.Collection;

import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.ubikit.AbstractPhysicalEnvironmentItem;
import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.AbstractRootPhysicalEnvironmentModel;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.PhysicalEnvironmentModelObserver;
import org.ubikit.PhysicalEnvironmentItem.Property;
import org.ubikit.event.EventGate;
import org.ubikit.pem.event.ItemAddedEvent;
import org.ubikit.pem.event.ItemAddingFailedEvent;
import org.ubikit.pem.event.ItemDroppedEvent;
import org.ubikit.pem.event.ItemPropertiesUpdatedEvent;

//import fr.immotronic.license.LicenseManager;
//import fr.immotronic.license.impl.LicenseManagerImpl;
import fr.immotronic.ubikit.pems.modbus.DeviceProfile;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice;
import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.LinkType;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;
import fr.immotronic.ubikit.pems.modbus.item.SDM630ProxyImpl;

public final class PemLauncher extends AbstractRootPhysicalEnvironmentModel
{	
	//-------Number of required concurrent threads
	// TODO:	Here, customize the dimension in terms of number of threads the executor
	//			service that will support concurrent tasks in your PEM
	private static final int NUMBER_OF_REQUIRED_CONCURRENT_THREADS = 1; // For the Serial Bridge
	
	
	//-------SPECIFIC APP PRIVATE MEMBERS--------------------------
	
	private static EventGate higherEventGate;
	
	private final BundleContext bc;
	//private final LicenseManager licenseManager;
	
	private static DatabaseManagerImpl database;
	private APIServlet apiServlet;
	
	private boolean isSerialLineConnectionEnabled = false;
	
	//-------END OF SPECIFIC APP PRIVATE MEMBERS-------------------
	
	
	
	public PemLauncher(BundleContext bc)
	{
		super(NUMBER_OF_REQUIRED_CONCURRENT_THREADS, bc);
		this.bc = bc;
		
		/*LicenseManagerImpl lm = null;
		try 
		{
			lm = new LicenseManagerImpl(bc, System.getProperty("user.dir")+"/"+bc.getProperty("fr.immotronic.placetouch.downloadFolder"));
		} 
		catch (IOException e) 
		{
			Logger.info(LC.gi(), this, "##### LICENSE MANAGER CANNOT BE CREATED #######");
			try { bc.getBundle(0).stop(); } catch (BundleException be) { System.exit(0); }
		}
		finally
		{
			licenseManager = lm;
			if(licenseManager != null) 
			{
				licenseManager.checkLicenceFile();
			}
		}*/
	}

	@Override
	protected void start() throws Exception
	{
		String serialLineEnabled = bc.getProperty("fr.immotronic.modbus.isSerialLineConnectionEnabled");
		if (serialLineEnabled != null && serialLineEnabled.toLowerCase().equals("true")) {
			isSerialLineConnectionEnabled = true;
		}
		
		// Pem Initilization
		higherEventGate = getHigherAbstractionLevelEventGate();
		database = new DatabaseManagerImpl(getDatabaseConnection());
		BridgeFactory.init(bc, getExecutorService(), higherEventGate, isSerialLineConnectionEnabled);

		// Loading known devices from database
		loadKnownDevicesFromDatabase();

		// Loading servlet
		apiServlet = new APIServlet(this);
		registerServlet("", apiServlet);

		Logger.info(LC.gi(), this, "PEM modbus did started");
	}

	@Override
	protected void stop() 
	{
		AutomaticReadingManager.getInstance().terminate();
		BridgeFactory.terminate();
		
		database = null;
		apiServlet = null;
		
		Logger.info(LC.gi(), this, "PEM modbus did stopped");	
	}
	
	/**
	 * Add the given device in the pem items list and store it in database if storeInDatabase is true.
	 * @param device The device to add in items list.
	 * @param storeInDatabase If true, store device in database.
	 * @return true if the device could have been added to the pem items list.
	 */
	private boolean addDevice(ModbusDevice device, boolean storeInDatabase)
	{
		if(getAllItems().size() < /*licenseManager.getMaximalNumberOfItemsAllowed()*/10000)
		{
			super.addItem(device);
			if (storeInDatabase) {
				database.insertItem(device);
			}
			return true;
		}
		return false;
	}
	
	private void loadKnownDevicesFromDatabase()
	{
		Collection<JSONObject> items = database.getItems();
		if(items != null) 
		{
			for(JSONObject item : items) 
			{
				try 
				{
					String UID = item.getString(RawSlaveProxy.Field.UID.name());
					DeviceProfile profile = DeviceProfile.valueOf(item.getString(RawSlaveProxy.Field.PROFILE.name()));
					String address = item.getString(RawSlaveProxy.Field.ADDRESS.name());
					LinkType linkType = LinkType.valueOf(item.getString(RawSlaveProxy.Field.LINKTYPE.name()));
					JSONObject userProperties = item.getJSONObject(RawSlaveProxy.Field.PROPERTIES.name());
					JSONObject configuration = null;
					if (item.has(RawSlaveProxy.Field.CONFIGURATION.name()))
						configuration = item.getJSONObject(RawSlaveProxy.Field.CONFIGURATION.name());
					
					AbstractPhysicalEnvironmentItem device = null;
					switch (profile)
					{
						case RAW_SLAVE:
							device = new RawSlaveProxy(UID, address, linkType, userProperties, configuration);
							break;
						case SDM630:
							device = new SDM630ProxyImpl(UID, address, userProperties, configuration);
							break;
					}
					
					if (device != null) {
						addDevice((ModbusDevice) device, false);
					}
				} 
				catch (Exception e) 
				{
					Logger.error(LC.gi(), this, "loadKnownDevicesFromDatabase(): While loading devices from database.", e);
				}
			}
		}
	}
	
	@Override
	public void addItem(PhysicalEnvironmentItem item) 
	{
		if (item != null && item instanceof ModbusDevice)
		{		
			// We add name & location properties the item if the higher layer forget to add them.
			JSONObject userProperties = item.getPropertiesAsJSONObject();
			if (!userProperties.has(PhysicalEnvironmentItem.Property.CustomName.name())) {
				item.setPropertyValue(PhysicalEnvironmentItem.Property.CustomName.name(), "A Modbus Device");
			}
			if (!userProperties.has(PhysicalEnvironmentItem.Property.Location.name())) {
				item.setPropertyValue(PhysicalEnvironmentItem.Property.Location.name(), "");
			}
			
			// Adding device.
			boolean deviceAdded = addDevice((ModbusDevice) item, true);
			
			if (deviceAdded)
			{
				// Posting ItemAddedEvent.
				Logger.debug(LC.gi(), this, "Posting ItemAddedEvent to Higher Abstraction Level");
				higherEventGate.postEvent(new ItemAddedEvent(item.getUID(), getUID(), PhysicalEnvironmentItem.Type.OTHER, item.getPropertiesAsJSONObject(), item.getCapabilities(), item.getConfigurationAsJSON()));
			}
		}
	}
	
	private static boolean isInteger(String s)
	{
		try { Integer.parseInt(s); } catch(NumberFormatException e) { return false; }
	    return true;
	}
	
	protected AbstractPhysicalEnvironmentModelEvent createNewDevice(DeviceProfile profile, String address, String customName, String location, JSONObject specificProperties, int automaticReadingFrequencyInSeconds)
	{
		assert profile != null : "Device Profile MUST NOT be null";
		assert address != null : "Device Address MUST NOT be null";
		assert customName != null : "Device Name MUST NOT be null";
		assert location != null : "Device Location MUST NOT be null";
		
		AbstractPhysicalEnvironmentModelEvent event = null;
		AbstractPhysicalEnvironmentItem device = null;
		
		switch(profile)
		{
			case SDM630:
				if (automaticReadingFrequencyInSeconds < 0) {
					event = new ItemAddingFailedEvent(customName, "INVALID_FREQUENCY", 2);
				}
				else if (address.isEmpty() || !isInteger(address)) {
					event = new ItemAddingFailedEvent(customName, "INVALID_UNIT_ID", 2);
				}
				else {
					device = new SDM630ProxyImpl(address, specificProperties, automaticReadingFrequencyInSeconds);
				}
				
				if(event != null) {
					higherEventGate.postEvent(event);
					return event;
				}
				break;
		}
			
		if (device != null)
		{
			// Setting the device properties with the given user defined custom name & location
			device.setPropertyValue(Property.CustomName.name(), customName);
			device.setPropertyValue(Property.Location.name(), location);
			
			// Adding device.
			boolean deviceAdded = addDevice((ModbusDevice) device, true);
			
			if (deviceAdded) {
				// Create a ItemAddedEvent.
				event = new ItemAddedEvent(device.getUID(), getUID(), PhysicalEnvironmentItem.Type.OTHER, device.getPropertiesAsJSONObject(), device.getCapabilities(), device.getConfigurationAsJSON());
			}
			else {
				event = new ItemAddingFailedEvent(customName, "MAX_NUMBER_OF_DEVICES_REACHED", 2);
			}
		}
		else {
			event = new ItemAddingFailedEvent(customName, "UNKNOWN__PROFILE", 2);
		}
		
		Logger.debug(LC.gi(), this, "createNewDevice(): Posting "+event.getClass()+" to Higher Abstraction Level");
		higherEventGate.postEvent(event);
		return event;
	}
	
	public static void updateDeviceProperties(ModbusDevice device, String[] propertiesName)
	{
		if(device != null)
		{
			database.updateItem(device);
			
			if (propertiesName != null) {
				higherEventGate.postEvent(new ItemPropertiesUpdatedEvent(device.getUID(), propertiesName));
			}
		}
	}
	
	@Override
	public PhysicalEnvironmentItem removeItem(String itemUID) 
	{
		Logger.debug(LC.gi(), this, "Posting ItemDroppedEvent to Higher Abstraction Level");
		higherEventGate.postEvent(new ItemDroppedEvent(itemUID, getUID()));
		database.removeItem(itemUID);
		return super.removeItem(itemUID);
	}
	
	public static EventGate getHigherEventGate()
	{
		return higherEventGate;
	}
	
	@Override
	public PhysicalEnvironmentModelInformations getInformations()
	{
		// ----- 
		// OPTIONAL
		//    TODO: Return a adequate object that implements the 
		//			PhysicalEnvironmentModelInformations interface.
		//			Such object SHOULD carry introspection data about
		//			your PEM state. It is a feature that you can give
		//			to PEM clients to help them know what is going on
		//			in your PEM, or get statistical data, or whatever.
		// -----
		
		return null;
	}
	
	@Override
	public void setObserver(PhysicalEnvironmentModelObserver observer)
	{
		// ----- 
		// OPTIONAL
		//    TODO: If you wish to make your PEM able to emit some log
		//			data to PEM clients, you can use the observer object for
		//			that. Keep its reference wherever you want.
		// -----
	}
	
	@Override
	public HardwareLinkStatus getHardwareLinkStatus()
	{
		// ----- 
		//
		// TODO: 	Implement here the code that return
		//			the current status of the link between
		//			this PEM and its hardware counterpart.
		//
		// -----
		if (!isSerialLineConnectionEnabled || (SerialBridge.getInstance() != null && SerialBridge.getInstance().isConnected()))
		{
			return HardwareLinkStatus.CONNECTED;
		}
		
		return HardwareLinkStatus.DISCONNECTED;
	}
}