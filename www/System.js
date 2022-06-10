'use strict';

var SystemPlugin = (function () {

	var exec = require('cordova/exec');

    var mailClients = [
        {
            title: 'Spark',
            scheme: 'readdlespark:'
        },
        {
            title: 'Gmail',
            scheme: 'googlegmail:'
        },
        {
            title: 'Apple Mail',
            scheme: 'com.apple.mobilemail:'
        },
        {
            title: 'Outlook',
            scheme: 'ms-outlook:'
        },
        {
            title: 'Spike',
            scheme: 'spike:'
        },
        {
            title: 'Airmail',
            scheme: 'airmail:'
        },
        {
            title: 'Edison Mail',
            scheme: 'edisonmail:'
        },
        {
            title: 'Twobird',
            scheme: 'twobird:'
        },
        {
            title: 'Hey',
            scheme: 'hey:'
        },
        {
            title: 'ProtonMail',
            scheme: 'protonmail:'
        },
        {
            title: 'Polymail',
            scheme: 'polymail:'
        },
        {
            title: 'Blue mail',
            scheme: 'bluemail:'
        },
        {
            title: 'Newton Mail',
            scheme: 'cloudmagic:'
        },
        {
            title: 'Yandex.Mail',
            scheme: 'yandexmail:'
        },
        {
            title: 'Mail.ru',
            scheme: 'mailrumail:'
        },
        {
            title: 'myMail',
            scheme: 'mycom-mail-x-callback:'
        },
        {
            title: 'Canary Mail',
            scheme: 'canary:'
        },
        {
            title: 'Yahoo Mail',
            scheme: 'ymail:'
        }
    ];

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
                if (!Array.isArray(mailClients)) {
                    return reject(pluginName + ' mailClients error');
                }
                exec(resolve, reject, 'SystemPlugin', 'getAvailableMailClients', [mailClients]);
            });
        },


	};
	return SystemPlugin;
})();

var System = new SystemPlugin();

module.exports = System;
