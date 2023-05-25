'use strict';

const fs = require('fs');
const path = require("path");
const utilities = require("./lib/utilities");
const xcode = require("xcode");

let pluginVariables = {};
const ANDROID_DIR = 'platforms/android';
const IOS_DIR = 'platforms/ios';

module.exports = function(context){
    //get platform from the context supplied by cordova
    var platforms = context.opts.platforms;
    utilities.setContext(context);

    if (platforms.indexOf('android') !== -1) {
        try {
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
        } catch (e) {
            utilities.logError(e);
        }
    }

    if (platforms.indexOf('ios') !== -1) {
        try {
            utilities.log('Preparing on iOS');

            const xcodeProjPath = utilities.fromDir(IOS_DIR, ".xcodeproj", false);
            const projectPath = xcodeProjPath + "/project.pbxproj";
            const project = xcode.project(projectPath);

            project.parseSync();

            const firstTarget = project.getFirstTarget();
            const productName = firstTarget.firstTarget.productName;

            // Default values:
            // SWIFT_MODULE_NAME = $(PRODUCT_MODULE_NAME)
            // PRODUCT_MODULE_NAME = $(PRODUCT_NAME:c99extidentifier)
            // PRODUCT_NAME = $(TARGET_NAME:c99extidentifier)
            // in our case we need to replace our constant to SWIFT_MODULE_NAME

            const pathToFile = path.resolve(`${IOS_DIR}/${utilities.unquote(firstTarget.firstTarget.productName)}/Plugins/cordova-plugin-system`, 'CDVReachabilityManager.h');
            if (!fs.existsSync(pathToFile) || !fs.statSync(pathToFile).isFile()) {
                utilities.logError('CDVReachabilityManager does not find');
                return;
            }

            let fileContent = fs.readFileSync(pathToFile, 'utf8');
            fileContent = fileContent.replace(utilities.REPLACE_SWIFT_MODULE_NAME, utilities.convertC99ExtIdentifier(productName));
            fs.writeFileSync(pathToFile, fileContent, 'utf8');

        } catch (e) {
            utilities.logError(e);
        }
    }

};
