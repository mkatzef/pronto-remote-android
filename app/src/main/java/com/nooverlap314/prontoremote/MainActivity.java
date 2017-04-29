package com.nooverlap314.prontoremote;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.*;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.felhr.usbserial.UsbSerialInterface;
import com.felhr.usbserial.UsbSerialDevice;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    public static final String ACTION_USB_PERMISSION = "com.nooverlap314.prontoremote.USB_PERMISSION";
    public static final String REMOTE_CONFIG_FILE_NAME = "remote_config.json";

    public static final String CONFIG_KEY_DEVICE_NAME = "DeviceName";
    public static final String CONFIG_KEY_BUTTON_NAME = "ButtonName";
    public static final String CONFIG_KEY_BUTTON_CODE = "ButtonCode";

    public static final Integer INVALID_BUTTON = -1;
    public static final Integer BUTTON_COUNT = 30;
    public static final Integer BAUD_RATE = 115200;
    public static final Integer ARDUINO_DEVICE_ID = 1027;

    private static HashMap<Integer, RemoteButton> RemoteState;
    private static Integer focusButtonID = -1;
    private static UsbManager usbManager;
    public UsbSerialDevice serialPort;
    private boolean connected = false;

    private ArrayList<String> dataBuffer;
    private int expectedProntoLength = 0;
    private int receivedProntoLength = 0;
    private String receivedPronto;

    UsbDevice device;
    UsbDeviceConnection connection;
    static boolean granted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isTaskRoot()) {
            finish();
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        String savedJson = "";
        try {
            StringBuilder sb = new StringBuilder();
            FileInputStream fis = openFileInput(REMOTE_CONFIG_FILE_NAME);
            while (fis.available() > 0) {
                sb.append((char) fis.read());
            }
            fis.close();
            savedJson = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        RemoteState = getRemotes(savedJson);
        onClickConnect(findViewById(R.id.status_label));
        updateButtonLabels();
    }

    //----------------------------------------------------------------------------------------------
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                String[] newTokens = (new String(arg0, "UTF-8")).trim().split("\\s+");

                int offset = 0;
                if (expectedProntoLength == 0) {
                    expectedProntoLength = Integer.parseInt(newTokens[0]);
                    offset++;
                }

                for (int i = 0; i < newTokens.length - offset; i++) {
                    dataBuffer.add(newTokens[i + offset]);
                    receivedProntoLength++;
                }

                if (receivedProntoLength == expectedProntoLength) {
                    StringBuilder sb = new StringBuilder();
                    for (String entry : dataBuffer) {
                        sb.append(entry);
                        sb.append(" ");
                    }
                    expectedProntoLength = 0;
                    receivedProntoLength = 0;
                    dataBuffer = null;

                    receivedPronto = sb.toString();
                    runOnUiThread(interpretResponse);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                attemptConnection();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickConnect(findViewById(R.id.status_label));
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serialPort.close();
                connected = false;
                updateStatus("Connection lost.");
            }
        }
    };

    private Runnable interpretResponse = new Runnable() {
        @Override
        public void run() {
            updateStatus("Received data!");

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("RecordedPronto", receivedPronto);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(MainActivity.this, "Saved recorded code to clipboard!", Toast.LENGTH_LONG).show();
        }
    };


    private void attemptConnection() {
        if (granted) {
            connection = usbManager.openDevice(device);
            try {
                serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            } catch (Exception e) {
                serialPort = null;
            }
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    connected = true;
                    updateStatus("Connected!");

                } else {
                    updateStatus("Port not open.");
                }
            } else {
                updateStatus("Port is null.");
            }
        } else {
            updateStatus("Permission not granted.");
        }
    }
    //----------------------------------------------------------------------------------------------


    //----------------------------------------------------------------------------------------------
    public HashMap<Integer, RemoteButton> getRemotes(String jsonString) {
        JSONObject remotesJObject;
        try {
            remotesJObject = new JSONObject(jsonString);
        } catch (Exception e) {
            remotesJObject = null;
        }

        HashMap remotesMap = new HashMap<>();

        for (int i = 0; i < BUTTON_COUNT; i++) {
            String deviceName = "";
            String buttonName = "";
            String buttonCode = "";

            try {
                JSONObject buttonJObject = remotesJObject.getJSONObject(Integer.toString(i));
                deviceName = buttonJObject.getString(CONFIG_KEY_DEVICE_NAME);
                buttonName = buttonJObject.getString(CONFIG_KEY_BUTTON_NAME);
                buttonCode = buttonJObject.getString(CONFIG_KEY_BUTTON_CODE);

            } catch (Exception e) {
                updateStatus("Could not parse the remote file.");
            }

            RemoteButton button = new RemoteButton(deviceName, buttonName, buttonCode);
            remotesMap.put(i, button);
        }

        return remotesMap;
    }


    public String saveRemotes() {
        JSONObject remoteJObject = new JSONObject();
        for (int i = 0; i < BUTTON_COUNT; i++) {
            JSONObject buttonJObject = new JSONObject();
            RemoteButton focusButton = RemoteState.get(i);
            try {
                buttonJObject.put(CONFIG_KEY_DEVICE_NAME, focusButton.getDeviceName());
                buttonJObject.put(CONFIG_KEY_BUTTON_NAME, focusButton.getButtonName());
                buttonJObject.put(CONFIG_KEY_BUTTON_CODE, focusButton.getButtonCode());
                remoteJObject.put(Integer.toString(i), buttonJObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String remoteString = remoteJObject.toString();
        try {
            FileOutputStream fos = openFileOutput(REMOTE_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            fos.write(remoteString.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return remoteString;
    }
    //----------------------------------------------------------------------------------------------

    public void sendButtonCode(Integer buttonID) {
        RemoteButton focusButton = RemoteState.get(buttonID);
        String buttonCode = focusButton.getButtonCode().toUpperCase();

        byte[] message = buttonCode.getBytes();
        if (connected) {
            serialPort.write(message);
        }
    }

    public void remoteButtonPress(View view) {
        String tag = view.getTag().toString();
        int buttonId = Integer.valueOf(tag);
        focusButtonID = buttonId;

        RemoteButton focusButton = RemoteState.get(buttonId);

        String deviceName = focusButton.getDeviceName();
        String buttonName = focusButton.getButtonName();
        String buttonLabel;
        if (deviceName.equals("") && buttonName.equals("")) {
            buttonLabel = "Button " + buttonId;
        } else if (deviceName.equals("")) {
            buttonLabel = buttonName + "(" + buttonId + ")";
        } else if (buttonName.equals("")) {
            buttonLabel = deviceName + "(" + buttonId + ")";
        } else {
            buttonLabel = deviceName + " - " + buttonName;
        }

        if (connected) {
            sendButtonCode(buttonId);
            updateStatus(buttonLabel + " code sent.");
        } else {
            updateStatus(buttonLabel + " selected.");
        }
    }

    public void updateStatus(String message) {
        TextView textView = (TextView) findViewById(R.id.status_label);
        textView.setText(message);
    }


    public void updateButtonLabels() {
        for (int i = 0; i < BUTTON_COUNT; i++) {
            RemoteButton button = RemoteState.get(i);
            String deviceName = button.getDeviceName();
            String buttonName = button.getButtonName();
            String buttonLabel;
            if (deviceName.equals("") && buttonName.equals("")) {
                buttonLabel = "Button " + i;
            } else if (deviceName.equals("")) {
                buttonLabel = buttonName + "(" + i + ")";
            } else if (buttonName.equals("")) {
                buttonLabel = deviceName + "(" + i + ")";
            } else {
                buttonLabel = deviceName + " - " + buttonName;
            }
            String buttonID = "remote_button_" + i;
            int resId = getResources().getIdentifier(buttonID, "id", getPackageName());

            ((Button) findViewById(resId)).setText(buttonLabel);
        }
    }

    public void onClickConnect(View view) {
        if (connected){
            updateStatus("Connected!");
        } else {
            boolean foundDevice = false;
            HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
            if (!usbDevices.isEmpty()) {
                for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                    device = entry.getValue();
                    int deviceVID = device.getVendorId();
                    if (deviceVID == ARDUINO_DEVICE_ID) {
                        PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(device, pi);
                        foundDevice = true;
                    } else {
                        connection = null;
                        device = null;
                    }
                }
            }

            if (foundDevice) {
                attemptConnection();
            } else {
                updateStatus("Device not found. Tap here to check again.");
            }
        }
    }

    public void onClickRecord(View view) {
        if (connected) {
            expectedProntoLength = 0;
            receivedProntoLength = 0;
            dataBuffer = new ArrayList<>();

            serialPort.write(new byte[]{(byte)'R'});
            updateStatus("Waiting for device response.");

        } else {
            updateStatus("Requires device.");
        }
    }

    public void onClickEdit(View view) {
        if (focusButtonID < 0) {
            updateStatus("Please select (press) a button first.");
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            View content = inflater.inflate(R.layout.dialog_button_edit, null);

            final EditText deviceNameEntry = (EditText) content.findViewById(R.id.deviceNameEntry);
            final EditText buttonNameEntry = (EditText) content.findViewById(R.id.buttonNameEntry);
            final EditText buttonCodeEntry = (EditText) content.findViewById(R.id.buttonCodeEntry);

            RemoteButton focusButton = RemoteState.get(focusButtonID);
            deviceNameEntry.setText(focusButton.getDeviceName());
            buttonNameEntry.setText(focusButton.getButtonName());
            buttonCodeEntry.setText(focusButton.getButtonCode());

            builder .setTitle("Edit Button " + focusButtonID)
                    .setView(content)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String deviceName = deviceNameEntry.getText().toString();
                            String buttonName = buttonNameEntry.getText().toString();
                            String buttonCode = buttonCodeEntry.getText().toString();

                            RemoteState.put(focusButtonID, new RemoteButton(deviceName, buttonName, buttonCode));
                            saveRemotes();
                            updateButtonLabels();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();

        }
    }


    public void onClickSave(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("SavedRemote", saveRemotes());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(MainActivity.this, "Saved remote to clipboard!", Toast.LENGTH_LONG).show();
    }


    public void onClickImport(View view) {
        final EditText remoteJsonEditText = new EditText(this);
        remoteJsonEditText.setHint("Enter JSON");
        remoteJsonEditText.setMaxLines(3);

        new AlertDialog.Builder(this)
                .setTitle("Remote JSON")
                .setMessage("Paste in the massive clump of JSON describing the 30 buttons.")
                .setView(remoteJsonEditText)
                .setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        RemoteState = getRemotes(remoteJsonEditText.getText().toString());
                        updateButtonLabels();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
}
