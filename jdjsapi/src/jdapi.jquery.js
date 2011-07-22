/**
 * @preserve JDownloader API Bindings for jQuery (jQuery.jd)
 * @version 07/2011
 * @author mhils
 * 
 * Callback functions accept the parameters data and pid (optional).
 * 
 */

(function($) {
	$.jd = {
		/**
		 * Set options
		 * 
		 * @param options to set
		 */
		setOptions : function(options) {
			if (options) {
				$.extend($.jd._settings, options);
			}
			if (!$.jd._settings.apiServer.match(/\/$/))
				$.jd._settings.apiServer = $.jd._settings.apiServer + "/";
			return this;
		},
		/**
		 * Get a clone of the current options
		 * 
		 * @return {Object.<string, *>}
		 */
		getOptions : function() {
			return $.extend(true, {}, $.jd._settings);
		},
		/**
		 * Start a session. Please note that this function is async and will
		 * return immediately. Use the callback function for functions requiring
		 * a session. For anonymous sessions, omit login parameters. If user and
		 * pass are set as options, you can omit these parameters, too.
		 * 
		 * @param {string=} user (optional)
		 * @param {string=} pass (optional)
		 * @param {function(Object.<string, *>,?string=)=} callback
		 * @suppress {checkTypes}
		 */
		startSession : function(user, pass, callback) {
			if ($.jd._ajax.sessionStatus !== $.jd.e.sessionStatus.NO_SESSION) {
				$.jd.stopSession(function() {
					$.jd.startSession(user, pass, callback);
				}); // disconnect first then
				return;
			}
			// Shift parameters if user and pass are ommited
			if ($.isFunction(user)) {
				callback = user;
				user = pass = undefined;
			}
			$.jd._settings.user = (user) ? user : $.jd._settings.user;
			$.jd._settings.pass = (pass) ? pass : $.jd._settings.pass;

			$.jd.send("session/handshake", ($.jd._settings.user) ? [ $.jd._settings.user, $.jd._settings.pass ]
					: /* anonymous */[ "", "" ], function(response, pid) {
				if ($.jd._settings.debug)
					console.log([ "Handshake response:", response ]);
				// Check if server is returning a session id. If so, our
				// handshake (either authenticated or anonymous) succeeded.
				if (response && typeof (response) === "string" && response !== ""
						&& response !== $.jd.e.sessionStatus.ERROR) {
					var status = ($.jd._settings.user) ? $.jd.e.sessionStatus.REGISTERED
							: $.jd.e.sessionStatus.ANONYMOUS;
					$.jd._ajax.token = response;
					$.jd._settings.user = user;
					$.jd._settings.pass = pass;
					$.jd._ajax.sessionStatus = status;
					if ($.isFunction(callback)) {
						callback({
							"status" : $.jd._ajax.sessionStatus,
							"data" : response
						});
					}
				} else if ($.isFunction(callback)) {
					callback({
						"status" : $.jd.e.sessionStatus.ERROR,
						"data" : response
					});
				}
			}, function(response) {
				if ($.isFunction(callback)) {
					callback({
						"status" : $.jd.e.sessionStatus.ERROR,
						"data" : response
					});
				}
			});

		},
		/**
		 * Stop a session.
		 * 
		 * @param {function(Object.<string, *>,?string=)=} callback
		 */
		stopSession : function(callback) {
			$.jd.stopPolling();
			$.jd._ajax.token = undefined;
			// No matter whether
			// the server
			// invalidates the
			// session, we do!
			$.jd._ajax.sessionStatus = $.jd.e.sessionStatus.NO_SESSION;
			$.jd
					.send("session/disconnect", callback, callback /*
																	 * As we
																	 * invalidate
																	 * the
																	 * session
																	 * locally,
																	 * there are
																	 * no errors
																	 * ;-P
																	 */);
		},
		/**
		 * Get the current session status. See sessionStatus enum.
		 * 
		 * @return {string|undefined}
		 */
		getSessionStatus : function() {
			return $.jd._ajax.sessionStatus;
		},
		/**
		 * Starts polling from the server continuously
		 * 
		 * @return {jQuery}
		 */
		startPolling : function() {

			if ($.jd.isPollingContinuously())
				return this;
			$.jd._ajax.active = true;
			$.jd._ajax.lastEventId = undefined;
			$.jd.pollOnce();
			return this;
		},

		/**
		 * Stops polling from the server continuously
		 * 
		 * @return {jQuery}
		 */
		stopPolling : function() {
			$.jd._ajax.active = false;
			if ($.jd._ajax.jqXHR && $.jd._ajax.jqXHR.abort)
				$.jd._ajax.jqXHR.abort();
			$.jd._ajax.lastEventId = undefined;
			return this;
		},

		/**
		 * Returns true if jquery is polling continuously
		 * 
		 * @return {boolean}
		 */
		isPollingContinuously : function() {
			return $.jd._ajax.active;
		},

		/**
		 * Poll events from the server once
		 * 
		 * @return {jQuery}
		 */
		pollOnce : function() {
			if ($.jd._ajax.sessionStatus === $.jd.e.sessionStatus.NO_SESSION) {
				$.jd.stopPolling();
				$.jd._ajax.handlePoll({
					"type" : $.jd.e.messageType.SYSTEM,
					"message" : $.jd.e.sysMessage.ERROR,
					"data" : {
						"status" : "No active session. Did you call startSession()?"
					}
				});
			} else {
				if ($.jd._ajax.jqXHR !== null) // abort running
					// requests
					$.jd._ajax.jqXHR.abort();
				$.jd._ajax.jqXHR = $.ajax({
					dataType : ($.jd._settings.sameDomain ? "json" : "jsonp"),
					type : "GET",
					url : $.jd._settings.apiServer + 'events/listen',
					data : {
						token : $.jd._ajax.token,
						lastEventId : $.jd._ajax.lastEventId
					},
					async : true,
					cache : false,
					timeout : $.jd._settings.apiTimeout,
					success : $.jd._ajax.handlePoll,
					complete : $.jd._ajax.pollComplete,
					error : $.jd._ajax.pollError
				});
			}
			return this;
		},

		/**
		 * Perform a certain action on the server
		 * 
		 * @param {string} action to perform.
		 * @param {function(Object.<string, *>,?string=)|Array.<string>=}
		 * params (optional) parameters for this action as an JSON array (e.g.
		 * ["param1",1,1.2,"param4"])
		 * @param {function(Object.<string, *>,?string=)=} onSuccess (optional)
		 * function to execute. Response and PID are supplied as function
		 * parameters
		 * @param {function(Object.<string, *>)=} onError (optional) function
		 * to execute if an error occurs. Response is supplied as function
		 * parameter.
		 * @param {function(Object.<string, *>,?string=)=} onEvent (optional)
		 * function to execute each time an event is polled that refers to this
		 * request. If this parameter is either omitted or this functions does
		 * *not* return false, the general poll callback is invoked, too.
		 * @returns {jQuery}
		 * @suppress {checkTypes}
		 */
		send : function(action, params, onSuccess, onError, onEvent) {
			// shift callback if params are undefined
			if ($.isFunction(params)) {
				onEvent = onError;
				onError = onSuccess;
				onSuccess = params;
				params = undefined;
			}

			// Check if callbacks are real functions
			// TODO: Check jQuery source whether this is necessary or not.
			if (!$.isFunction(onSuccess))
				onSuccess = undefined;
			if (!$.isFunction(onEvent))
				onEvent = undefined;
			if (!$.isFunction(onError))
				onError = undefined;

			// Remove leading /, it's already in the apiServer URL
			if (action[0] === "/")
				action = action.substr(1);

			// Try to interpolate if we didn't get an array for
			// params
			if ((!$.isArray(params)) && (params !== undefined)) {
				if ($.isPlainObject(params)) {
					var tmp = [];
					$.each(params, function(k, v) {
						tmp.push(v);
					});
					params = tmp;
				} else
					params = $.makeArray(params);
			}
			if ($.jd._settings.debug)
				console.log([ $.jd._settings.apiServer + action, params, onSuccess, onError, onEvent ]);
			$.ajax({
				dataType : ($.jd._settings.sameDomain ? "json" : "jsonp"),
				type : "GET",
				url : $.jd._settings.apiServer + action,
				data : {
					"token" : $.jd._ajax.token,
					"p" : params
				},
				async : true,
				cache : false,
				timeout : $.jd._settings.apiTimeout,
				success : function(event) {
					$.jd._ajax.sendSuccess(event, onSuccess, onError, onEvent);
				},
				error : function(a, b, c) {
					$.jd._ajax.sendError(a, b, c, onError);
				}
			});
			return this;
		},

		/**
		 * Default settings
		 */
		_settings : {
			/**
			 * Username for API auth
			 * 
			 * @type {(string|undefined)}
			 */
			user : undefined,
			/**
			 * pass for API auth
			 * 
			 * @type {(string|undefined)}
			 */
			pass : undefined,
			/**
			 * API server root url
			 * 
			 * @type {string}
			 */
			apiServer : 'http://127.0.0.1:3128/',
			/**
			 * Callback function to be called if an event is recieved.
			 * 
			 * @type {function(Object,number=)|undefined}
			 */
			onmessage : undefined,
			/**
			 * Callback function to be called if an error occured.
			 * 
			 * @type {function(Object)|undefined}
			 */
			onerror : undefined,
			/**
			 * Debug Mode
			 * 
			 * @type {boolean}
			 */
			debug : false,
			/**
			 * Timeout until API calls time out. Applies for both polling (once)
			 * and sending.
			 * 
			 * @type {number}
			 */
			apiTimeout : 15000,
			/**
			 * Use JSON instead of JSONP requests. (Force same domain origin)
			 * 
			 * @type {boolean}
			 */
			sameDomain : false
		},
		// enums
		e : {
			/**
			 * e for login status
			 * 
			 * @const {string|undefined}
			 */
			sessionStatus : {
				NO_SESSION : undefined,
				ERROR : "error",
				ANONYMOUS : "anonymous",
				REGISTERED : "registered"
			},
			/**
			 * e for polling message type
			 * 
			 * @const {string}
			 */
			messageType : {
				SYSTEM : "system"
			},
			/**
			 * e for polling system message type
			 * 
			 * @const {string}
			 */
			sysMessage : {
				ERROR : "error",
				DISCONNECT : "disconnect",
				HEARTBEAT : "heartbeat",
				OUT_OF_SYNC: "outofsync"
			}
		},
		// internal functions
		_ajax : {
			/**
			 * Contains the last received message id. If connection fails due to
			 * timeouts, messages will get fetched again.
			 */
			lastEventId : undefined,
			/**
			 * Status of continuous polling.
			 */
			active : false,
			/**
			 * Stores the active jqXHR object for polling. Needed for aborting
			 * requests
			 */
			jqXHR : null,
			/**
			 * Callback map storing callback functions for async requests. {pid :
			 * callback}
			 */
			callbackMap : {},
			/**
			 * Current security token
			 */
			token : undefined,
			/**
			 * Current session status
			 * 
			 * @type {string|undefined}
			 */
			sessionStatus : undefined,
			/**
			 * Trigger/Register callbacks associated with a certain request
			 * (fixed process id)
			 * 
			 * @param event direct return value of the request
			 * @param {function(Object.<string, *>,?string=)=} callback to be
			 * called immediately
			 * @param {function(Object.<string, *>,?string=)=} processCallback
			 * to be called if further events associated with this pid are
			 * streamed.
			 */
			sendSuccess : function(event, onSuccess, onError, onEvent) {
				
				//Check for errors
				if (event.type && event.type === $.jd.e.messageType.SYSTEM && event.message && event.message === $.jd.e.sysMessage.ERROR) {
					onError(event.data);
					return;
				}
				
				// register processCallback
				if (event.pid && $.isFunction(onEvent)) {
					$.jd._ajax.callbackMap[event.pid] = onEvent;
					if ($.jd._settings.debug)
						console.log([ "Register PID...", event ]);
				}
				// run normal callback
				if (jQuery.isFunction(onSuccess))
					onSuccess(event.data, event.pid);
			},
			/**
			 * Handles completed poll from the server
			 * 
			 * @param {Object.<string,*>} event data received from the server
			 */
			handlePoll : function(event) {
				// accept message if lastEventId is valid. System events come out
				// of scope
				if (($.jd._ajax.lastEventId === undefined || (event.data && ($.jd._ajax.lastEventId === (event.id - event.data.length))))
						|| (event.type && event.type === $.jd.e.messageType.SYSTEM)) {
					if (event.data && !(event.type && event.type == $.jd.e.messageType.SYSTEM))
						$.jd._ajax.lastEventId = event.id;
					
					//System events will be handeled internally and might be passed to the onerror function
					if (event.type && event.type === $.jd.e.messageType.SYSTEM) {
						switch (event.message) {
						case $.jd.e.sysMessage.DISCONNECT:
							$.jd.stopPolling();
							if ($.isFunction($.jd._settings.onerror))
								$.jd._settings.onerror({"status":$.jd.e.sysMessage.DISCONNECT});
						break;
						case $.jd.e.sysMessage.HEARTBEAT:
						break;
						case $.jd.e.sysMessage.OUT_OF_SYNC:
							$.jd.stopPolling();
							if ($.isFunction($.jd._settings.onerror))
								$.jd._settings.onerror({"status":$.jd.e.sysMessage.OUT_OF_SYNC});
							break;
						default:
							if ($.isFunction($.jd._settings.onerror))
								$.jd._settings.onerror(event.data);
						break;
						}
					} else // We recieved a regular event.
					{
						if ($.jd._settings.debug)
							console.log(event.data.length + " events in this message.");

						$.each(event.data, function(i, e) {
							$.jd._ajax.handleEvent(e);
						});
					}
				}
			},
			/**
			 * Trigger all callbacks belonging to the given event
			 * 
			 * @param {Object.<string, *>} event the event
			 * @suppress {checkTypes}
			 */
			handleEvent : function(event) {
				if (event.pid && (event.pid in $.jd._ajax.callbackMap)) {
					// If specific callback returns false,
					// don't trigger general callback
					if ($.jd._ajax.callbackMap[event.pid](event.data, event.pid) === false)
						return;

				}
				if ($.isFunction($.jd._settings.onmessage))
					$.jd._settings.onmessage(event.data, event.pid);
			},
			/**
			 * Checks if continuous polling is active and polls again if
			 * necessary
			 * 
			 * @param jqXHR see jQuery.ajax doc
			 * @param textStatus see jQuery.ajax doc
			 */
			pollComplete : function(jqXHR, textStatus) {
				$.jd._ajax.jqXHR = null;
				if ($.jd._ajax.active && $.jd._ajax.active === true) {
					if ($.jd._settings.debug === true) {
						setTimeout($.jd.pollOnce, 1000);
					} else {
						$.jd.pollOnce();
					}
				}
			},
			/**
			 * Handle ajax poll error.
			 * 
			 * @param jqXHR see jQuery.ajax doc
			 * @param textStatus see jQuery.ajax doc
			 * @param errorThrown see jQuery.ajax doc
			 */
			pollError : function(jqXHR, textStatus, errorThrown) {
				$.jd._ajax.handlePoll({
					"type" : $.jd.e.messageType.SYSTEM,
					"message" : $.jd.e.sysMessage.ERROR,
					"data" : {
						"jqXHR" : jqXHR,
						"status" : textStatus,
						"errorThrown" : errorThrown
					}
				});
			},
			/**
			 * Handle ajax errors for sending.
			 * 
			 * @param jqXHR see jQuery.ajax doc
			 * @param textStatus see jQuery.ajax doc textStatus
			 * @param errorThrown see jQuery.ajax doc
			 * @param {function(Object.<string, *>,?string=)=} onError
			 * additional callback to be executed
			 */
			sendError : function(jqXHR, textStatus, errorThrown, onError) {
				if ($.isFunction(onError))
					onError({
						"jqXHR" : jqXHR,
						"status" : textStatus,
						"errorThrown" : errorThrown
					});
			}

		}
	};

})(jQuery);