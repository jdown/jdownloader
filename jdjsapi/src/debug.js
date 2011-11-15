function dosettings() {
	
	function log(type, event){
		console.log(event);
		$("#log").append(type).append(prettyPrint(event));
	}
	
	$.jd.setOptions({
		debug : true,
		user: "user",
		pass: "pass",
		apiServer : $("#url").val(),
		sameDomain : ($("#samedomain").attr("checked") === "checked"),
		onmessage : function(data) {log("Event: <br>",data);},
		onerror : function(data) {log("Error: <br>",data);}
	});
}
function refresh() {
	if($.jd.getSessionStatus() === $.jd.e.sessionStatus.NO_SESSION)
	{
		$("#pollingtoggle").attr("disabled",true);
		$("#sessiontoggle").text("Start Session");
	}
	else
	{
		$("#pollingtoggle").attr("disabled",false);
		$("#sessiontoggle").text("Stop Session");
	}
	$("#pollingtoggle").text("Polling " + (($.jd.isPollingContinuously()) ? "active" : "no"));
}
$(function() {
	refresh();
	setInterval(refresh, 500);

	$("#sendButton").click(
			function() {
				var action = $("#send").val();
				var params = $("#params").val();
				params = ( params !== "") ? params.split("\n") : undefined;
				$.jd.send(action, params, function(e) {
					console.log(e);
					$("#pidlog").append(
							"<br>Direct response to " + action + " [" + params + "]: <br>")
							.append(prettyPrint(e));
				}, function(e) {
					console.log(e);
					$("#pidlog").append(
							"<br><b>Direct Error</b> " + action + " [" + params + "]: <br>")
							.append(prettyPrint(e));
				},
				function(e) {
					console.log(e);
					$("#pidlog").append(
							"<br>Event associated to to " + action + " [" + params + "]: <br>")
							.append(prettyPrint(e));
				});
			});

	$("#samedomain").click(function() {
		dosettings();
	});
	$("#pollingtoggle").click(function() {
		if (!$.jd.isPollingContinuously()) {
			$.jd.startPolling();
		} else {
			$.jd.stopPolling();
		}
		refresh();
	});
	
	$("#sessiontoggle").click(function() {
		dosettings();
		if($.jd.getSessionStatus() === $.jd.e.sessionStatus.NO_SESSION)
			$.jd.startSession();
		else
			$.jd.stopSession();
		refresh();
	});
	console.log("Init complete.");
});