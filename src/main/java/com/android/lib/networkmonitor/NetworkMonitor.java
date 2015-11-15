
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
    private List<NetworkChangeListener> listeners;
    private Activity currentlyAttachedActivity;
    private Context context;
    private List<Fragment> currentlyAttachedFragments;
    private ConnectionType currentConnectionType;

    private static NetworkMonitor Instance;

    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Network connectivity change");
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                ConnectionType newType = Instance.getConnectionType();
                // check if the connection type changed and the internet connectivity changed
                if (newType != currentConnectionType
                        && isConnectionTypeInternet(newType) != isCurrentConnectionTypeInternet()) {
                    Log.d(TAG, "Network connectivity change: " + currentConnectionType.toString()
                            + "   " + newType.toString());
                    currentConnectionType = newType;
                    notifyListeners(isCurrentConnectionTypeInternet());
                    addRemoveConnectionOverlay();
                }
                currentConnectionType = newType;
            }
        }
    };

    private NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        context.registerReceiver(mConnReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        listeners = new ArrayList<>();
        currentlyAttachedFragments = new ArrayList<>();
        currentConnectionType = getConnectionType();
    }

    /**
     * Initialize the Network Monitor. This should be the first function called
     *
     * @param ctx
     */
    public static synchronized void initialize(Context ctx) {
        if (Instance == null) {
            if (ctx == null) {
                throw new IllegalArgumentException(
                        "Context cannot be null. NetworkMonitor cannot be initialized");
            }
            Instance = new NetworkMonitor(ctx);
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
        Instance.currentlyAttachedActivity = act;
        // currentlyAttachedActivity = act;
        if (!Instance.isCurrentConnectionTypeInternet()) {
            Instance.addRemoveConnectionOverlay(Instance.currentlyAttachedActivity, true);
        }
    }

    public static void detachActivity(Activity act) {
        checkIfInitialized();

        if (act == null) {
            throw new IllegalArgumentException(
                    "Activity cannot be null. Cannot detachActivity a null Activity");
        }
        Instance.addRemoveConnectionOverlay(Instance.currentlyAttachedActivity, false);
        Instance.resetActivity();
    }

    private void resetActivity() {
        Instance.currentlyAttachedActivity = null;
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
     * remove the fragment from the list
     * 
     * @param fragment
     */
    private void removeFragment(Fragment fragment) {
        currentlyAttachedFragments.remove(fragment);
    }

    /**
     * add the fragment to the list
     * 
     * @param fragment
     */
    private void addFragment(Fragment fragment) {
        currentlyAttachedFragments.add(fragment);
    }

    private void addRemoveConnectionOverlay(Fragment fragment, boolean addOverlay) {
        if (fragment != null) {
            if (fragment.isAdded() && !fragment.isRemoving() && fragment.getView() != null) {
                ViewGroup viewGroup = (ViewGroup) fragment.getView();
                addRemoveConnectionOverlay(viewGroup, addOverlay, true, fragment.getContext());
            } else {
                removeFragment(fragment);
            }
        }
    }

    private void addRemoveConnectionOverlay(Activity activity, boolean addOverlay) {
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

    private void addRemoveConnectionOverlay(ViewGroup viewGroup, boolean addOverlay,
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
    private void addRemoveConnectionOverlay() {
        final boolean addOverlay = !isCurrentConnectionTypeInternet();
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
            Instance.listeners.add(networkChangeListener);
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
        Instance.listeners.remove(networkChangeListener);
    }

    /**
     * Notify the listeners about the change in the network connection
     * 
     * @param connected
     */
    private void notifyListeners(boolean connected) {

        for (NetworkChangeListener listener : listeners) {
            listener.onConnectivityChanged(connected);
        }
    }

    /**
     * @return the current connection type based on the currently Active networkInfo
     */
    private ConnectionType getConnectionType() {
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
     * @return
     */
    private boolean isConnectionTypeInternet(ConnectionType type) {
        return (type == ConnectionType.CONNECTION_MOBILE_DATA || type == ConnectionType.CONNECTION_WIFI);
    }

    private boolean isCurrentConnectionTypeInternet() {
        return isConnectionTypeInternet(Instance.currentConnectionType);
    }

    /**
     * Interface which client can use to get updates about connection change in case they want to do
     * any processing
     */
    public interface NetworkChangeListener {
        public void onConnectivityChanged(boolean connected);
    }

}