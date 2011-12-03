function dosettings() {
    
    function log(type, event){
        console.log(event);
        $("#log").prepend(type,prettyPrint(event));
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

    function pidlog(type,event){
       console.log(event);
       $("#pidlog").prepend("<br>"+type+"<br>",prettyPrint(event));
    }
    
    $("#sendButton").click(
            function() {
                var action = $("#send").val();
                var params = $("#params").val();
                params = ( params !== "") ? params.split("\n") : undefined;
                $.jd.send(action, params, function(e) { 
                    pidlog("Direct response to " + action + " [" + params + "]:",e);
                }, function(e) {
                    pidlog("<b>Direct Error</b> " + action + " [" + params + "]:",e);
                },
                function(e) {
                    pidlog("Event associated to to " + action + " [" + params + "]:",e);
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