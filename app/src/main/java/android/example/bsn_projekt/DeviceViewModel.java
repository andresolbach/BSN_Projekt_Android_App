package android.example.bsn_projekt;
import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.bluetooth.BluetoothDevice;

public class DeviceViewModel extends AndroidViewModel {
    String TAG = "DeviceViewModel";

    private BluetoothDevice mSelectedDevice;
    private Boolean mConnected = false;

    public DeviceViewModel (Application application) {
        super(application);
    }

    public void select(BluetoothDevice device){
        mSelectedDevice = device;
    }

    public void setConnected(Boolean connected){
        mConnected = connected;
    }

    public BluetoothDevice getSelected() {
        return mSelectedDevice;
    }

    public Boolean getConnected() {
        return mConnected;
    }
}