var ModbusConfigurationUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.modbus",
	baseURL: "/Immotronic/modbus/",
	
	init: function()
	{
		Placetouch.registerPemConfigurationUIHandler(ModbusConfigurationUIHandler.UID, ModbusConfigurationUIHandler);
		ModbusConfigurationUIHandler.__loadConfigurationUI();
	},
	
	addConfigurationUI: function(sel)
	{ 
		if(ModbusConfigurationUIHandler.__UI != undefined)
		{
			$(sel).append(ModbusConfigurationUIHandler.__UI);
			ModbusConfigurationUIHandler.__initUI(sel);
		}
	},
	
	__initUI: function(sel)
	{	
		cwf.l10n.tr_element(sel);
	},
	
	__loadConfigurationUI: function()
	{
		$.ajax({	
			type: "GET", 
			url:ModbusConfigurationUIHandler.baseURL + "ui/configuration/"+ModbusConfigurationUIHandler.__sysappName+"/configuration.html",
			success: function(data) {
				ModbusConfigurationUIHandler.__UI = data;
			},
			error: function() { 
				cwf.log("No configuration UI in PEM "+ModbusConfigurationUIHandler.UID);
			}
		});
	},
	
	__UI: {},
	
	__sysappName : "placetouch"
}

ModbusConfigurationUIHandler.init();