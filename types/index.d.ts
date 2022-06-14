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
