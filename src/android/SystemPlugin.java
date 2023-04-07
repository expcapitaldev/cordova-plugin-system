package com.expcapital.system;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SystemPlugin extends CordovaPlugin {

	private SystemPlugin instance = null;

	private static final String ACTION_SET_TEXT_ZOOM = "setTextZoom";
	private static final String ACTION_OPEN_EMAIL_APP = "openEmailApp";
	private static final String ACTION_GET_AVAILABLE_MAIL_CLIENTS = "getAvailableMailClients";
	private static final String ACTION_OPEN_BROWSER_RESTRICTION_PAGE = "openBrowserRestrictionPage";
	private static final String ACTION_OPEN_SYSTEM_WEB_VIEW = "openSystemWebView";
	private static final String ACTION_START_NETWORK_INFO_NOTIFIER = "startNetworkInfoNotifier";
	private static final String ACTION_STOP_NETWORK_INFO_NOTIFIER = "stopNetworkInfoNotifier";

	private static final String TRANSLATION_TITLE_REPLACE_KEY = "TITLE_TEXT";
	private static final String TRANSLATION_DESCRIPTION_REPLACE_KEY = "DESC_TEXT";
	private static final String TRANSLATION_BUTTON_REPLACE_KEY = "BUTTON_TEXT";

	private static final String RESTRICTION_PAGE_STYLE_FILE_PATH = "www/browser-restriction-page/style.css";
	private static final String RESTRICTION_PAGE_HTML_FILE_PATH = "www/browser-restriction-page/index.html";

	private static String restrictionTitleKeyIdentifier = "restriction_title_key";
	private static String restrictionDescriptionKeyIdentifier = "restriction_description_key";
	private static String restrictionButtonKeyIdentifier = "restriction_button_key";

	protected static final String TAG = "SystemPlugin";
	private ConnectivityManager.NetworkCallback networkCallback;
	private ConnectivityManager sockMan;
	private CallbackContext networkInfoChangedCallbackContext;
//	prevNetworkInfo:
//	{
//		transport: ["WIFI", "VPN"],
//		proxy: {
//			host: "host",
//			port: "port",
//			exclusion: ["ex.com", "ex2.com"]
//		},
//		addresses: [
//			{
//				"interfaceName": ["ip1", "ip2"]
//			}
//		]
//	}
	private JSONObject prevNetworkInfo = new JSONObject();

	@Override
	protected void pluginInitialize() {
		Log.d(TAG, "==> initialize");
		instance = this;
	}

	@Override
	public void onDestroy() {
		instance = null;
		super.onDestroy();
	}

	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);
		unregisterDefaultNetworkCallback();
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		unregisterDefaultNetworkCallback();
		registerDefaultNetworkCallback();
	}

	@Override
	public void onReset() {
		unregisterDefaultNetworkCallback();
		networkInfoChangedCallbackContext = null;
		prevNetworkInfo = new JSONObject();
	}

	@Override
	public boolean execute(String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {

		switch (action) {
			case ACTION_SET_TEXT_ZOOM:
				setTextZoom(args, callbackContext);
				break;
			case ACTION_OPEN_EMAIL_APP:
				openEmailApp(args, callbackContext);
				break;
			case ACTION_GET_AVAILABLE_MAIL_CLIENTS:
				getAvailableMailClients(callbackContext);
				break;
			case ACTION_OPEN_BROWSER_RESTRICTION_PAGE:
				openBrowserRestrictionPage(args, callbackContext);
				break;
			case ACTION_OPEN_SYSTEM_WEB_VIEW:
				openSystemWebView(callbackContext);
				break;
			case ACTION_START_NETWORK_INFO_NOTIFIER:
				startNetworkInfoNotifier(callbackContext);
				break;
			case ACTION_STOP_NETWORK_INFO_NOTIFIER:
				stopNetworkInfoNotifier(callbackContext);
				break;
		}

		return true;

	}

	private void setTextZoom(CordovaArgs args, CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					Integer textZoom = args.getInt(0);

					View mView = webView.getView();
					Method getSettings = mView.getClass().getMethod("getSettings");
					Object wSettings = getSettings.invoke(mView);
					Method getTextZoom = wSettings.getClass().getMethod("getTextZoom");

					Integer actualZoom = Integer.parseInt(getTextZoom.invoke(wSettings).toString());

					if (!actualZoom.equals(textZoom)) {
						Method setTextZoom = wSettings.getClass().getMethod("setTextZoom", Integer.TYPE);
						setTextZoom.invoke(wSettings, textZoom);
					}

					callbackContext.success();
				} catch (Exception e) {
					handleExceptionWithContext(e, callbackContext);
				}
			}
		});

	}

	private void getAvailableMailClients(CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				try {
					Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"));
					PackageManager pm = webView.getContext().getPackageManager();
					List<ResolveInfo> resInfo = pm.queryIntentActivities(emailIntent, 0);
					JSONArray availableMailList = new JSONArray();
					// filter duplicate packageName
					Set<String> packageNameSet = new HashSet<String>();
					for (int i = 0; i < resInfo.size(); i++) {
						ResolveInfo ri = resInfo.get(i);
						String packageName = ri.activityInfo.packageName;
						ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
						String label = pm.getApplicationLabel(info).toString();
						if (!packageNameSet.contains(packageName)) {
							packageNameSet.add(packageName);
							JSONObject obj = new JSONObject();
							obj.put("label", label);
							obj.put("package", packageName);
							availableMailList.put(obj);
						}
					}
					JSONArray sortedJsonArray = sortJSONArray(availableMailList, "label");

					callbackContext.success(sortedJsonArray);
				} catch (Exception e) {
					handleExceptionWithContext(e, callbackContext);
				}
			}
		});
	}

	private void openEmailApp(CordovaArgs args, CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					String packageName = args.getString(0);
					PackageManager manager = cordova.getActivity().getApplicationContext().getPackageManager();
					Intent launchIntent = manager.getLaunchIntentForPackage(packageName);

					if (launchIntent == null) {
						callbackContext.error("application " + packageName + " not found");
						return;
					}
					cordova.getActivity().startActivity(launchIntent);
					callbackContext.success();

				} catch (Exception e) {
					handleExceptionWithContext(e, callbackContext);
				}
			}
		});
	}

	private void openBrowserRestrictionPage(CordovaArgs args, CallbackContext callbackContext) {

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					String language = args.getString(0);
//					String language = "es";

					Resources resources = cordova.getActivity().getResources();
					String packageName = cordova.getActivity().getPackageName();
					Configuration configuration = resources.getConfiguration();

					String styleContent = readFromAssets(cordova.getContext(), RESTRICTION_PAGE_STYLE_FILE_PATH);
					if (styleContent == null) {
						callbackContext.error("style content is empty");
						return;
					}

					int restrictionTitleKeyId = resources.getIdentifier(restrictionTitleKeyIdentifier, "string", packageName);
					int restrictionDescriptionKeyId = resources.getIdentifier(restrictionDescriptionKeyIdentifier, "string", packageName);
					int restrictionButtonKeyId = resources.getIdentifier(restrictionButtonKeyIdentifier, "string", packageName);

					if (restrictionTitleKeyId == 0 || restrictionDescriptionKeyId == 0 || restrictionButtonKeyId == 0) {
						callbackContext.error("text key does not exist");
						return;
					}

					String translationTitleKey = resources.getString(restrictionTitleKeyId);
					String translationDescriptionKey = resources.getString(restrictionDescriptionKeyId);
					String translationButtonKey = resources.getString(restrictionButtonKeyId);

					configuration.locale = new Locale(language);
					DisplayMetrics dm = resources.getDisplayMetrics();
					Resources resources2 = new Resources(resources.getAssets(), dm, configuration);
					int restrictionTitleKeyId2 = resources2.getIdentifier(translationTitleKey, "string", packageName);
					int restrictionDescriptionKeyId2 = resources2.getIdentifier(translationDescriptionKey, "string", packageName);
					int restrictionButtonKeyId2 = resources2.getIdentifier(translationButtonKey, "string", packageName);

					if (restrictionTitleKeyId2 == 0 || restrictionDescriptionKeyId2 == 0 || restrictionButtonKeyId2 == 0) {
						callbackContext.error("translation key does not exist");
						return;
					}

					String titleText = resources2.getString(restrictionTitleKeyId2);
					String descriptionText = resources.getString(restrictionDescriptionKeyId2);
					String buttonText = resources.getString(restrictionButtonKeyId2);

					String pageContent = readFromAssets(cordova.getContext(), RESTRICTION_PAGE_HTML_FILE_PATH);
					if (pageContent == null) {
						callbackContext.error("html content is empty");
						return;
					}
					pageContent = pageContent.replace(TRANSLATION_TITLE_REPLACE_KEY, titleText);
					pageContent = pageContent.replace(TRANSLATION_DESCRIPTION_REPLACE_KEY, descriptionText);
					pageContent = pageContent.replace(TRANSLATION_BUTTON_REPLACE_KEY, buttonText);

					injectStyleCode(styleContent);
					injectHtmlCode(pageContent);

					callbackContext.success();

				} catch (Exception e) {
					handleExceptionWithContext(e, callbackContext);
				}
			}
		});
	}

	private void openSystemWebView(CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW);
