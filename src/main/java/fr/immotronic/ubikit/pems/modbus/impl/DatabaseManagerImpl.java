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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.DatabaseProxy;
import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;

public final class DatabaseManagerImpl
{
	private static final String TABLE_STRUCTURE = "CREATE TABLE IF NOT EXISTS items (" +
			"UID VARCHAR(50) NOT NULL, " +
			"profile VARCHAR(50) NOT NULL, " +
			"address VARCHAR(100) NOT NULL, " +
			"linkType VARCHAR(30) NOT NULL, " +
			"properties VARCHAR(4096) NOT NULL, " +
			"configuration VARCHAR(2056), " +
			"PRIMARY KEY (UID));";
	
	private static final String QUERY_ITEMS = "SELECT * FROM items;";
	
	private static final String INSERT_QUERY = "INSERT INTO items " +
			"(UID, profile, address, linkType, properties, configuration) " +
			"VALUES (?, ?, ?, ?, ?, ?);";
	
	private static final String UPDATE_QUERY = "UPDATE items SET profile = ?, address = ?, linkType = ?, properties = ?, configuration = ? WHERE UID = ?;";
	
	private static final String DELETE_QUERY = "DELETE FROM items WHERE UID = ?;";
	
	private final DatabaseProxy dbProxy;
	private final PreparedStatement getItems;
	private final PreparedStatement insertItem;
	private final PreparedStatement updateItem;
	private final PreparedStatement removeItem;
	
	public DatabaseManagerImpl(DatabaseProxy dbProxy)
	{
		this.dbProxy = dbProxy;
		
		dbProxy.executeUpdate(TABLE_STRUCTURE);
		
		getItems = dbProxy.getPreparedStatement(QUERY_ITEMS);
		insertItem = dbProxy.getPreparedStatement(INSERT_QUERY);
		updateItem = dbProxy.getPreparedStatement(UPDATE_QUERY);
		removeItem = dbProxy.getPreparedStatement(DELETE_QUERY);
	}
	
	public Collection<JSONObject> getItems()
	{
		Collection<JSONObject> items = new ArrayList<JSONObject>();
		ResultSet rs = dbProxy.executePreparedQuery(getItems);
		if(rs != null)
		{
			try 
			{
				while(rs.next())
				{
					JSONObject o = new JSONObject();
					o.put(RawSlaveProxy.Field.UID.name(), rs.getString(1));
					o.put(RawSlaveProxy.Field.PROFILE.name(), rs.getString(2));
					o.put(RawSlaveProxy.Field.ADDRESS.name(), rs.getString(3));
					o.put(RawSlaveProxy.Field.LINKTYPE.name(), rs.getString(4));
					o.put(RawSlaveProxy.Field.PROPERTIES.name(), new JSONObject(rs.getString(5)));
					String config = rs.getString(6);
					if (config != null) {
						o.put(RawSlaveProxy.Field.CONFIGURATION.name(), new JSONObject(config));
					}
					items.add(o);
				}
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "getItems(): Cannot get the Modbus items list");
				return null;
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "getItems(): Cannot construct Modbus items JSON object");
				return null;
			}
		}
		
		return items;
	}
	
	public boolean insertItem(ModbusDevice item)
	{
		if(item != null)
		{
			try 
			{
				insertItem.setString(1, item.getUID());
				insertItem.setString(2, item.getProfile().name());
				insertItem.setString(3, item.getAddress());
				insertItem.setString(4, item.getLinkType().name());
				insertItem.setString(5, item.getPropertiesAsJSONObject().toString());
				if (item.getConfiguration() != null) {
					insertItem.setString(6, item.getConfiguration().toString());
				}
				else {
					insertItem.setNull(6, java.sql.Types.VARCHAR);
				}
				return (dbProxy.executePreparedUpdate(insertItem) >= 0);
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "insertItem(): Cannot insert the new Modbus item.", e);
				return false;
			}
		}
		
		return true;
	}
	
	public boolean updateItem(ModbusDevice item)
	{
		if(item != null)
		{
			try 
			{
				updateItem.setString(1, item.getProfile().name());
				updateItem.setString(2, item.getAddress());
				updateItem.setString(3, item.getLinkType().name());
				updateItem.setString(4, item.getPropertiesAsJSONObject().toString());
				if (item.getConfiguration() != null) {
					updateItem.setString(5, item.getConfiguration().toString());
				}
				else {
					updateItem.setNull(5, java.sql.Types.VARCHAR);
				}
				updateItem.setString(6, item.getUID());
				return (dbProxy.executePreparedUpdate(updateItem) >= 0);
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "updateItem(): Cannot update the specified Modbus item.", e);
				return false;
			}
		}
		
		return true;
	}
	
	public boolean removeItem(String itemUID)
	{
		if(itemUID != null)
		{
			try 
			{
				removeItem.setString(1, itemUID);
				return (dbProxy.executePreparedUpdate(removeItem) >= 0);
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "removeItem(): Cannot remove the specified Modbus item.", e);
				return false;
			}
		}
		
		return true;
	}
	
}
