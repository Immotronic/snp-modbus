var ModbusControlUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.modbus",
	baseURL: "/Immotronic/modbus/",
	
	init: function()
	{
		Placetouch.registerPemControlUIHandler(ModbusControlUIHandler.UID, ModbusControlUIHandler);
		ModbusControlUIHandler.__loadCapabilityUIs();
	},
		
	addControlUI: function(itemUID, capability, configuration, sel)
	{
		var ui = ModbusControlUIHandler.__capabilityUIs[capability]; 
		if(ui != undefined)
		{
			$(sel).append(ui);
			ModbusControlUIHandler.__initUI(itemUID, capability, configuration);
		}
	},
	
	doesControlUIExistFor: function(capability)
	{
		return ModbusControlUIHandler.__capabilityUIs[capability] != undefined;
	},
	
	__initUI: function(itemUID, capability, configuration)
	{
		switch(capability)
		{
			case "SDM630":
				// Here we can add a button to update device data.
				break;
		}
	},
	
	__loadCapabilityUIs: function()
	{
		// ModbusControlUIHandler.__loadCapabilityUI("SDM630");
	},
	
	__loadCapabilityUI: function(capability)
	{
		$.ajax({	
			type: "GET", 
			url:ModbusControlUIHandler.baseURL + "ui/control/placetouch/"+capability+".html",
			success: function(data) {
				ModbusControlUIHandler.__capabilityUIs[capability] = data;
			},
			error: function() { 
				cwf.log("No control UI for "+capability+" capability in PEM "+ModbusControlUIHandler.UID);
			}
		});
	},
	
	__capabilityUIs: {}
}

ModbusControlUIHandler.init();