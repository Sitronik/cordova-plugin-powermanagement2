/*
 * Copyright 2013-2014 Wolfgang Koller
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Cordova (Android) plugin for accessing the power-management functions of the device
 * @author Wolfgang Koller <viras@users.sourceforge.net>
 */
package org.apache.cordova.powermanagement;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.os.PowerManager;
import android.os.Build;
import android.os.Handler;
import android.app.PendingIntent;
import android.content.Intent;
import java.lang.Runnable;
import android.view.View;

import android.util.Log;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;

/**
 * Plugin class which does the actual handling
 */
public class PowerManagement extends CordovaPlugin {
	// As we only allow one wake-lock, we keep a reference to it here
	private PowerManager.WakeLock wakeLock = null;
	private PowerManager powerManager = null;
	private boolean releaseOnPause = true;

	private Handler handler;
	private PendingIntent wakeupIntent;
	private CordovaWebView webView;

	private final Runnable heartbeat = new Runnable() {
		public void run() {
			try {
				//Log.d("PowerManagementPlugin", "About to declare ourselves VISIBLE");
				webView.getView().dispatchWindowVisibilityChanged(View.VISIBLE);

				// if sdk is 23 (android 6) or greater
				if(android.os.Build.VERSION.SDK_INT > 22){

					if (wakeLock != null && powerManager != null && powerManager.isDeviceIdleMode()) {
						//Log.d("PowerManagementPlugin", "Poking location service");
						try {
							wakeupIntent.send();
						} catch (SecurityException e) {
							Log.d("PowerManagementPlugin", "SecurityException : Heartbeat location manager keep-alive failed");
						} catch (PendingIntent.CanceledException e) {
							Log.d("PowerManagementPlugin", "PendingIntent.CanceledException : Heartbeat location manager keep-alive failed");
						}
					}

				}

			} finally {
				if (handler != null) {
					handler.postDelayed(this, 10000);
				}
			}
		}
	};

	/**
	 * Fetch a reference to the power-service when the plugin is initialized
	 */
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webViewPara) {

		Context context = cordova.getActivity().getApplicationContext();

		this.webView = webViewPara;

		super.initialize(cordova, webViewPara);

		this.powerManager = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);

		handler = new Handler();
		wakeupIntent = PendingIntent.getBroadcast( context , 0,
				new Intent("com.android.internal.location.ALARM_WAKEUP"), 0);

	}

	public PluginResult partialWakeLock() {
		Log.d("PowerManagementPlugin", "Partial wake lock" );
		PluginResult result = this.acquire( PowerManager.PARTIAL_WAKE_LOCK );
		handler.postDelayed(heartbeat, 10000);
		return result;
	}

	@Override
	public boolean execute(String action, JSONArray args,
						   CallbackContext callbackContext) throws JSONException {

		PluginResult result = null;
		Log.d("PowerManagementPlugin", "Plugin execute called - " + this.toString() );
		Log.d("PowerManagementPlugin", "Action is " + action );

		try {
			if( action.equals("acquire") ) {
				if( args.length() > 0 && args.getBoolean(0) ) {
					result = partialWakeLock();
				} else {
					result = partialWakeLock();
				}
			} else if( action.equals("release") ) {
				result = this.release();
			} else if( action.equals("setReleaseOnPause") ) {
				try {
					this.releaseOnPause = args.getBoolean(0);
					result = new PluginResult(PluginResult.Status.OK);
				} catch (Exception e) {
					result = new PluginResult(PluginResult.Status.ERROR, "Could not set releaseOnPause");
				}
			}
		}
		catch( JSONException e ) {
			result = new PluginResult(Status.JSON_EXCEPTION, e.getMessage());
		}

		callbackContext.sendPluginResult(result);
		return true;
	}

	/**
	 * Acquire a wake-lock
	 * @param p_flags Type of wake-lock to acquire
	 * @return PluginResult containing the status of the acquire process
	 */
	private PluginResult acquire( int p_flags ) {
		PluginResult result = null;

		if (this.wakeLock == null) {
			this.wakeLock = this.powerManager.newWakeLock(p_flags, "PowerManagementPlugin");
			try {
				this.wakeLock.acquire();
				result = new PluginResult(PluginResult.Status.OK);
			}
			catch( Exception e ) {
				this.wakeLock = null;
				result = new PluginResult(PluginResult.Status.ERROR,"Can't acquire wake-lock - check your permissions!");
			}
		}
		else {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION,"WakeLock already active - release first");
		}

		return result;
	}

	/**
	 * Release an active wake-lock
	 * @return PluginResult containing the status of the release process
	 */
	private PluginResult release() {
		PluginResult result = null;

		if( this.wakeLock != null ) {
			try {
				this.wakeLock.release();
				result = new PluginResult(PluginResult.Status.OK, "OK");
			}
			catch (Exception e) {
				result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "WakeLock already released");
			}

			this.wakeLock = null;
		}
		else {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "No WakeLock active - acquire first");
		}

		return result;
	}

	/**
	 * Make sure any wakelock is released if the app goes into pause
	 */
	@Override
	public void onPause(boolean multitasking) {
		if( this.releaseOnPause && this.wakeLock != null ) {
			this.wakeLock.release();
		}

		super.onPause(multitasking);
	}

	/**
	 * Make sure any wakelock is acquired again once we resume
	 */
	@Override
	public void onResume(boolean multitasking) {
		if( this.releaseOnPause && this.wakeLock != null ) {
			this.wakeLock.acquire();
		}

		super.onResume(multitasking);
	}
}
