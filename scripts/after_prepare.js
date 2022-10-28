'use strict';

const fs = require('fs');
const path = require("path");
const utilities = require("./lib/utilities");

let pluginVariables = {};
const ANDROID_DIR = 'platforms/android';

module.exports = function(context){
    //get platform from the context supplied by cordova
    var platforms = context.opts.platforms;
    utilities.setContext(context);

    if (platforms.indexOf('android') !== -1) {
        utilities.log('Preparing on Android');

        pluginVariables = utilities.parsePluginVariables();
        const theme = pluginVariables.RESTRICTION_PAGE_THEME;

        switch (theme) {
            case 'cc':
            case 'cx':
                break;
            default:
                utilities.logError('RESTRICTION_PAGE_THEME variable is not valid');
        }
        // we don't need to copy to the www/ folder, but it's more clear for developers that this is the code used in the application
        utilities.copyFileSync('./plugins/' + utilities.getPluginId() + '/res/android/assets/' + theme + '/style.css', ANDROID_DIR + '/app/src/main/assets/www/browser-restriction-page/style.css');
        utilities.copyFileSync('./plugins/' + utilities.getPluginId() + '/res/android/assets/index.html', ANDROID_DIR + '/app/src/main/assets/www/browser-restriction-page/index.html');
    }

};
