
package com.android.lib.networkmonitor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by prateek on 11/13/15.
 */
public final class NetworkMonitor {

    private enum ConnectionType {
        CONNECTION_NO_NETWORK, CONNECTION_WIFI, CONNECTION_MOBILE_DATA
    }

    private static final String TAG = "NetworkApp";
    private static List<NetworkChangeListener> listeners;
    private static Activity currentlyAttachedActivity;
    private static Context context;
    private static List<Fragment> currentlyAttachedFragments;
    private static ConnectionType currentConnectionType;
    private static boolean initialized;

    private static BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Network connectivity change");
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                ConnectionType newType = getConnectionType(context);
                // check if the connection type changed and the internet connectivity changed
                if (newType != currentConnectionType
                        && isConnectionTypeInternet(newType) != isConnectionTypeInternet(currentConnectionType)) {
                    Log.d(TAG, "Network connectivity change: " + currentConnectionType.toString()
                            + "   " + newType.toString());
                    currentConnectionType = newType;
                    notifyListeners(isConnectionTypeInternet(currentConnectionType));
                    addRemoveConnectionOverlay();
                }
                currentConnectionType = newType;
            }
        }
    };

    /**
     * Initialize the Network Monitor. This should be the first function called
     *
     * @param ctx
     */
    public static void initialize(Context ctx) {
        if (!initialized) {
            if (ctx == null) {
                throw new IllegalArgumentException(
                        "Context cannot be null. NetworkMonitor cannot be initialized");
            }
            listeners = new ArrayList<>();
            currentlyAttachedFragments = new ArrayList<>();
            context = ctx.getApplicationContext();
            currentConnectionType = getConnectionType(context);
            context.registerReceiver(mConnReceiver, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
            initialized = true;
        } else {
            Log.d(TAG, "Trying to initialize the already initialized network monitor");
        }
    }

    /**
     * function to check if the network monitor has been initialized
     */
    private static void checkIfInitialized() {
        if (!initialized) {
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
        currentlyAttachedActivity = act;
        if (!isConnectionTypeInternet(currentConnectionType)) {
            addRemoveConnectionOverlay(currentlyAttachedActivity, true);
        }
    }

    public static void detachActivity(Activity act) {
        checkIfInitialized();

        if (act == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null. Cannot detachActivity a null Activity");
        }
        addRemoveConnectionOverlay(currentlyAttachedActivity, false);
        resetActivity();
    }

    private static void resetActivity() {
        currentlyAttachedActivity = null;
    }

    public static void attachFragment(Fragment fragment) {
        checkIfInitialized();

        if (fragment == null) {
            throw new IllegalArgumentException(
                    "Fragment cannot be null. Cannot Attach a null Fragment");
        }
        addFragment(fragment);
        if (!isConnectionTypeInternet(currentConnectionType)) {
            addRemoveConnectionOverlay(fragment, true);
        }
    }

    public static void detachFragment(Fragment fragment) {
        checkIfInitialized();

        if (fragment == null) {
            throw new IllegalArgumentException(
                    "Fragment cannot be null. Cannot detachActivity a null Fragment");
        }
        addRemoveConnectionOverlay(fragment, false);
        removeFragment(fragment);
    }

    /**
     * remove the fragment from the list
     * 
     * @param fragment
     */
    private static void removeFragment(Fragment fragment) {
        currentlyAttachedFragments.remove(fragment);
    }

    /**
     * add the fragment to the list
     * 
     * @param fragment
     */
    private static void addFragment(Fragment fragment) {
        currentlyAttachedFragments.add(fragment);
    }

    private static void addRemoveConnectionOverlay(Fragment fragment, boolean addOverlay) {
        if (fragment != null) {
            if (fragment.isAdded() && !fragment.isRemoving() && fragment.getView() != null) {
                ViewGroup viewGroup = (ViewGroup) fragment.getView();
                addRemoveConnectionOverlay(viewGroup, addOverlay, true, fragment.getContext());
            } else {
                removeFragment(fragment);
            }
        }
    }

    private static void addRemoveConnectionOverlay(Activity activity, boolean addOverlay) {
        if (activity != null) {
            if (!activity.isFinishing()) {
                final ViewGroup viewGroup =
                        (ViewGroup) currentlyAttachedActivity.findViewById(android.R.id.content);
                addRemoveConnectionOverlay(viewGroup, addOverlay, false, activity);
            } else {
                resetActivity();
            }
        }
    }

    private static void addRemoveConnectionOverlay(ViewGroup viewGroup, boolean addOverlay,
            boolean fragment, Context context) {
        View overlay =
                viewGroup.findViewById(fragment ? R.id.overlay_id_fragment
                        : R.id.overlay_id_activity);

        if (!addOverlay) {
            // remove Overlay view if there
            if (overlay != null) {
                viewGroup.removeView(overlay);
            }
        } else {
            // add Overlay View if not there
            if (overlay == null) {
                TextView textView = new TextView(context);
                textView.setId(fragment ? R.id.overlay_id_fragment : R.id.overlay_id_activity);
                textView.setTextColor(ContextCompat.getColor(context, R.color.overlay_text_color));
                textView.setBackgroundColor(ContextCompat.getColor(context,
                        R.color.overlay_background));
                FrameLayout.LayoutParams lp =
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT);
                int padding =
                        context.getResources().getDimensionPixelOffset(R.dimen.overlay_padding);
                textView.setPadding(padding, padding, padding, padding);
                textView.setLayoutParams(lp);
                textView.setText(context.getResources().getString(R.string.no_network_overlay_mssg));
                viewGroup.addView(textView, viewGroup.getChildCount());
            }
        }
    }

    /**
     * function to go though the list of attached fragments and Activity and add or remove the
     * overlay based on network state.
     */
    private static void addRemoveConnectionOverlay() {
        final boolean addOverlay = !isConnectionTypeInternet(currentConnectionType);
        for (Fragment fragment : currentlyAttachedFragments) {
            addRemoveConnectionOverlay(fragment, addOverlay);
        }
        addRemoveConnectionOverlay(currentlyAttachedActivity, addOverlay);
    }

    /**
     * add a networkListener to listen for the connection change callbacks
     * 
     * @param networkChangeListener
     */
    public static void addOnNetworkChangeListener(NetworkChangeListener networkChangeListener) {
        checkIfInitialized();
        if (networkChangeListener != null) {
            listeners.add(networkChangeListener);
        } else {
            Log.e(TAG, "Trying to add a null NetworkChangeListener");
        }
    }

    /**
     * remove the listener
     * 
     * @param networkChangeListener
     */
    public static void removeNetworkChangeListener(NetworkChangeListener networkChangeListener) {
        checkIfInitialized();
        listeners.remove(networkChangeListener);
    }

    /**
     * Notify the listeners about the change in the network connection
     * 
     * @param connected
     */
    public static void notifyListeners(boolean connected) {

        for (NetworkChangeListener listener : listeners) {
            listener.onConnectivityChanged(connected);
        }
    }

    /**
     * @param context
     * @return the current connection type based on the currently Active networkInfo
     */
    private static ConnectionType getConnectionType(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getApplicationContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();

        if (netInfo != null) {
            Log.d(TAG, netInfo.toString());
        }
        if (netInfo == null || !netInfo.isAvailable() || !netInfo.isConnected()) {
            return ConnectionType.CONNECTION_NO_NETWORK;
        } else {
            if ((netInfo.getType() == ConnectivityManager.TYPE_WIFI)
                    || (netInfo.getType() == ConnectivityManager.TYPE_WIMAX)) {
                return ConnectionType.CONNECTION_WIFI;
            }
            if ((netInfo.getType() == ConnectivityManager.TYPE_MOBILE)
                    || (netInfo.getType() == ConnectivityManager.TYPE_MOBILE_DUN)
                    || (netInfo.getType() == ConnectivityManager.TYPE_MOBILE_HIPRI)
                    || (netInfo.getType() == ConnectivityManager.TYPE_MOBILE_SUPL)
                    || (netInfo.getType() == ConnectivityManager.TYPE_MOBILE_MMS)) {
                return ConnectionType.CONNECTION_MOBILE_DATA;
            }
        }

        return ConnectionType.CONNECTION_NO_NETWORK;

    }

    /**
     * Check if the the connection type belongs to the category of NO_NETWORK or
     * NETWORK(WIFI,MOBILE)
     * 
     * @param type
     * @return
     */
    private static boolean isConnectionTypeInternet(ConnectionType type) {
        return (type == ConnectionType.CONNECTION_MOBILE_DATA || type == ConnectionType.CONNECTION_WIFI);
    }

    /**
     * Interface which client can use to get updates about connection change in case they want to do
     * any processing
     */
    public interface NetworkChangeListener {
        public void onConnectivityChanged(boolean connected);
    }

}
