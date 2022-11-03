/**
 * Utilities and shared functionality for the build hooks.
 */
const parser = require('xml-js');
const fs = require('fs');
const path = require("path");

let _configXml, _pluginXml, _context, _pluginVariables;

const Utilities = {};

fs.ensureDirSync = function(dir){
    if(!fs.existsSync(dir)){
        dir.split(path.sep).reduce(function(currentPath, folder){
            currentPath += folder + path.sep;
            if(!fs.existsSync(currentPath)){
                fs.mkdirSync(currentPath);
            }
            return currentPath;
        }, '');
    }
};

Utilities.setContext = function(context){
    _context = context;
};

/**
 * The ID of the plugin; this should match the ID in plugin.xml.
 */
Utilities.getPluginId = function(){
    // if(!_context) throw "Cannot retrieve plugin ID as hook context is not set";
    return _context.opts.plugin.id;
};

Utilities.parsePluginXml = function(){
    if(_pluginXml) return _pluginXml;
    _pluginXml = Utilities.parseXmlFileToJson("plugins/"+Utilities.getPluginId()+"/plugin.xml");
    return _pluginXml;
};

Utilities.parseXmlFileToJson = function(filepath, parseOpts){
    parseOpts = parseOpts || {compact: true};
    return JSON.parse(parser.xml2json(fs.readFileSync(path.resolve(filepath), 'utf-8'), parseOpts));
};

Utilities.parseConfigXml = function(){
    if(_configXml) return _configXml;
    _configXml = Utilities.parseXmlFileToJson("config.xml");
    return _configXml;
};

Utilities.parsePackageJson = function(){
    return JSON.parse(fs.readFileSync(path.resolve('./package.json')));
};

Utilities.parsePluginVariables = function(){
    if(_pluginVariables) return _pluginVariables;

    var pluginVariables = {};

    // Parse plugin.xml
    var plugin = Utilities.parsePluginXml();
    var prefs = [];
    if(plugin.plugin.preference){
        prefs = prefs.concat(plugin.plugin.preference);
    }
    if(typeof plugin.plugin.platform.length === 'undefined') plugin.plugin.platform = [plugin.plugin.platform];
    plugin.plugin.platform.forEach(function(platform){
        if(platform.preference){
            prefs = prefs.concat(platform.preference);
        }
    });
    prefs.forEach(function(pref){
        if (pref._attributes){
            pluginVariables[pref._attributes.name] = pref._attributes.default;
        }
    });

    // Parse config.xml
    var config = Utilities.parseConfigXml();
    (config.widget.plugin ? [].concat(config.widget.plugin) : []).forEach(function(plugin){
        (plugin.variable ? [].concat(plugin.variable) : []).forEach(function(variable){
            if((plugin._attributes.name === Utilities.getPluginId() || plugin._attributes.id === Utilities.getPluginId()) && variable._attributes.name && variable._attributes.value){
                pluginVariables[variable._attributes.name] = variable._attributes.value;
            }
        });
    });

    // Parse package.json
    var packageJSON = Utilities.parsePackageJson();
    if(packageJSON.cordova && packageJSON.cordova.plugins){
        for(const pluginId in packageJSON.cordova.plugins){
            if(pluginId === Utilities.getPluginId()){
                for(const varName in packageJSON.cordova.plugins[pluginId]){
                    var varValue = packageJSON.cordova.plugins[pluginId][varName];
                    pluginVariables[varName] = varValue;
                }
            }
        }
    }

    _pluginVariables = pluginVariables;
    return pluginVariables;
};

Utilities.copyFileSync = function(src, dest) {
    var folder = dest.substring(0, dest.lastIndexOf('/'));
    fs.ensureDirSync(folder);
    fs.copyFileSync(path.resolve(src), path.resolve(dest));
}

Utilities.log = function(msg){
    console.log(Utilities.getPluginId()+': '+msg);
};

Utilities.logError = function(message) {
    console.error(Utilities.getPluginId()+ ' error: ' + message);
    throw new Error(message);
}

module.exports = Utilities;
