/**
 * @author mhils
 * @version 2011-07-15
 */
// documentation on writing tests here: http://docs.jquery.com/QUnit

if(!console)
{
	console = {
		log: function() {}
	};
}

var timeout = 3100;
$.jd.setOptions({
	apiTimeout : timeout,
	apiServer: "http://192.168.2.110:3128/",
	//debug: true,
	onerror: onErrorFail
}); // Reduce test time

function onErrorFail(e){
	ok(false, "Error occured: " + $.toJSON(e));
	start();
}
function ok2(a,b){
	ok(true,a+": "+$.toJSON(b));
}

//-------------------------------------------------------------
module("jQuery.jd - Init & Session Management");

test("Test options", 1, function() {
	$.jd.setOptions({
		'foo' : 'bar'
	});
	equal($.jd.getOptions().foo, "bar", "Settings are working.");
});

asyncTest("call not existing url, raising exception", 1, function() {
	$.jd.send('arrrr', 
		function(e) {
			ok(false,"onmessage called.");
			start();
		},
		function(e){
			ok(true,"onError called: "+$.toJSON(e));
			start();
		}		
	);
});

asyncTest("init with no credentials (anonymous) & disconnect",3, function() {
	$.jd.startSession(function(e) {
		equal(e.status, $.jd.e.sessionStatus.ANONYMOUS, "e.status");
		equal($.jd.getSessionStatus(),$.jd.e.sessionStatus.ANONYMOUS,"getSessionStatus");
		$.jd.stopSession(function(e) {
			equal($.jd.getSessionStatus(),$.jd.e.sessionStatus.NO_SESSION,"stopSession");
			start();
		});
	});
});

asyncTest("init with incorrect credentials", 3, function() {
	$.jd.setOptions({
		user : "user",
		pass : "wron"
	}).startSession(function(e) {
		equal(e.status,$.jd.e.sessionStatus.ERROR,"e.status");
		equal($.jd.getSessionStatus(),$.jd.e.sessionStatus.NO_SESSION,"getSessionStatus");
		ok((e.data && e.data.type === "unauthorized"), "Unauthorized: " + $.toJSON(e));
		
		$.jd.stopSession(function(e) {
			start();
		});
	});
});

asyncTest("init with correct credentials", 2, function() {
	$.jd.setOptions({
		user : "user",
		pass : "pass"
	}).startSession(function(e) {
		equal(e.status,$.jd.e.sessionStatus.REGISTERED,"e.status");
		equal($.jd.getSessionStatus(),$.jd.e.sessionStatus.REGISTERED,"getSessionStatus");
		start();
	});
});

//-------------------------------------------------------------
module("jQuery.jd - Sending");

asyncTest("send", function() {
	$.jd.setOptions({
		onmessage : function(e) {
			//must not trigger
			ok(false, "General Poll Callback triggered: " + $.toJSON(e));
			
		}
	});
	$.jd.send('ping', function(e) {
		equal(e, "pong", "Direct Callback Event: " + $.toJSON(e));
		start();
	}, onErrorFail);

});


asyncTest("send & pid send", 7, function() {
	$.jd.send('startCounter', function(e1,pid1) {
		ok(pid1,"PID1 returned: " + pid1);
		$.jd.send('startCounter', function(e2,pid2) {
			ok(pid2,"PID2 returned: " + pid2);
			$.jd.send('processes/list', function(processlist) {
				$.each([pid1,pid2],function(i,pid)
						{
							$.each(processlist,function(k,v)
							{
								console.log([pid,v]);
								if(v.pid === pid)
								{
									ok(true, "Pid is in list.");
									
								}
							});
						});
				setTimeout(function(){
					$.jd.send('processes/'+pid1+'/getValue', function(value) {
						ok(parseInt(value) !== NaN,"getValue: "+$.toJSON(value));
						
						$.jd.send('processes/'+pid1+'/stopCounter', function(e){
							ok2("stopCounter",e);
							$.jd.send('processes/list', function(processlist) {
								for(p in processlist)
									if(p.pid === pid1)
										{
										ok(false, "Pid is in list."); return;
										}
								ok(true,"Pid is not in the processlist anymore.");
								start();
						}, onErrorFail);
						$.jd.send('processes/'+pid2+'/stopCounter',undefined,undefined,onErrorFail);	
						}, onErrorFail);
					}, onErrorFail);
				},300);
			}, onErrorFail);
			
			
		}, onErrorFail);
		
	}, onErrorFail);

});
	
module("jQuery.jd - Polling");

asyncTest("PID Polling with pollOnce", 6, function() {
	
	var thisPid = -1;
	
	$.jd.setOptions({
		onmessage : function(e, pid) {
			if(pid === thisPid)
			{
				if(e <= 2)
				{
					ok2("onmessage",e);
					if(e === 2 )
					{
						$.jd.send('processes/'+thisPid+'/stopCounter', function(e){
							start();
						});
						
					}
					else if(e <= 2)
					{
						$.jd.pollOnce();
					}
				}
				return;
			}
			$.jd.pollOnce();
		}
	});
	
	$.jd.send('startCounter', function(e, pid) {
		ok2("Direct",e);
		thisPid = pid;
		$.jd.pollOnce();
	}, 
	onErrorFail, 
	function(e, pid) {
		if(pid === thisPid && e<=2)
		{
			ok2("onEvent",e);
			if(e===1)
			{
				$.jd.pollOnce();
				return false; //test return false
			}
				
			return true;
		}
	});
});

asyncTest("Polling continuously", 4, function() {
	
	var thisPid = -1;
	
	$.jd.setOptions({
		onmessage : function(e,pid) {
			if(pid === thisPid && e === 1)
			{
				ok2("onmessage",e);
				$.jd.stopPolling();
				$.jd.send('processes/'+thisPid+'/stopCounter', function(e){
					ok2("stopcounter",e);
					start();
				});
			}
		}
	});
	ok(!$.jd.isPollingContinuously(), "Not polling continuously");
	$.jd.startPolling();
	ok($.jd.isPollingContinuously(), "Polling continuously");
	$.jd.send('startCounter',function(e,pid){thisPid = pid;});

});

asyncTest("Emulate out of sync", 2, function() {
		
	$.jd.setOptions({
		onmessage : undefined,
		onerror: function(e){
			ok2("onerror",e);
			equal(e.status,"outofsync","is out of sync");
			start();
		}
	});
	$.jd.startPolling();
	$.jd._ajax.lastEventId = 0;
	$.jd.send('startCounter',function(e,pid){thisPid = pid;});

});