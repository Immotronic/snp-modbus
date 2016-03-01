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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.modbus.ModbusRequest;
import fr.immotronic.ubikit.pems.modbus.item.RawSlaveProxy;

public final class AutomaticReadingManager
{
	private static int THREAD_POOL_SIZE = 5;
	private static volatile AutomaticReadingManager INSTANCE = new AutomaticReadingManager();
	
	private final ScheduledExecutorService executor;
	private final Map<RawSlaveProxy, Collection<ScheduledFuture<?>>> tasks;
	
	private AutomaticReadingManager()
	{
		executor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
		tasks = Collections.synchronizedMap(new HashMap<RawSlaveProxy, Collection<ScheduledFuture<?>>>());
	}
	
	public static AutomaticReadingManager getInstance()
	{
		return INSTANCE;
	}
	
	private class AutomaticReader implements Runnable
	{
		private final Collection<ModbusRequest> requests;
		
		public AutomaticReader(Collection<ModbusRequest> requests)
		{
			this.requests = requests;
		}
		
		@Override
		public void run()
		{
			if (requests != null && !requests.isEmpty())
			{
				ModbusBridge bridge = ((ModbusRequestImpl) requests.iterator().next()).getBridge();
				if (bridge != null) {
					bridge.offerDataRequests(requests);
				}
			}
		}
	}
	
	/**
	 * Schedule one or more requests to be executed every <i>interval</i> seconds.
	 * @param slaveProxy The proxy that schedule these requests.
	 * @param requests The requests to schedule.
	 * @param initialDelay The delay (in seconds) of the first schedule.
	 * @param interval The interval (in seconds) at which requests will be executed.
	 */
	public void scheduleAutomaticReading(RawSlaveProxy slaveProxy, Collection<ModbusRequest> requests, long initialDelay, long interval)
	{
		synchronized(tasks)
		{
			Logger.debug(LC.gi(), this, "Data for device "+slaveProxy.getUID()+" will be automatically read every "+interval+" seconds.");
			if (!tasks.containsKey(slaveProxy)) {
				Collection<ScheduledFuture<?>> proxyTasks = new ArrayList<ScheduledFuture<?>>();
				tasks.put(slaveProxy, proxyTasks);
			}
			
			AutomaticReader reader = new AutomaticReader(requests);
			ScheduledFuture<?> task = executor.scheduleAtFixedRate(reader, initialDelay, interval, TimeUnit.SECONDS);
			tasks.get(slaveProxy).add(task);
		}
	}
	
	/**
	 * Stop all the requests that have been provided by the given proxy.
	 * @param slaveProxy The proxy for which requests will be removed.
	 */
	public void stopAutomaticReading(RawSlaveProxy slaveProxy)
	{
		synchronized(tasks)
		{
			Collection<ScheduledFuture<?>> proxyTasks = tasks.get(slaveProxy);
			if (proxyTasks != null)
			{
				for (ScheduledFuture<?> task: proxyTasks)
				{
					// The task is cancelled. Setting parameter to false allow in-progress work to complete. 
					task.cancel(false);
				}
			}
		}
	}
	
	protected void terminate()
	{
		synchronized(tasks)
		{
			executor.shutdown();
			for (Collection<ScheduledFuture<?>> proxyTasks: tasks.values()) {
				proxyTasks.clear();
			}
			tasks.clear();
		}
	}
	
}
