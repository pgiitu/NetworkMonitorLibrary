# NetworkMonitorLibrary


An Android Library which helps application in monitoring changes to the network connectivity.

Features:

1. It adds an overlay showing "no network message" on all the fragments and the currently attached activity if the network goes off. Removes the overlay once the network comes back on.
2. It allows the application to attach multiple fragments.
3. Only allows the application to attach a single activity
4. Also allows the application to register for NetworkConnectivity changes with an interface so that in the callback the application can do whatever it wants whenever the connectivity changes.


Usage:

Add `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` to the manifest file.


The first call to the Library must be 

`NetworkMonitor.initialize(context);`

Here is a sample activity showing the use:

    public class HomeActivity extends AppCompatActivity {

    NetworkMonitor.NetworkChangeListener listener = new NetworkMonitor.NetworkChangeListener() {
        @Override
        public void onConnectivityChanged(boolean connected) {
            Toast.makeText(HomeActivity.this, "OnConnectivityChanged", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        //Initialize the NetworkMonitor
        NetworkMonitor.initialize(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Attaching the activity
        NetworkMonitor.attachActivity(this);
        //adding the Network change listener
        NetworkMonitor.addOnNetworkChangeListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Detach the activity in onResume
        NetworkMonitor.detachActivity(this);
        //Remove the NetworkChange listener
        NetworkMonitor.removeNetworkChangeListener(listener);
    }

    }
    
Note:
    
1. Since right now the library can be attached with a single activity you should attch the activity in the `onResume()` and detach it in `onPause()`.
2. The library as of now simply adds a TextView with the message "Network not available. Looking for network..." in the root view of the activity and fragment irrespective of the type of ViewGroup currently inflated in the fragment.

