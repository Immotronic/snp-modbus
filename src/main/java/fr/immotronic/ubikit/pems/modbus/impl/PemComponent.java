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
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.PhysicalEnvironmentModelObserver;
import org.ubikit.event.EventGate;
import org.ubikit.service.PemRegistryService;
import org.ubikit.service.RootPhysicalEnvironmentModelService;
import org.ubikit.service.HSQLDatabaseService;


@Component
@Instantiate
@Provides
public final class PemComponent implements RootPhysicalEnvironmentModelService
{	
	// ------------------------------------------------------------------------
	// IN MOST CASE, THERE IS NOTHING TO CHANGE BELOW THIS LINE
	// Except if you need to require more services from third-party components,
	// and need to link them to your code.
	// Then, you might want to update validate() and/or invalidate() methods.
	// ------------------------------------------------------------------------
	
	@ServiceProperty
	private String uid;
	
	@Requires
	private HttpService httpService = null;
	
	@Requires
	private HSQLDatabaseService hsqldbConnectionFactory = null;
	
	@Requires
	private PemRegistryService pemRegistryService = null;
	
	private PemLauncher pemLauncher;
	
	public PemComponent(BundleContext bc)
	{
		pemLauncher = new PemLauncher(bc);
		uid = pemLauncher.getUID();
	}
	
	@Validate
    public synchronized void validate()
    {
		pemLauncher.validate(httpService, pemRegistryService, hsqldbConnectionFactory);
    }
	
	@Invalidate
    public synchronized void invalidate()
    {
		pemLauncher.invalidate();
    }
    
    @Override
	public String getUID() 
	{
		return pemLauncher.getUID();
	}

	@Override
	public void clearItems() 
	{
		pemLauncher.clearItems();
	}

	@Override
	public Collection<PhysicalEnvironmentItem> getAllItems() 
	{
		return pemLauncher.getAllItems();
	}

	@Override
	public PhysicalEnvironmentItem getItem(String itemUID) 
	{
		return pemLauncher.getItem(itemUID);
	}
	
	@Override
	public void addItem(PhysicalEnvironmentItem item) 
	{
		pemLauncher.addItem(item);	
	}

	@Override
	public PhysicalEnvironmentItem removeItem(String itemUID) 
	{
		return pemLauncher.removeItem(itemUID);
	}

	@Override
	public void linkTo(EventGate eventGate) 
	{
		pemLauncher.linkTo(eventGate);
	}

	@Override
	public void unlink(EventGate eventGate) 
	{
		pemLauncher.unlink(eventGate);
	}
	
	@Override
	public PhysicalEnvironmentModelInformations getInformations()
	{
		return pemLauncher.getInformations();
	}
	
	@Override
	public void setObserver(PhysicalEnvironmentModelObserver observer)
	{
		pemLauncher.setObserver(observer);
	}
	
	@Override
	public String getBaseURL() 
	{
		return pemLauncher.getBaseURL();
	}
	
	@Override
	public HardwareLinkStatus getHardwareLinkStatus()
	{
		return pemLauncher.getHardwareLinkStatus();
	}
}
