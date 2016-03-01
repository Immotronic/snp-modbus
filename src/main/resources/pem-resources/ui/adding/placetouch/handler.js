var ModbusAddingUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.modbus",
	baseURL: "/Immotronic/modbus/",
	
	init: function()
	{
		Placetouch.registerPemAddingUIHandler(ModbusAddingUIHandler.UID, ModbusAddingUIHandler);
		ModbusAddingUIHandler.__loadAddingUI();
	},
	
	addAddingUI: function(sel)
	{ 
		if(ModbusAddingUIHandler.__UI != undefined)
		{
			$(sel).append(ModbusAddingUIHandler.__UI);
			ModbusAddingUIHandler.__initUI(sel);
		}
	},
	
	__initUI: function(sel)
	{	
		cwf.l10n.tr_element(sel);
		ModbusAddingUIHandler.__loadDeviceUI(sel);
		
		$("#ModbusAddNode").click(function() {
			var properties = { };
			$(sel+" .itemSpecificProperty").each(function() {
				var elem = $($($(this).find("td.value")[0]).children(":first"));
				properties[elem.attr("id")] = elem.val();
			});
			
			cwf.api.query(ModbusAddingUIHandler.baseURL+"api/add", {
				deviceProfile: $(sel+" #deviceProfile").val(),
				unitID: $(sel+" #itemUnitID").val(),
				customName: $(sel+" #itemName").val(),
				location: $(sel+" #itemLocation").val(),
				autoReadingFrequency: $(sel+" #itemFrequency").val(),
				properties: JSON.stringify(properties)
			},
			{
				success: function(data)
				{
					$(" #addItemFeedbackFailed").hide();
					$(sel+" #itemName").val("");
					$(sel+" #itemUnitID").val("");
				},
				failed: function(reason, errorCode) {
					$(" #addItemFeedbackFailed #reason").html(reason);
					$(" #addItemFeedbackFailed").show();
		    	}
			},
			{
				//to_hide: ["#EnoceanAddNode"],
				to_show: [sel+" IMG"]
			});
		});
		
		$(sel+" #deviceProfile").change(function() {
			ModbusAddingUIHandler.__loadDeviceUI(sel);
		});
	},
	
	__loadAddingUI: function()
	{
		$.ajax({	
			type: "GET", 
			url:ModbusAddingUIHandler.baseURL + "ui/adding/"+ModbusAddingUIHandler.__sysappName+"/adding.html",
			success: function(data) {
				ModbusAddingUIHandler.__UI = data;
			},
			error: function() { 
				cwf.log("No adding UI in PEM "+ModbusAddingUIHandler.UID);
			}
		});
	},
	
	__loadDeviceUI: function(sel)
	{
		var profile = $(sel+" #deviceProfile").val();
		$.ajax({	
			type: "GET", 
			url:ModbusAddingUIHandler.baseURL + "ui/adding/"+ModbusAddingUIHandler.__sysappName+"/"+profile+".html",
			success: function(data) {
				$(sel+" .itemSpecificProperty").remove();
				$(sel+" #addingModbusDevice tr:last").before(data);
				cwf.l10n.tr_element(sel+" .itemSpecificProperty");
			},
			error: function() { 
				$(sel+" .itemSpecificProperty").remove();
				cwf.log("No additional properties for profile "+profile+" in PEM "+EnoceanControlUIHandler.UID);
			}
		});
	},
	
	__UI: {},
	
	__sysappName : "placetouch"
}

ModbusAddingUIHandler.init();