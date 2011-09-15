/**
 * CaptchaPush for JDownloader
 * 
 * @author mhils
 */

isPhoneGap = false; // Perform several adjustments for mobile platforms

document.addEventListener("deviceready", onPhoneGap, false);

function onPhoneGap() {
	isPhoneGap = true;
	$("#sound").attr("checked", "checked").parents("tr").hide();
}

$(function CaptchaPush() {

	initComplete = false;
	/*
	 * Needed for the history plugin. The callback function will get triggered
	 * 1-2 times (depending on browser) on init, we don't change anything in
	 * these cases.
	 */

	var CaptchaHandler = {
		_queue : [],
		addCaptcha : function addCaptcha(captcha) {
			if (captcha.type !== "NORMAL") {
				alert("Warning: Captcha type " + captcha.type + " is not supported.");
			}
			CaptchaHandler._queue.push(captcha);
			if (CaptchaHandler._queue.length == 1) {
				CaptchaHandler.showNext();
			}
		},
		showNext : function showNext() {
			$("#captcha img").attr("src", $.jd.getURL("captcha/get", CaptchaHandler._queue[0].id));
			notifyUser();
			setStatus("Captcha for "+CaptchaHandler._queue[0].hoster)
			$("#captcha #display").slideDown();
			$("#captchaInput").val("").focus();
		},
		solve : function solve() {
			$.jd.send("captcha/solve", [ CaptchaHandler._queue.shift().id, $("#captchaInput").val() ]);
			$("#captcha #display").slideUp();
			setStatus("Waiting for Captchas!");
			if (CaptchaHandler._queue.length > 0)
				CaptchaHandler.showNext();
			return false;
		}
	};

	function setStatus(status) {
		console.log(status);
		if (status == "")
			status = "&nbsp;";
		$("#status").html(status);
	}

	function onConnect(resp) {
		setStatus("Connection etablished.");
		$("#auth .do").attr("disabled", null);
		
		if ($.webStorage.local().getItem("autologin") === true) {
			$("#auth .do").click();
		}	
	}
	function onConnectError(err) {
		if(err.status === "timeout")
			setStatus("Connection timed out. Server not running?");
		else
			setStatus("Error on connecting: "+err.errorThrown);
		console.log(err);
		window.history.back();
	}

	function onAuth(resp) {
		console.log(resp);

		if (resp.status == $.jd.e.sessionStatus.REGISTERED) {
			// Save config in localStorage
			setStatus("Waiting for Captchas!");
			if ($("#remember").is(':checked')) {
				$.each(settingIds, function(i, a) {
					o = $("#" + a);
					if (o.is('[type="checkbox"]')) {
						var isChecked = (o.attr("checked") === "checked");
						$.webStorage.local().setItem(a, isChecked);
					} else
						$.webStorage.local().setItem(a, o.val());
				});
			} else {
				$.each(settingIds, function(i, a) {
					$.webStorage.local().removeItem(a);
				});
			}
			
			$.jd.setOptions({
				//debug: true,
				//onmessage : log
			});
			getCaptchas();
			$.jd.subscribe("captcha", onCaptcha);
			$.jd.startPolling();
		} else {
			window.history.back();
			setStatus("Authentication failed!");
		}
	}
	
	function getCaptchas() {
		$.jd.send("captcha/list", function onCaptchaList(captchas) {
			$.each(captchas, function(i, captcha) {
				CaptchaHandler.addCaptcha(captcha);
			});
		});
	}

	function notifyUser() {
		if (isPhoneGap) {
			navigator.notification.vibrate(1000);
			navigator.notification.beep(1);
		} else {
			if ($("#sound").is(":checked"))
				captchaSound.play();
		}
	}

	function onCaptcha(event) {
		//console.log(event);
		if (event.message === "new") {
			CaptchaHandler.addCaptcha(event.data);
		}
	}

	$("#connect .do").click(function() {

		setStatus("Connecting...");
		$.jd.setOptions({
			apiServer : $("#apiurl").val(),
			debug: true//,
			//apiTimeout : 1000
		}).send('jd/getVersion', onConnect, onConnectError);
		$("#auth .do").attr("disabled", "disabled");
		return false;
	});

	$("#auth .do").click(function() {
		setStatus("Authenticating...");
		$.jd.setOptions({
			user : $("#user").val(),
			pass : $("#pass").val()
		}).startSession(onAuth);
		return false;
	});

	$("#captcha .solve").click(CaptchaHandler.solve);

	$("#remember").change(function(e) {
		var isChecked = $("#remember").is(":checked");
		$("#autologin").parent().parent().toggle(isChecked);
		if (!isChecked)
			$("#autologin").removeAttr("checked");
	});

	$(".back").click(function() {
		$.jd.stopSession();
		$("#autologin").removeAttr("checked");
		$.webStorage.local().removeItem("autologin");
		window.history.back();
		setStatus("");
		return false;
	});

	$(".do").click(function() {
		// Regular <a href> links would work great, but we extend browser
		// support this way (see history plugin limitations)
		$.history.load($(this).data("href"));
	});

	function hashChanged(hash) {
		if (!initComplete)
			return;
		if (hash === "")
			hash = "connect";
		console.log("hashChanged: " + hash);
		var off = $(".active");
		var on = $("#" + hash);
		if (!on.is(off)) {
			on.addClass("active");
			// We have to do this right now because
			// execution in the callback may lead to
			// sync errors
			off.removeClass("active").stop(true, true).slideToggle(function() {
				on.slideToggle();
				off.children(".do").blur();
				on.children(".do").focus();
			});
		}
	}

	window.location.hash = "";
	$.history.init(hashChanged);

	var settingIds = [ "user", "pass", "remember", "autologin", "apiurl", "sound" ];

	// Restore settings from localStore
	$.each(settingIds, function(i, a) {
		if($.webStorage.local().getItem(a) === null)
			return;
		var o = $("#" + a);
		if (o.is('[type="checkbox"]'))
		{
			if($.webStorage.local().getItem(a))
			o.attr('checked',$.webStorage.local().getItem(a)).trigger('change');
		}
		else if ($.webStorage.local().getItem(a))
			o.val($.webStorage.local().getItem(a));
	});

	/*
	 * Chrome 14 does not handle a quick change of the location.hash correctly,
	 * which results in invalid back buttons. Workaround replaces back button
	 * functionality and removes autologin key in the localStorage
	 */
	if ($.browser.webkit === true) {
		$(".back").unbind("click").click(function() {
			$.webStorage.local().removeItem("autologin");
			location.reload();
		});
	}

	captchaSound = new buzz.sound("./sounds/captcha", {
		formats : [ "ogg", "mp3" ],
		preload : true,
		autoplay : false,
		loop : false
	});

	var isMobile = navigator.userAgent.match(/iPad|iPhone|android|symian|maemo|meego|webos|phone|mobile/i);
	if (isMobile) {
		// Increase performance by diabling jquery effects
		$.fx.off = true;
	}

	setStatus("");
	initComplete = true;

	if ($.webStorage.local().getItem("autologin"))
		$("#connect .do").submit();
});