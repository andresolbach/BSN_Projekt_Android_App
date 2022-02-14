package android.example.bsn_projekt;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import bsn_projekt.R;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;


public class UserInputFragment extends Fragment {
    // Debug
    private static final String TAG = "SpectrometerSettings";

    // Intent Request Codes
    private final static int REQUEST_ENABLE_BT = 1;

    // Member Fields
    private DeviceViewModel mDeviceViewModel;
    private ParametersInterface mCallback;
    private EditText mtextToSendEditText;

    public interface ParametersInterface {
        void parSendInformation(String textToSend);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null ) {
            mDeviceViewModel = ViewModelProviders.of(getActivity()).get(DeviceViewModel.class);
        }
        mDeviceViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(DeviceViewModel.class);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (ParametersInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_user_input, container, false);

        // Layout Views
        TextView mNotConnectedText = rootView.findViewById(R.id.textview);
        Button mSendTextButton = rootView.findViewById(R.id.send_text_button);
        Button mRasperryPiShutdownButton = rootView.findViewById(R.id.rasperry_pi_shutdown_button);
        Button mRasperryPiRebootButton = rootView.findViewById(R.id.rasperry_pi_reboot_button);
        Button mRasperryPiOpenChromium = rootView.findViewById(R.id.rasperry_pi_open_chromium_button);
        Button mRasperryPiOpenExplorer = rootView.findViewById(R.id.rasperry_pi_open_explorer_button);
        mtextToSendEditText = rootView.findViewById(R.id.text_to_send);
        RelativeLayout mUserInput = rootView.findViewById(R.id.parameters_input);

        if (mDeviceViewModel.getConnected()) {
            mUserInput.setVisibility(View.VISIBLE);
            mNotConnectedText.setVisibility(View.INVISIBLE);

            mRasperryPiShutdownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run(){
                            mCallback.parSendInformation("shutdown");
                        }
                    };
                    Handler h = new Handler();
                    h.postDelayed(r, 3000); // <-- the "1000" is the delay time in miliseconds.
                    Toast.makeText(getContext(), "Rasperry Pi wird ausgeschaltet...", Toast.LENGTH_SHORT).show();
                }
            });

            mRasperryPiRebootButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run(){
                            mCallback.parSendInformation("reboot");
                        }
                    };
                    Handler h = new Handler();
                    h.postDelayed(r, 3000); // <-- the "1000" is the delay time in miliseconds.
                    Toast.makeText(getContext(), "Rasperry Pi wird neu gestartet...", Toast.LENGTH_SHORT).show();
                }
            });

            mRasperryPiOpenChromium.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run(){
                            mCallback.parSendInformation("chromium-browser");
                        }
                    };
                    Handler h = new Handler();
                    h.postDelayed(r, 3000); // <-- the "1000" is the delay time in miliseconds.
                    Toast.makeText(getContext(), "Chrome wird geöffnet...", Toast.LENGTH_SHORT).show();
                }
            });

            mRasperryPiOpenExplorer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run(){
                            mCallback.parSendInformation("explorer");
                        }
                    };
                    Handler h = new Handler();
                    h.postDelayed(r, 3000); // <-- the "1000" is the delay time in miliseconds.
                    Toast.makeText(getContext(), "Explorer wird geöffnet...", Toast.LENGTH_SHORT).show();
                }
            });

            mSendTextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (areParametersValid()) {
                        String textToSend = mtextToSendEditText.getText().toString();
                        mCallback.parSendInformation(textToSend);
                        Toast.makeText(getContext(),"Text abgeschickt!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            mUserInput.setVisibility(View.INVISIBLE);
            mNotConnectedText.setVisibility(View.VISIBLE);
        }
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean areParametersValid() {
        if (mtextToSendEditText.getText().toString().equals("")) {
            Toast.makeText(getContext(),
                    "Der Text darf nicht leer sein", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
