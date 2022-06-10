package com.expcapital.system;


import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SystemPlugin extends CordovaPlugin {

	protected static SystemPlugin instance = null;

	private static final String ACTION_SET_TEXT_ZOOM = "setTextZoom";
	private static final String ACTION_OPEN_EMAIL_APP = "openEmailApp";

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
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setTextZoom(args, callbackContext);
				}
			});
		} else if (ACTION_OPEN_EMAIL_APP.equals(action)) {
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					openEmailApp(callbackContext);
				}
			});
		}

		return true;

	}

	private void setTextZoom(CordovaArgs args, CallbackContext callbackContext) {

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

	private void openEmailApp(CallbackContext callbackContext) {

		try {
//			Intent draft = new Intent(Intent.ACTION_VIEW, Uri.fromParts("mailto", "", null));
//			draft.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			cordova.getActivity().startActivity(Intent.createChooser(draft, null));
//			callbackContext.success();


			Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"));
			PackageManager pm = webView.getContext().getPackageManager();

			List<ResolveInfo> resInfo = pm.queryIntentActivities(emailIntent, 0);
			if (resInfo.size() > 0) {
				ResolveInfo ri = resInfo.get(0);
				// First create an intent with only the package name of the first registered email app
				// and build a picked based on it
				Intent intentChooser = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
				Intent openInChooser = Intent.createChooser(intentChooser, null);

				// Then create a list of LabeledIntent for the rest of the registered email apps
				List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();
				for (int i = 1; i < resInfo.size(); i++) {
					// Extract the label and repackage it in a LabeledIntent
					ri = resInfo.get(i);
					String packageName = ri.activityInfo.packageName;
					Intent intent = pm.getLaunchIntentForPackage(packageName);
					intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
				}

				LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
				// Add the rest of the email apps to the picker selection
				openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
				cordova.getActivity().startActivity(openInChooser);
				callbackContext.success();
			} else {
				callbackContext.error("no email app found");
			}


		} catch (Exception e) {
			handleExceptionWithContext(e, callbackContext);
		}

	}

//	@Override
//	public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
//		super.onRestoreStateForActivityResult(state, callbackContext);
//		this.callbackContext = callbackContext;
//	}

	/*
	 * Helper methods
	 */

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
