package com.expcapital.system;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Browser;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
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

	protected static SystemPlugin instance = null;

	private static final String ACTION_SET_TEXT_ZOOM = "setTextZoom";
	private static final String ACTION_OPEN_EMAIL_APP = "openEmailApp";
	private static final String ACTION_GET_AVAILABLE_MAIL_CLIENTS = "getAvailableMailClients";
	private static final String ACTION_OPEN_BROWSER_RESTRICTION_PAGE = "openBrowserRestrictionPage";
	private static final String ACTION_OPEN_SYSTEM_WEB_VIEW = "openSystemWebView";

	private static final String TRANSLATION_TITLE_REPLACE_KEY = "TITLE_TEXT";
	private static final String TRANSLATION_DESCRIPTION_REPLACE_KEY = "DESC_TEXT";
	private static final String TRANSLATION_BUTTON_REPLACE_KEY = "BUTTON_TEXT";

	private static final String RESTRICTION_PAGE_STYLE_FILE_PATH = "www/browser-restriction-page/style.css";
	private static final String RESTRICTION_PAGE_HTML_FILE_PATH = "www/browser-restriction-page/index.html";

	private static String restrictionTitleKeyIdentifier = "restriction_title_key";
	private static String restrictionDescriptionKeyIdentifier = "restriction_description_key";
	private static String restrictionButtonKeyIdentifier = "restriction_button_key";

	protected static final String TAG = "SystemPlugin";

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
					Resources resources2 = new Resources(resources.getAssets(), new DisplayMetrics(), configuration);
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


//	@Override
//	public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
//		super.onRestoreStateForActivityResult(state, callbackContext);
//		this.callbackContext = callbackContext;
//	}

	/*
	 * Helper methods
	 */

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

	private JSONArray sortJSONArray(JSONArray jsonArray, String key) throws JSONException
	{
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
