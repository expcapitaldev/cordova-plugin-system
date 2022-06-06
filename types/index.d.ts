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
	 * Set the current text zoom percent value for the WebView
	 * @param textZoom, number
	 */
	setTextZoom(textZoom: number): Promise<void>;
}
