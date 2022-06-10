interface Cordova {
	plugins: CordovaPlugins;
}

interface CordovaPlugins {
	System: ISystemPlugin;
}

interface ISystemPlugin {
	/**
	 * Catch block return only string error
	 */

	/**
     * Android, iOS
	 * Set the current text zoom percent value for the WebView
	 * @param textZoom, number
	 */
	setTextZoom(textZoom: number): Promise<void>;

    /**
     * iOS
     * Return array of IMailClient if app exists
     * See list of email apps
     *
     */
    getAvailableMailClients(): Promise<IMailClient[]>;

    /**
     * Android, iOS
     * Create chooser on Android  with all app
     *
     * @param scheme, for iOS only, required for ios, try to open scheme,
     * by default will open native window to download Mail iOS app
     *
     */
    openEmailApp(scheme?: string): Promise<void>;
}

interface IMailClient {
    title: string;
    scheme: string;
}
