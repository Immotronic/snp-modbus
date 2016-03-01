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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.Logger;
import org.ubikit.pem.event.ItemAddedEvent;
import org.ubikit.pem.event.ItemAddingFailedEvent;
import org.ubikit.tools.http.WebApiCommons;

import fr.immotronic.ubikit.pems.modbus.DeviceProfile;

public class APIServlet extends HttpServlet 
{
	private static final long serialVersionUID = -6372127057898997808L;

	private final PemLauncher pemLauncher;
	
	public APIServlet(PemLauncher pemLauncher)
	{
		this.pemLauncher = pemLauncher;
	}	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException 
	{
		String[] pathInfo = req.getPathInfo().split("/"); // Expected path info is /command/command_param_1/command_param_2/...etc. 	
		// Notes: if the request is valid (e.g. looks like /command/command_params), 
		//		pathInfo[0] contains an empty string,
		//		pathInfo[1] contains "command",
		//		pathInfo[2] and next each contains a command parameter. Command parameters are "/"-separated.
		if(pathInfo != null && pathInfo.length > 1)
		{
			
		}
		
		resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String[] pathInfo = req.getPathInfo().split("/"); // Expected path info is /command/command_param_1/command_param_2/...etc. 	
		// Notes: if the request is valid (e.g. looks like /command/command_params), 
		//		pathInfo[0] contains an empty string,
		//		pathInfo[1] contains "command",
		//		pathInfo[2] and next each contains a command parameter. Command parameters are "/"-separated.
		if(pathInfo != null && pathInfo.length > 1)
		{
			if(pathInfo[1].equals("add"))
			{
				try
				{
					DeviceProfile profile = DeviceProfile.valueOf(req.getParameter("deviceProfile"));
					JSONObject properties = new JSONObject(req.getParameter("properties"));
					
					AbstractPhysicalEnvironmentModelEvent addingResponseEvent = pemLauncher.createNewDevice(
							profile, 
							req.getParameter("unitID"), 
							req.getParameter("customName"), 
							req.getParameter("location"),
							properties,
							Integer.parseInt(req.getParameter("autoReadingFrequency"))
					);
					
					if (addingResponseEvent != null)
					{
						if(addingResponseEvent instanceof ItemAddedEvent)
						{
							JSONObject o = new JSONObject();
							
							try {
								o.put("deviceUID", addingResponseEvent.getSourceItemUID());
							}
							catch (JSONException e) {
								Logger.error(LC.gi(), this, "doPost/addDevice: While building the response object.");
								resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
								return;
							}
							
							resp.getWriter().write(WebApiCommons.okMessage(o));
							return;
						}
						else if(addingResponseEvent instanceof ItemAddingFailedEvent)
						{
							ItemAddingFailedEvent e = (ItemAddingFailedEvent)addingResponseEvent;
							resp.getWriter().write(WebApiCommons.errorMessage(e.getReason(), e.getErrorCode()));
							return;
						}
					}
					else
					{
						Logger.error(LC.gi(), this, "doPost/addDevice: While building the response object.");
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
						return;
					}
				}
				catch(JSONException e)
				{
					Logger.error(LC.gi(), this, "doPost/addDevice: Error when reading device specific properties.");
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
					return;
				}
				catch(NumberFormatException e)
				{
					Logger.error(LC.gi(), this, "doPost/addDevice: Automatic Reading Frequency is not an Integer.");
					resp.getWriter().write(WebApiCommons.errorMessage("FREQUENCY_IS_NOT_A_NUMBER", WebApiCommons.Errors.invalid_query));
					return;
				}
			}
		}
		
		resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
	}
}
