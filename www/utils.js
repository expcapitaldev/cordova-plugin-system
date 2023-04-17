'use strict'

/**
 * Supported Chrome version for current project (for webworkers too)
 * 63 need to Promise.finally() and only for JS part
 * 73 style ionic components we must use ::part selector, which is supported only in 73 version of Web View.
 * https://ionicframework.com/docs/theming/css-shadow-parts and https://caniuse.com/mdn-css_selectors_part
 */
var SUPPORTED_CHROME_VERSION = 73;

var PLUGIN_NAME = 'SystemPlugin';

var IONIC_VARIABLE_SAFE_AREA_TOP = '--ion-safe-area-top';
var IONIC_VARIABLE_SAFE_AREA_RIGHT = '--ion-safe-area-right';
var IONIC_VARIABLE_SAFE_AREA_BOTTOM = '--ion-safe-area-bottom';
var IONIC_VARIABLE_SAFE_AREA_LEFT = '--ion-safe-area-left';

var IONIC_SAFE_AREA_TOP_DEFAULT_VALUE = 'env(safe-area-inset-top)';
var IONIC_SAFE_AREA_RIGHT_DEFAULT_VALUE = 'env(safe-area-inset-right)';
var IONIC_SAFE_AREA_BOTTOM_DEFAULT_VALUE = 'env(safe-area-inset-bottom)';
var IONIC_SAFE_AREA_LEFT_DEFAULT_VALUE = 'env(safe-area-inset-left)';

/**
 * Supported Chrome version for current project (for webworkers too)
 * @return {number}
 */
function getSupportedChromeVersion() {
    return SUPPORTED_CHROME_VERSION;
}

/**
 * Return true for android platform
 * @return {boolean}
 */
function isAndroid() {
    return cordova.platformId === 'android';
}

/**
 * Safety check
 * @return {boolean}
 */
function isNavigatorDefined() {
    return typeof window !== 'undefined' && typeof window.navigator !== 'undefined';
}

/**
 * See https://developer.chrome.com/docs/multidevice/user-agent/#webview_user_agent
 * Return undefined if can not get Chrome version
 * @return {number|undefined}
 */
function getChromeBrowserVersion() {
    if (!isNavigatorDefined()) {
        return undefined;
    }
    if (window.navigator.userAgent.indexOf('Chrome/') === -1) {
        return undefined;
    }
    var regResult = /Chrome\/([0-9]+)/.exec(window.navigator.userAgent)
    if (!regResult) {
        return undefined;
    }
    return +regResult[1];
    // return (/Chrome\/([0-9]+)/.exec(window.navigator.userAgent)||[0,0])[1];
}

/**
 * Get supported language for project
 * @return {string}
 */
function calculateBrowserLanguage() {

    function getBrowserLang() {
        if (!isNavigatorDefined()) {
            return undefined;
        }
        var browserLang = window.navigator.languages ? window.navigator.languages[0] : null;
        browserLang = browserLang || window.navigator.language || window.navigator.browserLanguage || window.navigator.userLanguage;
        // if (typeof browserLang === 'undefined') {
        if (typeof browserLang !== 'string') {
            return undefined;
        }
        if (browserLang.indexOf('-') !== -1) {
            browserLang = browserLang.split('-')[0];
        }
        if (browserLang.indexOf('_') !== -1) {
            browserLang = browserLang.split('_')[0];
        }
        return browserLang;
    }

    function isWindowEnvExists() {
        return typeof window !== 'undefined' && typeof window.env !== 'undefined';
    }

    function getProjectDefaultLanguage() {
        if (!isWindowEnvExists() || typeof window.env.DEFAULT_LANGUAGE !== 'string') {
            console.error('DEFAULT_LANGUAGE is not defined');
            return undefined;
        }
        return window.env.DEFAULT_LANGUAGE;
    }

    function getProjectLanguages() {
        if (!isWindowEnvExists() || !isArray(window.env.LANGUAGES)) {
            console.error('LANGUAGES is not defined');
            return [];
        }
        return window.env.LANGUAGES;
    }

    var detectedLang = getBrowserLang() || getProjectDefaultLanguage() || 'en';
    if (detectedLang === 'zh') {
        detectedLang = 'zh_Hans';
    }
    var projectLanguages = getProjectLanguages();
    if (projectLanguages && projectLanguages.length && new RegExp('(' + projectLanguages.join('|') + ')', 'gi').test(detectedLang)) {
        return detectedLang;
    }
    return 'en';
}

/**
 * Returns an indication of whether the argument is an array or not
 * see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/toString
 * @param val
 * @return {arg is any[]|boolean}
 */
function isArray(val) {
    if (Array.isArray) {
        return Array.isArray(val);
    } else {
        return Object.prototype.toString.call(val).slice(8, -1) === 'Array';
    }
}

function logError(errorString) {
    console.error(PLUGIN_NAME + ' error : ' + errorString)
}

function isValidUrl(url) {
    try {
        new URL(url);
        return true;
    } catch {
        return false;
    }
}

module.exports = {
    isAndroid: isAndroid,
    getChromeBrowserVersion: getChromeBrowserVersion,
    getSupportedChromeVersion: getSupportedChromeVersion,
    calculateBrowserLanguage: calculateBrowserLanguage,
    isNavigatorDefined: isNavigatorDefined,
    logError: logError,
    pluginName: PLUGIN_NAME,
    isValidUrl: isValidUrl,
    ionicVariableSafeAreaTop: IONIC_VARIABLE_SAFE_AREA_TOP,
    ionicVariableSafeAreaRight: IONIC_VARIABLE_SAFE_AREA_RIGHT,
    ionicVariableSafeAreaBottom: IONIC_VARIABLE_SAFE_AREA_BOTTOM,
    ionicVariableSafeAreaLeft: IONIC_VARIABLE_SAFE_AREA_LEFT,
    ionicSafeAreaTopDefaultValue: IONIC_SAFE_AREA_TOP_DEFAULT_VALUE,
    ionicSafeAreaRightDefaultValue: IONIC_SAFE_AREA_RIGHT_DEFAULT_VALUE,
    ionicSafeAreaBottomDefaultValue: IONIC_SAFE_AREA_BOTTOM_DEFAULT_VALUE,
    ionicSafeAreaLeftDefaultValue: IONIC_SAFE_AREA_LEFT_DEFAULT_VALUE,
}
