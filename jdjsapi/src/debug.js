function setStatusText() {
	$("#startstop").text("Polling " + (($.jd.isPollingContinuously()) ? "active" : "no"));
}
function jdinit() {
	$.jd.setOptions({
		debug : true,
		apiServer : $("#url").val(),
		sameDomain : ($("#samedomain").attr("checked") === "checked"),
		onmessage : function(data) {
			console.log(data);
			$("#log").append("Event: <br>").append(prettyPrint(data));
		},
		onerror : function(data) {
			console.log(data);
			$("#log").append("Error: <br>").append(prettyPrint(data));
		}
	});
}
$(function() {
	setStatusText();
	setInterval(setStatusText, 500);
	// jdinit();


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
		jdinit();		
	});
	$("#startstop").click(function() {
		jdinit();
		if (!$.jd.isPollingContinuously()) {
			$.jd.startPolling();
		} else {
			$.jd.stopPolling();
		}
		setStatusText();
	});
	console.log("Init complete.");
});