var CloudCheckResult = Object.freeze({
    CLEAR: 0,
    ERROR: 1,
    INVALID: 2,
    AVAILABLE: 3,
    UNAVAILABLE: 4
});

(function ($) {

    $.fn.checkCloudName = function (opts) {

        // Default options
        var options = $.extend({
            delay: 400,                             // How long to wait after user has stopped typing
            apiUrl: 'checkCloudName?cloudName=',    // URL of cloud name restful API
            completed: null,                        // Callback complete
            changed: null,                          // Callback after name change
            debug: false                            // Whether to show console logging
        }, opts);

        var $elem = $(this),
            ajaxReq,
            reqTimeout,
            cloudName = '';

        // After user has stopped typing, check cloud name
        function checkCloudName(event, delay) {
            cloudName = event.data;

            // Delay calling of cloud check
            reqTimeout = setTimeout(function () {
                ajaxReq = $.ajax({
                    url: options.apiUrl + encodeURIComponent(cloudName),
                    dataType: 'jsonp',
                    error: function (jqXHR, textStatus) {
                        if (textStatus !== 'abort') {
                            callOnComplete(CloudCheckResult.ERROR);
                        }
                    },
                    success: function (data) {
                        if (data['isError'] === true) {
                            callOnComplete(CloudCheckResult.ERROR)
                        } else if (data['isValid'] === false) {
                            callOnComplete(CloudCheckResult.INVALID)
                        } else {
                            var result = (data['isAvailable'] === true) ? CloudCheckResult.AVAILABLE : CloudCheckResult.UNAVAILABLE;
                            callOnComplete(result);
                        }
                    }
                });
            }, delay);
        }

        // Hook element events
        $elem.on('change keydown keyup paste input blur', function (event) {

            var newCloudName = $elem.val().toLowerCase().trim();
            if (options.debug) {
                console.log('event.type=' + event.type + ', newCloudName=' + newCloudName + ', cloudName=' + cloudName);
            }

            // deleted/cleared - don't add prefix, just show normal hint text
            if (newCloudName.length == 0) {
                cancelReq();
                callOnComplete(CloudCheckResult.CLEAR);
                return;
            }

            // ensure input starts with =
            if (!newCloudName.startsWith('=')) {
                newCloudName = '=' + newCloudName;
                $elem.val(newCloudName);
                $elem.trigger(jQuery.Event(event.type));
                return;
            }

            // ignore if unchanged (e.g. from arrow keys or something)
            if (newCloudName == cloudName) {
                return;
            }

            // text has changed - cancel prev
            cancelReq();

            // call changed
            callOnChange(newCloudName);

            // check name
            event.data = newCloudName;
            var delay = (event.type == 'blur') ? 0 : options.delay; // no delay for blur event
            checkCloudName(event, delay);
        });

        function cancelReq() {
            if (reqTimeout) {
                clearTimeout(reqTimeout);
            }
            if (ajaxReq) {
                ajaxReq.abort();
            }
        }

        function callOnChange(newCloudName) {
            if (typeof options.changed === "function") {
                options.changed.call(options, newCloudName);
            }
        }

        function callOnComplete(result) {
            if (typeof options.completed === "function") {
                options.completed.call(options, result);
            } else {
                if (options.debug) {
                    console.log('completed function undefined');
                }
            }
        }

        $elem.trigger(jQuery.Event('change'));
    };

})(jQuery);