//					String url = "market://details?id=com.google.android.webview";
					String url = "https://play.google.com/store/apps/details?id=com.google.android.webview";
					Uri uri = Uri.parse(url);
					intent.setData(uri);
					intent.putExtra(Browser.EXTRA_APPLICATION_ID, cordova.getActivity().getPackageName());
					cordova.getActivity().startActivity(Intent.createChooser(intent, null));
					callbackContext.success();

				} catch (Exception e) {
					handleExceptionWithContext(e, callbackContext);
				}
			}
		});
	}

	private void startNetworkInfoNotifier(CallbackContext callbackContext) {
		try {
			if (networkInfoChangedCallbackContext != null) {
				callbackContext.error("Already started");
				return;
			}
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
				callbackContext.error("Wrong api level");
				return;
			}
			networkInfoChangedCallbackContext = callbackContext;
			registerDefaultNetworkCallback();
		} catch (Exception e) {
			// Log.e(TAG, "===qqq networkInfoChangedCallbackException 1: " + e.toString());
			networkInfoChangedCallbackException(e);
		}
	}

	private void stopNetworkInfoNotifier(CallbackContext callbackContext) {
		try {
			unregisterDefaultNetworkCallback();
			networkInfoChangedCallbackContext = null;
			prevNetworkInfo = new JSONObject();
			callbackContext.success();
		} catch (Exception e) {
			handleExceptionWithContext(e, callbackContext);
		}
	}

	private void registerDefaultNetworkCallback() {
		if (networkInfoChangedCallbackContext == null) {
			return;
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			return;
		}
		this.sockMan = (ConnectivityManager) cordova.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

		this.networkCallback = new ConnectivityManager.NetworkCallback() {
			@Override
			public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
				// Log.e(TAG, "===qqq onCapabilitiesChanged: " + networkCapabilities);
				super.onCapabilitiesChanged(network, networkCapabilities);
				// api level >= 23
				// ArrayList<String> transports = new ArrayList<String>();
				JSONArray transports = new JSONArray();
				if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
					transports.put(getTransportName(NetworkCapabilities.TRANSPORT_CELLULAR));
				}
				if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
					transports.put(getTransportName(NetworkCapabilities.TRANSPORT_WIFI));
				}
				if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
					transports.put(getTransportName(NetworkCapabilities.TRANSPORT_BLUETOOTH));
				}
				if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
					transports.put(getTransportName(NetworkCapabilities.TRANSPORT_ETHERNET));
				}
				if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
					transports.put(getTransportName(NetworkCapabilities.TRANSPORT_VPN));
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
						transports.put(getTransportName(NetworkCapabilities.TRANSPORT_WIFI_AWARE));
					}
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
					if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
						transports.put(getTransportName(NetworkCapabilities.TRANSPORT_LOWPAN));
					}
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)) {
						transports.put(getTransportName(NetworkCapabilities.TRANSPORT_USB));
					}
				}
				// api level >= 31
				// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				// Log.e(TAG, "The default network changed getCapabilities: " +  Arrays.toString(networkCapabilities.getCapabilities()));
				// }
				onNetworkDataChanged(transports);
			}

			@Override
			public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
				// Log.e(TAG, "===qqq onLinkPropertiesChanged: " + linkProperties);
				super.onLinkPropertiesChanged(network, linkProperties);
				onNetworkDataChanged(linkProperties);
			}
		};
		this.sockMan.registerDefaultNetworkCallback(this.networkCallback);
	}

	private void unregisterDefaultNetworkCallback() {
		if (this.sockMan != null && this.networkCallback != null) {
			this.sockMan.unregisterNetworkCallback(this.networkCallback);
			this.sockMan = null;
			this.networkCallback = null;
		}
	}

	private String getTransportName(int transport) {
		switch (transport) {
			case NetworkCapabilities.TRANSPORT_CELLULAR:
				return "CELLULAR";
			case NetworkCapabilities.TRANSPORT_WIFI:
				return "WIFI";
			case NetworkCapabilities.TRANSPORT_BLUETOOTH:
				return "BLUETOOTH";
			case NetworkCapabilities.TRANSPORT_ETHERNET:
				return "ETHERNET";
			case NetworkCapabilities.TRANSPORT_VPN:
				return "VPN";
			case NetworkCapabilities.TRANSPORT_WIFI_AWARE:
				return "WIFI_AWARE";
			case NetworkCapabilities.TRANSPORT_LOWPAN:
				return "LOWPAN";
			case NetworkCapabilities.TRANSPORT_USB:
				return "USB";
		}
		return "UNKNOWN";
	}

	private void onNetworkDataChanged(JSONArray transports) {
		try {
			JSONObject data = new JSONObject();
			data.put("transport", transports);
			networkDataHandler(data);
		} catch (Exception e) {
			// Log.e(TAG, "===qqq networkInfoChangedCallbackException 2: " + e.toString());
			networkInfoChangedCallbackException(e);
		}
	}

	private void onNetworkDataChanged(LinkProperties linkProperties) {
		try {
			List<LinkAddress> addressesList = linkProperties.getLinkAddresses();
			JSONArray addressesArr = new JSONArray();
			for (int i = 0; i < addressesList.size(); i++) {
				LinkAddress address = addressesList.get(i);
				addressesArr.put(address.getAddress().getHostAddress());
			}
			String interfaceName = linkProperties.getInterfaceName();
			JSONObject data = new JSONObject();
			data.put(interfaceName != null ? interfaceName : "unknown", addressesArr);
			networkDataHandler(new JSONObject().put("addresses", new JSONArray().put(data)));
		} catch (Exception e) {
			// Log.e(TAG, "===qqq networkInfoChangedCallbackException 3: " + e.toString());
			networkInfoChangedCallbackException(e);
		}
	}

	private void networkDataHandler(JSONObject networkInfo) {
		// Log.e(TAG, "===qqq networkDataHandler: " + networkInfo.toString());
		try {
			JSONObject changedNetworkInfo = new JSONObject();

			if (networkInfo.has("addresses")) {
				JSONArray changedAddresses = networkInfo.getJSONArray("addresses");
				if (prevNetworkInfo.has("addresses")) {
					JSONArray prevAddresses = prevNetworkInfo.getJSONArray("addresses");
					// array of 1 elem only
					JSONObject changedAddress = changedAddresses.getJSONObject(0);
					JSONObject prevAddress = prevAddresses.getJSONObject(0);
					JSONArray changedAddressKeys = changedAddress.names();
					JSONArray prevAddressKeys = prevAddress.names();

					if (!isEqualsJSONArrayString(changedAddressKeys, prevAddressKeys)) {
						changedNetworkInfo.put("addresses", changedAddresses);
					} else {
						JSONArray changedIps = changedAddress.getJSONArray(changedAddressKeys.getString(0));
						JSONArray prevIps = prevAddress.getJSONArray(prevAddressKeys.getString(0));
						if (!isEqualsJSONArrayString(prevIps, changedIps)) {
							changedNetworkInfo.put("addresses", changedAddresses);
						}
					}

				} else {
					changedNetworkInfo.put("addresses", changedAddresses);
				}
			}

			if (networkInfo.has("transport")) {
				JSONArray changedTransport = networkInfo.getJSONArray("transport");
				if (prevNetworkInfo.has("transport")) {
					JSONArray prevTransport = prevNetworkInfo.getJSONArray("transport");
					if (!isEqualsJSONArrayString(prevTransport, changedTransport)) {
						changedNetworkInfo.put("transport", changedTransport);
					}
				} else {
					changedNetworkInfo.put("transport", changedTransport);
				}
			}
			JSONObject currentProxyInfo = getHttpProxyInformation();
			if (prevNetworkInfo.has("proxy")) {
				JSONObject prevProxyInfo = prevNetworkInfo.getJSONObject("proxy");
				if (!isProxyInfoEquals(prevProxyInfo, currentProxyInfo)) {
					changedNetworkInfo.put("proxy", currentProxyInfo);
				}
			} else {
				if (currentProxyInfo.length() != 0) {
					changedNetworkInfo.put("proxy", currentProxyInfo);
				}
			}

			if (changedNetworkInfo.has("addresses")) {
				prevNetworkInfo.put("addresses", changedNetworkInfo.getJSONArray("addresses"));
			}
			if (changedNetworkInfo.has("transport")) {
				prevNetworkInfo.put("transport", changedNetworkInfo.getJSONArray("transport"));
			}
			if (changedNetworkInfo.has("proxy")) {
				prevNetworkInfo.put("proxy", changedNetworkInfo.getJSONObject("proxy"));
			}
			// Log.e(TAG, "===qqq changedNetworkInfo " +  changedNetworkInfo.toString());

			if (changedNetworkInfo.length() > 0) {
				sendPluginResultAndKeepCallback(changedNetworkInfo, networkInfoChangedCallbackContext);
			}

		} catch (Exception e) {
			// Log.e(TAG, "===qqq networkInfoChangedCallbackException 4: " + e.toString());
			networkInfoChangedCallbackException(e);
		}

	}

	private boolean isProxyInfoEquals(@NonNull JSONObject proxyInfo1, @NonNull JSONObject proxyInfo2) throws JSONException {
//		proxy: {
//			host: "host",
//			port: "port",
//			exclusion: ["ex.com", "ex2.com"]
//		}
		if (proxyInfo1.length() != proxyInfo2.length()) {
			return false;
		}
		if (!proxyInfo1.optString("host").equals(proxyInfo2.optString("host"))) {
			return false;
		}
		if (!proxyInfo1.optString("port").equals(proxyInfo2.optString("port"))) {
			return false;
		}
		return isEqualsJSONArrayString(proxyInfo1.optJSONArray("exclusion"), proxyInfo2.optJSONArray("exclusion"));
	}

	@NonNull
	private JSONObject getHttpProxyInformation() {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && this.sockMan != null) {
				ProxyInfo proxy = this.sockMan.getDefaultProxy();
				if (proxy == null) {
					return new JSONObject();
				}
				// Log.e(TAG, "===qqq ProxyInfo: " + proxy.toString());
				JSONObject data = new JSONObject();
				if (proxy.getHost() != null) {
					data.put("host", proxy.getHost());
				}
				data.put("port", proxy.getPort());
				if (proxy.getExclusionList() != null) {
					data.put("exclusion", new JSONArray(proxy.getExclusionList()));
				}
				return data;
			} else {
				return new JSONObject();
			}
		} catch (Exception e) {
			// Log.e(TAG, "===qqq networkInfoChangedCallbackException 5: " + e.toString());
			networkInfoChangedCallbackException(e);
			return new JSONObject();
		}
	}

	/*
	 * Helper methods
	 */

	private boolean isEqualsJSONArrayString(@Nullable JSONArray arr1, @Nullable JSONArray arr2) throws JSONException {
		if (arr1 == null && arr2 == null) {
			return true;
		}
		if (arr1 == null || arr2 == null) {
			return false;
		}
		Set<String> set1 = new HashSet<String>();
		Set<String> set2 = new HashSet<String>();
		for (int i = 0; i < arr1.length(); i++) {
			set1.add(arr1.getString(i));
		}
		for (int i = 0; i < arr2.length(); i++) {
			set2.add(arr2.getString(i));
		}
		return set1.equals(set2);
	}

	@Nullable
	private String readFromAssets(@NonNull Context context, @NonNull String filepath) {
		try (InputStream is = context.getAssets().open(filepath)) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			final StringBuilder sb = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				sb.append(line);
				line = reader.readLine();
			}
			reader.close();
			return sb.toString();
		} catch (Exception e) {
			handleExceptionWithoutContext(e);
		}
		return null;
	}

	private void injectHtmlCode(@NonNull String code) {
		String jsWrapper = "(function(d) { d.body.insertAdjacentHTML('beforeend', %s); })(document)";
		injectDeferredObject(code, jsWrapper);
	}

	private void injectStyleCode(@NonNull String code) {
		String jsWrapper = "(function(d) { var c = d.createElement('style'); c.innerHTML = %s; d.body.appendChild(c); })(document)";
		injectDeferredObject(code, jsWrapper);
	}

	private void injectDeferredObject(String source, String jsWrapper) {
		org.json.JSONArray jsonEsc = new org.json.JSONArray();
		jsonEsc.put(source);
		String jsonRepr = jsonEsc.toString();
		String jsonSourceString = jsonRepr.substring(1, jsonRepr.length()-1);
		String scriptToInject = String.format(jsWrapper, jsonSourceString);
		this.cordova.getActivity().runOnUiThread(new Runnable() {
			@SuppressLint("NewApi")
			@Override
			public void run() {
				webView.getEngine().evaluateJavascript(scriptToInject, null);
			}
		});
	}

	private JSONArray sortJSONArray(JSONArray jsonArray, String key) throws JSONException {
		JSONArray sortedJsonArray = new JSONArray();
		List list = new ArrayList();
		for(int i = 0; i < jsonArray.length(); i++) {
			list.add(jsonArray.getJSONObject(i));
		}
		Collections.sort(list, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject a, JSONObject b) {
				String str1 = new String();
				String str2 = new String();
				try {
					str1 = (String)a.get(key);
					str2 = (String)b.get(key);
				} catch(JSONException e) {
					e.printStackTrace();
				}
				return str1.compareTo(str2);
			}
		});
		for(int i = 0; i < jsonArray.length(); i++) {
			sortedJsonArray.put(list.get(i));
		}
		return sortedJsonArray;
	}

	private void handleExceptionWithContext(Exception e, CallbackContext context) {
		String msg = e.toString();
		Log.e(TAG, msg);
		context.error(msg);
		logErrorToWebview(msg);
	}

	private void handleExceptionWithoutContext(Exception e){
		String msg = e.toString();
		Log.e(TAG, msg);
		if (instance != null) {
			instance.logErrorToWebview(msg);
		}
	}

	private void networkInfoChangedCallbackException(Exception e) {
		if (networkInfoChangedCallbackContext != null) {
			sendPluginErrorAndKeepCallback(e, networkInfoChangedCallbackContext);
		} else {
			handleExceptionWithoutContext(e);
		}
	}

	private void sendPluginResultAndKeepCallback(JSONObject result, CallbackContext callbackContext) {
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
		sendPluginResultAndKeepCallback(pluginResult, callbackContext);
	}

	private void sendPluginErrorAndKeepCallback(Exception e, CallbackContext callbackContext){
		String result = e.toString();
		PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
		sendPluginResultAndKeepCallback(pluginResult, callbackContext);
	}

//	private void sendPluginErrorAndKeepCallback(String result, CallbackContext callbackContext){
//		PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
//		sendPluginResultAndKeepCallback(pluginResult, callbackContext);
//	}

	private void sendPluginResultAndKeepCallback(PluginResult pluginResult, CallbackContext callbackContext){
		pluginResult.setKeepCallback(true);
		callbackContext.sendPluginResult(pluginResult);
	}

	private void logErrorToWebview(String msg){
		Log.e(TAG, msg);
		executeGlobalJavascript("console.error(\""+TAG+"[native]: "+escapeDoubleQuotes(msg)+"\")");
	}

	private String escapeDoubleQuotes(String string){
		String escapedString = string.replace("\"", "\\\"");
		escapedString = escapedString.replace("%22", "\\%22");
		return escapedString;
	}

	private void executeGlobalJavascript(final String jsString){
		if(instance == null) return;
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				webView.loadUrl("javascript:" + jsString);
//				webView.getEngine().evaluateJavascript(jsString, new ValueCallback<String>() {
//					@Override
//					public void onReceiveValue(String s) {
//						Log.d("===onReceiveValue", s);
//						// no op
//					}
//				});
			}
		});
	}

}
