package com.megster.uribeacon;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.le.*;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Advertise a URI Beacon with Android L
 * See http://physical-web.org for more info
 *
 * @author Don Coleman
 */
public class UriBeacon extends Activity {

    private static final String TAG = "UriBeacon";
    private static final int ENABLE_BLUETOOTH_REQUEST = 17;
    // Really 0xFED8, but Android seems to prefer the expanded 128-bit UUID version
    public static final UUID URI_BEACON_128_BIT_UUID = UUID.fromString("0000FED8-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uri_beacon);

        setupBluetooth();
    }

    private void advertiseUriBeacon() {

        BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertisementData advertisementData = getAdvertisementData();
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();

        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);
    }

    private AdvertisementData getAdvertisementData() {
        AdvertisementData.Builder builder = new AdvertisementData.Builder();
        builder.setIncludeTxPowerLevel(false); // reserve advertising space for URI

        byte[] beaconData = new byte[9];
        beaconData[0] = (byte) 0xD8; // URI Beacon ID 0xFED8
        beaconData[1] = (byte) 0xFE; // URI Beacon ID 0xFED8
        beaconData[2] = 0x00; // flags
        beaconData[3] = 0x20; // transmit power
        beaconData[4] = 0x00; // http://www.
        beaconData[5] = 0x65; // e
        beaconData[6] = 0x66; // f
        beaconData[7] = 0x66; // f
        beaconData[8] = 0x08; // .org

        builder.setServiceData(new ParcelUuid(new UUID(0xFE, 0xD8)), beaconData);

        // Adding 0xFED8 to the "Service Complete List UUID 16" (0x3) for iOS compatibility
        // Need to use the 128 bit version of the UUID o_O
        builder.setServiceUuids(new ArrayList<ParcelUuid>() {{
            add(new ParcelUuid(URI_BEACON_128_BIT_UUID));
        }});

        return builder.build();
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        builder.setType(AdvertiseSettings.ADVERTISE_TYPE_NON_CONNECTABLE);

        return builder.build();
    }

    private void setupBluetooth() {

        BluetoothManager bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.bluetooth_error);
            builder.setMessage(R.string.no_bluetooth).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.show();

        } else if (!bluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST);

        } else {
            advertiseUriBeacon();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.uri_beacon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                advertiseUriBeacon();
            } else {
                finish();
            }
        }
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement successful";
            Log.d(TAG, successMsg);
        }

        @Override
        public void onFailure(int i) {
            String failMsg = "Advertisement failed error code: " + i;
            Log.e(TAG, failMsg);
        }
    };

}
