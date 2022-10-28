# Cordova System Plugin


### Supported Platforms

- Android
- iOS

## Types

```
interface Cordova {
	plugins: CordovaPlugins;
}

interface CordovaPlugins {
	System: ISystemPlugin;
}

/**
 * Catch block return only string error
 */
interface ISystemPlugin {
	/**
     * Android, iOS
	 * Set the current text zoom percent value for the WebView
	 * @param textZoom, number
	 */
	setTextZoom(textZoom: number): Promise<void>;

    /**
     * Android, iOS
     * Return sorted array of IMailClient by label if app exists
     * Notes:
     * for iOS: See list of email apps
     * for Android: return all available email apps from device
     *
     */
    getAvailableMailClients(): Promise<IMailClient[]>;

    /**
     * Android, iOS
     * Try to open email app by package/scheme
     *
     * @param scheme from IMailClient, try to open scheme,
     * by default will open native window to download Mail iOS app
     *
     */
    openEmailApp(scheme?: string): Promise<void>;
    
    /**
     * Android only
     * Check browser version and show restriction page if application is not supported
     * @param modulesNotSupported - indicate that the ES modules not supported. < 61 version
     */
    checkBrowserCompatibility(modulesNotSupported: boolean): void;
}

interface IMailClient {
    /**
     * label
     * iOS: it is app name from email list
     * Android: app name from package
     */
    label: string;
    /**
     * iOS: see email list
     * Android: package name
     */
    package: string;
}


```


## Example

```typescript

cordova.plugins.System.setTextZoom(100);

```
## Plugin variables

The following Cordova plugin variables are supported by the plugin.
Note that these must be set at plugin installation time. If you wish to change plugin variables, you'll need to uninstall the plugin and reinstall it with the new variable values.

### Android only
#### `checkBrowserCompatibility()` method supports next variables:
- `RESTRICTION_PAGE_THEME` - sets `cc` or `cx`, style for restriction page. Default valus is `cc`.
- `RESTRICTION_TITLE_KEY` - key name from `strings.xml` with translated text for restriction page, see `res/android/assets/index.html`, will replace `TITLE_TEXT`
- `RESTRICTION_DESCRIPTION_KEY` - key name from `strings.xml` with translated text for restriction page, see `res/android/assets/index.html`,  will replace `DESC_TEXT`
- `RESTRICTION_BUTTON_KEY` - key name from `strings.xml` with translated text for restriction page, see `res/android/assets/index.html`,  will replace `BUTTON_TEXT`

## Methods 
see additional information in `types`

### checkBrowserCompatibility
Android only.

Check browser version (Chrome) and show restriction page if application is not supported.
SUPPORTED_CHROME_VERSION = 63. See `www/utils.js`.

If system can get Chrome version from `user_agent` and Chrome version less than SUPPORTED_CHROME_VERSION, or `modulesNotSupported` variable is `true` system will show restriction page.

System will try to get `browser language`, expected default project language from `window.env.DEFAULT_LANGUAGE (string value expected)` variable and validate `browser language` with `window.env.LANGUAGES string[] type expected` variable.

 - Default `browser language` is `en` if system can not get language.
 - `window.env.DEFAULT_LANGUAGE` and `window.env.LANGUAGES` is optional.
 - System will try to get translation keys `(see Plugin variables)` from `string.xml` file.
 - translation keys in `strings.xml` required for default project language
 - when the restrictions page is shown, the system will try to hide the splash screen if the splash screen exists `navigator.splashscreen.hide();`
 - after click on the button on will open next link in supported application `https://play.google.com/store/apps/details?id=com.google.android.webview`

**Parameters**:
- {boolean} modulesNotSupported - it is mean that older browsers that do not support modular JavaScript code. See `https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script#attr-nomodule`
