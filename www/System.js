'use strict'

var utils = require('./utils')
var SystemPlugin = (function() {

    var exec = require('cordova/exec')

    var SystemPlugin = {}

    SystemPlugin.setTextZoom = function(textZoom) {
        return new Promise(function(resolve, reject) {
            if (!Number.isFinite(textZoom)) {
                return reject(utils.pluginName + ' invalid argument')
            }
            exec(resolve, reject, utils.pluginName, 'setTextZoom', [textZoom])
        })
    }
    SystemPlugin.openEmailApp = function(scheme) {
        return new Promise(function(resolve, reject) {
            var args = []
            if (typeof scheme === 'string') {
                args = [scheme]
            }
            exec(resolve, reject, utils.pluginName, 'openEmailApp', args)
        })
    }
    SystemPlugin.getAvailableMailClients = function() {
        return new Promise(function(resolve, reject) {
            exec(resolve, reject, utils.pluginName, 'getAvailableMailClients', [])
        })
    }

    /**
     * Check browser version and show restriction page if application is not supported
     * @param modulesNotSupported - indicate that the ES modules not supported. < 61 version
     * void
     */
    SystemPlugin.checkBrowserCompatibility = function(modulesNotSupported) {
        if (!utils.isAndroid()) {
            return
        }
        var chromeVersion = utils.getChromeBrowserVersion()
        var unexpectedChromeVersion = chromeVersion && chromeVersion < utils.getSupportedChromeVersion()
        if (modulesNotSupported || unexpectedChromeVersion) {
            var browserLanguage = utils.calculateBrowserLanguage()

            new Promise(function(resolve, reject) {
                exec(resolve, reject, utils.pluginName, 'openBrowserRestrictionPage', [browserLanguage])
            })
                .then(function() {
                    if (!utils.isNavigatorDefined() || typeof navigator.splashscreen === 'undefined' || typeof navigator.splashscreen.hide !== 'function') {
                        return
                    }
                    navigator.splashscreen.hide()
                })
                .catch(function(errorString) {
                    utils.logError(errorString);
                })
        }
    }

    SystemPlugin.openSystemWebView = function() {
        if (!utils.isAndroid()) {
            return;
        }
        new Promise(function(resolve, reject) {
            exec(resolve, reject, utils.pluginName, 'openSystemWebView', []);
        })
            .catch(function(errorString) {
                utils.logError(errorString);
            });
    }

    SystemPlugin.startNetworkInfoNotifier = function(url, success, error) {
        if (utils.isAndroid()) {
            exec(success, error, utils.pluginName, 'startNetworkInfoNotifier', [])
        } else {
            if (utils.isValidUrl(url)) {
                exec(success, error, utils.pluginName, 'startNetworkInfoNotifier', [url])
            } else {
                error('Invalid URL');
            }
        }
    }

    SystemPlugin.stopNetworkInfoNotifier = function() {
        return new Promise(function(resolve, reject) {
            exec(resolve, reject, utils.pluginName, 'stopNetworkInfoNotifier', [])
        })
    }


    return SystemPlugin

})

module.exports = new SystemPlugin()
