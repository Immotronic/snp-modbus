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

import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.ubikit.Logger;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.modbus.item.ModbusDevice.LinkType;

public final class BridgeFactory
{
	private static ScheduledExecutorService executor = null;
	private static BundleContext bc = null;
	
	protected static void init(BundleContext bc, ScheduledExecutorService executor, EventGate higherLevelEventGate, boolean isSerialLineConnectionEnabled)
	{
		if (BridgeFactory.bc == null) {
			BridgeFactory.bc = bc;
		}
		if (BridgeFactory.executor == null) {
			BridgeFactory.executor = executor;
		}
		
		SerialBridge.init(executor, bc, higherLevelEventGate, isSerialLineConnectionEnabled);
	}
	
	protected static void terminate()
	{
		BridgeFactory.executor = null;
		BridgeFactory.bc = null;
	}
	
	public static ModbusBridge createBridge(String address, LinkType connectionType)
	{
		assert executor != null : "ScheduledExecutorService MUST have been set before calling createBridge()";
		assert bc != null : "BundleContext MUST have been set before calling createBridge()";
		
		if (connectionType != null)
		{
			switch(connectionType)
			{
				case Serial :
					return SerialBridge.getInstance();
				case TCP:
				case UDP:
					// Get IP (or URL) and port from given modbus address.
					String addr = null;
					int port = -1;
					try
					{
						String[] splits = new StringBuilder(address).reverse().toString().split(":", 2);
						StringBuilder sb = new StringBuilder();
						for (int i = 1; i < splits.length; i++)
							sb.append(splits[i]);
						addr = sb.reverse().toString();
						port = Integer.parseInt(new StringBuilder(splits[0]).reverse().toString());
					}
					catch (Exception e) {
						Logger.debug(LC.gi(), null, "createBridge(): Cannot get IP (or URL) and port from given modbus address.");
						return null;
					}
					
					// Creating Bridge
					Logger.debug(LC.gi(), null, "Creating a new "+connectionType+"Bridge with parameters: address="+addr+", port="+port);
					if (connectionType == LinkType.TCP)
						return new TCPBridge(addr, port, executor);
					else
						return new UDPBridge(addr, port, executor);
			}
		}
		
		Logger.error(LC.gi(), null, "createBridge(): Instanciation of Bridge failed.");
		return null;
	}
}