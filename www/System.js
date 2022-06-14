'use strict';

var SystemPlugin = (function () {

	var exec = require('cordova/exec');
    var pluginName = 'SystemPlugin';

	function SystemPlugin () {}

	SystemPlugin.prototype = {

		setTextZoom: function (textZoom) {
			return new Promise(function (resolve, reject) {
				if (!Number.isFinite(textZoom)) {
					return reject(pluginName + ' invalid argument');
				}
				exec(resolve, reject, pluginName, 'setTextZoom', [textZoom]);
			});
		},
        openEmailApp: function (scheme) {
            return new Promise(function (resolve, reject) {
                var args = [];
                if (typeof scheme === 'string') {
                    args = [scheme];
                }
                exec(resolve, reject, pluginName, 'openEmailApp', args);
            });
        },
        getAvailableMailClients: function () {
            return new Promise(function (resolve, reject) {
                exec(resolve, reject, 'SystemPlugin', 'getAvailableMailClients', []);
            });
        },


	};
	return SystemPlugin;
})();

var System = new SystemPlugin();

module.exports = System;
