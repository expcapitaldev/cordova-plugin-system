'use strict';

var SystemPlugin = (function () {

	var exec = require('cordova/exec');

	function SystemPlugin () {}

	SystemPlugin.prototype = {

		setTextZoom: function (textZoom) {
			return new Promise(function (resolve, reject) {
				if (!Number.isFinite(textZoom)) {
					return reject('SystemPlugin invalid argument');
				}
				exec(resolve, reject, 'SystemPlugin', 'setTextZoom', [textZoom]);
			});
		},

	};
	return SystemPlugin;
})();

var System = new SystemPlugin();

module.exports = System;
