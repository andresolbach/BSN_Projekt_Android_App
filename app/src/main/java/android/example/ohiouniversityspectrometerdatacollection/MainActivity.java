package android.example.ohiouniversityspectrometerdatacollection;



import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

//import com.github.mikephil.charting.data.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements BluetoothDevicesDialogFragment.DeviceDialogListener, SpectrometerSettingsFragment.ParametersInterface {
    private static final String TAG = "MainActivity";





    // SPEECH TO TEXT
//    protected static final int RESULT_SPEECH = 1;
//    private EditText integration_time_edit;


    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;

    private ProgressBar mProgressBar;

    // Member Fields
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
//        integration_time_edit = findViewById(R.id.integration_time_edit);
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
//                        integration_time_edit.setText("");
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
                new SpectrometerSettingsFragment()).commit();
        // Set Toolbar title
        mToolBar.setTitle(R.string.toolbar_spectrometer_settings);

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
//                integration_time_edit.setText(text.get(0));
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
            //mTestButton.setVisibility(View.VISIBLE);
            //mTestEditText.setVisibility(View.VISIBLE);
            /*
            mTestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Send a message using content of the edit text widget
                    String information = mTestEditText.getText().toString();
                    sendInformation(information);
                }
            });
             */
        }

    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            SpectrometerSettingsFragment parFrag = (SpectrometerSettingsFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_user_input);
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            mDeviceViewModel.setConnected(true);
                            makeToast("Now connected to " + mDeviceViewModel.getSelected().getName());
                            mProgressBar.setVisibility(View.INVISIBLE);
                            updateFragment();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            mProgressBar.setVisibility(View.VISIBLE);

                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            //Toast.makeText(activity, "Error, Disconnected",
                            // Toast.LENGTH_SHORT).show();
                            mDeviceViewModel.setConnected(false);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            updateFragment();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    mProgressBar.setVisibility(View.VISIBLE);
                    //mText.setText(writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    mProgressBar.setVisibility(View.INVISIBLE);
                    try {
                        String readBuf = (String) msg.obj;
                        JSONObject mainObject = new JSONObject(readBuf);
                        int errorCode = mainObject.getInt("errorCode");
                        if (errorCode == 0) {
                            JSONArray spectra = mainObject.getJSONArray("spectra");
                            JSONArray wavelengths = mainObject.getJSONArray("wavelengths");
                            mDeviceViewModel.setSpectraAndWavelengths(
                                    new SpectraAndWavelengths(spectra, wavelengths));
                            makeToast("Data Received");
                            mDeviceViewModel.refreshLineData("Message READ");
                            mDeviceViewModel.setDate(new Date());
                            mDeviceViewModel.setIsSaved(false);
                        } else if (errorCode == 2) {
                            makeToast("Error: No Spectrometer Found");
                        }
                    } catch(JSONException e) {
                        break;
                    }
                    /*
                    if (readBuf.equals("calibratedw") || readBuf.equals(" calibratedw")) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        makeToast("Graph Calibrated");
                    } else {
                        mDeviceViewModel.setSpectraAndWavelengths(new SpectraAndWavelengths(readBuf));
                        Log.d(TAG, "handleMessage: Clearing Entries");
                        mProgressBar.setVisibility(View.INVISIBLE);
                        makeToast("Data Received");

                    /*
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    String[] chartData = readBuf.split(" ");
                    for (int i = 0; i < chartData.length; ++i) {
                        Log.d(TAG, "handleMessage: Adding point (" + Integer.toString(mDeviceViewModel.getNumPoints()) +", " + Float.toString(Float.parseFloat(chartData[i])) + ")");
                        mDeviceViewModel.addData(new Entry(mDeviceViewModel.getNumPoints(), Float.parseFloat(chartData[i])));
                    }


                        mDeviceViewModel.refreshLineData("Message READ");
                        mDeviceViewModel.setDate(new Date());
                        mDeviceViewModel.setIsSaved(false);
                    }
                    */

                    //mText.setText("Data received and plotted.");
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDevice = msg.getData().getString(Constants.DEVICE_NAME);
                    if (this != null) {
                        //Toast.makeText(mainActivity, "Connected to " + connectedDeviceName,
                                //Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void sendInformation(String integrationTime) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (!integrationTime.equals("") && !integrationTime.equals(" ")) {
            mBluetoothService.writeJson(integrationTime);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mTestEditText.setText(mOutStringBuffer);
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

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment selectedFragment;

                    // Get the selected Fragment
//                    switch (menuItem.getItemId()) {
//                        case R.id.nav_files:
//                            mToolBar.setTitle(R.string.saved_files_title);
//                            selectedFragment = new FilesFragment();
//                            loadFragment(selectedFragment);
//                            return true;
//                        case R.id.nav_user_input:
//                            mToolBar.setTitle(R.string.toolbar_spectrometer_settings);
//                            selectedFragment = new SpectrometerSettingsFragment();
//                            loadFragment(selectedFragment);
//                            return true;
//                        case R.id.nav_graph:
//                            mToolBar.setTitle(R.string.graph_title);
//                            selectedFragment = new GraphFragment();
//                            loadFragment(selectedFragment);
//                            return true;
//                    }

                    return false;
                }
            };

    /**
     * Replace and commit fragment
     * @param fragment a non null fragment to be displayed
     */
    private void loadFragment(@NonNull Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        // Material Guidelines used to say not to add these to back stack, but Youtube adds to back
        // stack, so they removed that, and I'm not sure which to pick.
        // fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


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
    public void parSendInformation(String integrationTime) {
        mDeviceViewModel.clearEntries();
        sendInformation(integrationTime);
    }

    private void makeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
