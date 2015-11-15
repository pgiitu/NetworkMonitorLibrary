
package com.android.lib.networkmonitor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import java.util.List;

/**
 * Created by prateek on 11/13/15.
 */
public final class NetworkMonitor {

    private static final String TAG = "NetworkApp";
    private static NetworkManager Instance;

    /**
     * Initialize the Network Monitor. This should be the first function called
     *
     * @param ctx
     */
    public static synchronized void initialize(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException(
                    "Context cannot be null. NetworkMonitor cannot be initialized");
        }
        if (Instance == null) {
            Instance = NetworkManager.getInstance(ctx);
        } else {
            Log.d(TAG, "Trying to initialize the already initialized network monitor");
        }
    }

    /**
     * function to check if the network monitor has been initialized
     */
    private static void checkIfInitialized() {
        if (Instance == null) {
            throw new IllegalStateException(
                    "NetworkMonitor has not been initialized yet. Please call initialize first");
        }
    }

    /**
     * Function to attach Activity the Network Monitor with the Activity
     *
     * @param act
     */
    public static void attachActivity(Activity act) {
        checkIfInitialized();

        if (act == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null. Cannot Attach a null Activity");
        }

        Instance.setCurrentlyAttachedActivity(act);
        if (!Instance.isCurrentConnectionTypeInternet()) {
            Instance.addRemoveConnectionOverlay(Instance.getCurrentlyAttachedActivity(), true);
        }
    }

    public static void detachActivity(Activity act) {
        checkIfInitialized();

        if (act == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null. Cannot detachActivity a null Activity");
        }
        Instance.addRemoveConnectionOverlay(Instance.getCurrentlyAttachedActivity(), false);
        Instance.resetActivity();
    }

    public static void attachFragment(Fragment fragment) {
        checkIfInitialized();

        if (fragment == null) {
            throw new IllegalArgumentException(
                    "Fragment cannot be null. Cannot Attach a null Fragment");
        }
        Instance.addFragment(fragment);
        if (!Instance.isCurrentConnectionTypeInternet()) {
            Instance.addRemoveConnectionOverlay(fragment, true);
        }
    }

    public static void detachFragment(Fragment fragment) {
        checkIfInitialized();

        if (fragment == null) {
            throw new IllegalArgumentException(
                    "Fragment cannot be null. Cannot detachActivity a null Fragment");
        }
        Instance.addRemoveConnectionOverlay(fragment, false);
        Instance.removeFragment(fragment);
    }

    /**
     * add a networkListener to listen for the connection change callbacks
     * 
     * @param networkChangeListener
     */
    public static void addOnNetworkChangeListener(
            NetworkManager.NetworkChangeListener networkChangeListener) {
        checkIfInitialized();
        if (networkChangeListener != null) {
            Instance.addListener(networkChangeListener);
        } else {
            Log.e(TAG, "Trying to add a null NetworkChangeListener");
        }
    }

    /**
     * remove the listener
     * 
     * @param networkChangeListener
     */
    public static void removeNetworkChangeListener(
            NetworkManager.NetworkChangeListener networkChangeListener) {
        checkIfInitialized();
        Instance.removeListener(networkChangeListener);
    }

}
