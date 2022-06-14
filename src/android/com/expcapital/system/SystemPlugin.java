package com.expcapital.system;


import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SystemPlugin extends CordovaPlugin {

	protected static SystemPlugin instance = null;

	private static final String ACTION_SET_TEXT_ZOOM = "setTextZoom";
	private static final String ACTION_OPEN_EMAIL_APP = "openEmailApp";
	private static final String ACTION_GET_AVAILABLE_MAIL_CLIENTS = "getAvailableMailClients";

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

		if (ACTION_SET_TEXT_ZOOM.equals(action)) {
			setTextZoom(args, callbackContext);
		} else if (ACTION_OPEN_EMAIL_APP.equals(action)) {
			openEmailApp(args, callbackContext);
		} else if (ACTION_GET_AVAILABLE_MAIL_CLIENTS.equals(action)) {
			getAvailableMailClients(callbackContext);
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
							obj.put("packageName", packageName);
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

//	@Override
//	public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
//		super.onRestoreStateForActivityResult(state, callbackContext);
//		this.callbackContext = callbackContext;
//	}

	/*
	 * Helper methods
	 */

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
