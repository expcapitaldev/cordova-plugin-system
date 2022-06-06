package com.expcapital.system;


import android.util.Log;
import android.view.View;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;

import java.lang.reflect.Method;

public class SystemPlugin extends CordovaPlugin {

	protected static SystemPlugin instance = null;

	private static final String ACTION_SET_TEXT_ZOOM = "setTextZoom";

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
