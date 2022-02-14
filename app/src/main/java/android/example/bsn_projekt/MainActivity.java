package android.example.bsn_projekt;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import bsn_projekt.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements BluetoothDevicesDialogFragment.DeviceDialogListener, UserInputFragment.ParametersInterface {
    private static final String TAG = "MainActivity";

    // SPEECH TO TEXT
    // protected static final int RESULT_SPEECH = 1;
    // private EditText text_to_send;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;

    // Member Fields
    private ProgressBar mProgressBar;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService;
    private DeviceViewModel mDeviceViewModel;
    private StringBuffer mOutStringBuffer;
    private String mConnectedDevice = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SPEECH TO TEXT
//        text_to_send = findViewById(R.id.text_to_send);
//        Button click_to_speak_button = findViewById(R.id.click_to_speak_button);
//        try {
//            click_to_speak_button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
//                    try {
//                        startActivityForResult(intent, RESULT_SPEECH);
//                        text_to_send.setText("");
//                    } catch (ActivityNotFoundException e) {
//                        Toast.makeText(getApplicationContext(), "Dein Gerät hat kein Speech-To-Text", Toast.LENGTH_SHORT).show();
//                        e.printStackTrace();
//                    }
//                }
//            });
//        } catch(NullPointerException ignored) { }

        ActionBar mToolBar = getSupportActionBar();

        // Setup Listener for bottom navigation bar
        // Layout Views
        BottomNavigationView mBottomNav = findViewById(R.id.bottom_navigation);
        mProgressBar = findViewById(R.id.loading_bar);
        mBottomNav.setOnNavigationItemSelectedListener(navListener);
        // Open the Spectrometer Settings Fragment first
        mBottomNav.setSelectedItemId(R.id.nav_user_input);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new UserInputFragment()).commit();
        // Set Toolbar title
        assert mToolBar != null;
        mToolBar.setTitle(R.string.toolbar_title);

        // Get the local bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device.", Toast.LENGTH_LONG).show();
            this.finish();
        }

        mDeviceViewModel = ViewModelProviders.of(this).get(DeviceViewModel.class);
    }

    // SPEECH TO TEXT
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == RESULT_SPEECH) {
//            if (resultCode == RESULT_OK && data != null) {
//                ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                text_to_send.setText(text.get(0));
//            }
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();
        // If Bluetooth is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mBluetoothService == null) {
            setupCommunication();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    private void setupCommunication() {
        Log.d(TAG, "SetupCommunication()");

        // Initialize the BluetoothService to perform bluetooth connections
        mBluetoothService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        // Connect to device if it's selected
        if (mDeviceViewModel.getSelected() != null &&
                mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            mBluetoothService.connect(mDeviceViewModel.getSelected(), false);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            UserInputFragment parFrag = (UserInputFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_user_input);
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            mDeviceViewModel.setConnected(true);
                            makeToast("Jetzt verbunden mit: " + mDeviceViewModel.getSelected().getName());
                            mProgressBar.setVisibility(View.INVISIBLE);
                            updateFragment();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            mProgressBar.setVisibility(View.VISIBLE);

                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            mDeviceViewModel.setConnected(false);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            updateFragment();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    mProgressBar.setVisibility(View.INVISIBLE); // HERE
                    break;
                case Constants.MESSAGE_READ:
                    mProgressBar.setVisibility(View.INVISIBLE);
                    try {
                        String readBuf = (String) msg.obj;
                        JSONObject mainObject = new JSONObject(readBuf);
                        int errorCode = mainObject.getInt("errorCode");
                    } catch(JSONException e) {
                        break;
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDevice = msg.getData().getString(Constants.DEVICE_NAME);
                    break;
            }
        }
    };

    private void sendInformation(String textToSend) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Mit keinem Gerät verbunden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (!textToSend.equals("") && !textToSend.equals(" ")) {
            mBluetoothService.writeJson(textToSend);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.bluetooth_connection_menu) {
            DialogFragment dialog = new BluetoothDevicesDialogFragment();
            dialog.show(getSupportFragmentManager(), "BluetoothDevices");
            return true;
        }
        return false;
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            menuItem -> false;

    @Override
    public void deviceClick() {
        if (!mConnectedDevice.equals(mDeviceViewModel.getSelected().getName())) {
            mBluetoothService.connect(mDeviceViewModel.getSelected(), false);
            mProgressBar.setVisibility(View.VISIBLE);
        }
        updateFragment();
    }

    private void updateFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            fragmentTransaction.detach(currentFragment);
            fragmentTransaction.attach(currentFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void parSendInformation(String textToSend) {
        sendInformation(textToSend);
    }

    private void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}