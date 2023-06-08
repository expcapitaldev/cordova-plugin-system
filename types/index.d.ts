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

    /**
     * Android only
     * Open next link in supported application https://play.google.com/store/apps/details?id=com.google.android.webview
     */
    openSystemWebView(): void;

    /**
     * iOS/Android
     * successCallback will be called only if network exists and if "transport|proxy|addresses" was changed (anything)
     * @see INetworkInfo
     * Method supported only 1 instance
     * Android only for api level 24 and more
     * @param url only for iOS, see https://developer.apple.com/documentation/cfnetwork/1426639-cfnetworkcopyproxiesforurl
     * @param successCallback
     * @param errorCallback
     */
    startNetworkInfoNotifier(url: string | undefined, successCallback: (result: INetworkInfo) => void, errorCallback: (error: string) => void): void;

    /**
     * Catch can return only string value
     */
    stopNetworkInfoNotifier(): Promise<void>;

    /**
     * iOS/Android
     * Preventing content of the window from appearing in screenshots or video recording
     *
     * Notes: on iOS it is not public api, it is manipulation with iOS layers, after that window do not listen safe area inset values,
     * so before enabling we will set value and return back default Ionic css value again
     *
     * iOS
     * Warning: This method implementation needs to be rechecked by the developer each time a new version of Ionic is released.
     * Warning: This method can only be used by a developer who understands the implementation of the method
     *
     * Catch can return only string value
     */
    enableScreenProtection(): Promise<void>

    /**
     * iOS/Android
     * Enable screenshots and video recording
     *
     * Catch can return only string value
     */
    disableScreenProtection(): Promise<void>;

    /**
     * iOS only
     * Detect insecure jailbroken device
     *
     * Catch can return only string value
     */
    isJailbroken(): Promise<boolean>;
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

/**
 * Can not be empty
 */
interface INetworkInfo {
    /**
     * array of active transport:
     * for ios: WIFI, CELLULAR, UNKNOWN only one active
     * for android see transports: @see https://developer.android.com/reference/android/net/NetworkCapabilities#TRANSPORT_BLUETOOTH
     * for android it is String[]: "WIFI" + "VPN" for example and etc
     */
    transport?: string[];
    proxy?: {
        /**
         * iOS only, @see https://developer.apple.com/documentation/cfnetwork/proxy_types?language=objc
         */
        type?: string;
        host?: string;
        port?: string;
        /**
         * iOS only
         * This key is only present for proxies of type kCFProxyTypeAutoConfigurationURL.
         * @see https://developer.apple.com/documentation/cfnetwork/kcfproxytypeautoconfigurationurl?language=objc
         * @see https://developer.apple.com/documentation/cfnetwork/kcfproxyautoconfigurationurlkey?language=objc
         */
        urlKey?: string;
        /**
         * android only, @see https://developer.android.com/reference/android/net/ProxyInfo#getExclusionList()
         */
        exclusion?: string[];
    },
    /**
     * array of active interfaces
     * for Android: it is only 1 active interface with array of ips
     * for iOS: it is array from all available interfaces from next list: ["en", "pdp_ip", "tap", "tun", "ipsec", "ppp", "utun"]
     *
     */
    addresses?: [
        {
            /**
             * ips included ipv4/ipv6
             */
            [interfaceName: string]: Array<[ips: string]>;
        }
    ]
}
