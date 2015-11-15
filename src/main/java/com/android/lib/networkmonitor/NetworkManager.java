
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
 * Created by prateek on 11/14/15.
 */
public final class NetworkManager {

    private enum ConnectionType {
        CONNECTION_NO_NETWORK, CONNECTION_WIFI, CONNECTION_MOBILE_DATA
    }

    private static final String TAG = "NetworkApp";
    private List<NetworkChangeListener> listeners;
    private Activity currentlyAttachedActivity;
    private Context context;
    private List<Fragment> currentlyAttachedFragments;
    private ConnectionType currentConnectionType;
    private static NetworkManager Instance;

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

    /**
     * private constructor
     * 
     * @param ctx
     */
    private NetworkManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        context.registerReceiver(mConnReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        listeners = new ArrayList<>();
        currentlyAttachedFragments = new ArrayList<>();
        currentConnectionType = getConnectionType();
    }

    /**
     * @param ctx
     * @return
     */
    protected static synchronized NetworkManager getInstance(Context ctx) {
        if (Instance == null) {
            Instance = new NetworkManager(ctx);
        }
        return Instance;
    }

    /**
     * remove the fragment from the list
     *
     * @param fragment
     */
    protected void removeFragment(Fragment fragment) {
        currentlyAttachedFragments.remove(fragment);
    }

    /**
     * add the fragment to the list
     *
     * @param fragment
     */
    protected void addFragment(Fragment fragment) {
        currentlyAttachedFragments.add(fragment);
    }

    /**
     * Add or Remove connection Overlay for a fragment
     * 
     * @param fragment
     * @param addOverlay
     */
    protected void addRemoveConnectionOverlay(Fragment fragment, boolean addOverlay) {
        if (fragment != null) {
            if (fragment.isAdded() && !fragment.isRemoving() && fragment.getView() != null) {
                ViewGroup viewGroup = (ViewGroup) fragment.getView();
                addRemoveConnectionOverlay(viewGroup, addOverlay, true, fragment.getContext());
            } else {
                removeFragment(fragment);
            }
        }
    }

    /**
     * Add or Remove connectionOverlay for an activity
     * 
     * @param activity
     * @param addOverlay
     */
    protected void addRemoveConnectionOverlay(Activity activity, boolean addOverlay) {
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

    /**
     * reset the current activity
     */
    protected void resetActivity() {
        currentlyAttachedActivity = null;
    }

    protected void addRemoveConnectionOverlay(ViewGroup viewGroup, boolean addOverlay,
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
    protected void addRemoveConnectionOverlay() {
        final boolean addOverlay = !isCurrentConnectionTypeInternet();
        for (Fragment fragment : currentlyAttachedFragments) {
            addRemoveConnectionOverlay(fragment, addOverlay);
        }
        addRemoveConnectionOverlay(currentlyAttachedActivity, addOverlay);
    }

    /**
     * Notify the listeners about the change in the network connection
     *
     * @param connected
     */
    protected void notifyListeners(boolean connected) {

        for (NetworkChangeListener listener : listeners) {
            listener.onConnectivityChanged(connected);
        }
    }

    /**
     * @return the current connection type based on the currently Active networkInfo
     */
    protected ConnectionType getConnectionType() {
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
    protected boolean isConnectionTypeInternet(ConnectionType type) {
        return (type == ConnectionType.CONNECTION_MOBILE_DATA || type == ConnectionType.CONNECTION_WIFI);
    }

    protected boolean isCurrentConnectionTypeInternet() {
        return isConnectionTypeInternet(currentConnectionType);
    }

    protected Activity getCurrentlyAttachedActivity() {
        return currentlyAttachedActivity;
    }

    protected void setCurrentlyAttachedActivity(Activity currentlyAttachedActivity) {
        this.currentlyAttachedActivity = currentlyAttachedActivity;
    }

    /**
     * Add the listener
     * 
     * @param listener
     */
    protected void addListener(NetworkChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the listener
     * 
     * @param listener
     */
    protected void removeListener(NetworkChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Interface which client can use to get updates about connection change in case they want to do
     * any processing
     */
    public interface NetworkChangeListener {
        public void onConnectivityChanged(boolean connected);
    }

}
