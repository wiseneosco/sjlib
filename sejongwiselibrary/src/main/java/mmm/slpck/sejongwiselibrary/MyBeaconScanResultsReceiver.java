package mmm.slpck.sejongwiselibrary;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by idoit on 26/03/2019.
 */

public class MyBeaconScanResultsReceiver extends BroadcastReceiver {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context context, Intent intent) {
        int bleCallbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1);
//        boolean isScanning = intent.getBooleanExtra("o-scan", false);
//        Log.i("MyTag", "o-scan: "+isScanning);
        if (bleCallbackType != -1) {
            Log.d("MyTag", "Passive background scan callback type: "+bleCallbackType);
            ArrayList<ScanResult> scanResults = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
            // Do something with your ScanResult list here.
            // These contain the data of your matching BLE advertising packets
            for (int i=0;i<scanResults.size();i++){
                Log.i("MyTag","scanResult");
                BluetoothDevice device = scanResults.get(i).getDevice();
                int rssi = scanResults.get(i).getRssi();
                byte [] scanRecord = scanResults.get(i).getScanRecord().getBytes();
                final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(device, rssi, scanRecord);
                if(ibeacon != null){
                    Log.i("MyTag123","################################");
                    Log.i("MyTag123", "[iBeacon LOG] iBeacon Uuid = " + ibeacon.proximityUuid);
                    Log.i("MyTag123", "[iBeacon LOG] iBeacon Major = " + ibeacon.major);
                    Log.i("MyTag123", "[iBeacon LOG] iBeacon Minor = " + ibeacon.minor);
                    Log.i("MyTag123", "[iBeacon LOG] iBeacon Address = " + ibeacon.bluetoothAddress);
                    Log.i("MyTag123", "[iBeacon LOG] iBeacon RSSI = " + ibeacon.rssi);
                    Log.i("MyTag123", "[iBeacon LOG] iBeacon Distance = " + ibeacon.distance);
                    Log.i("MyTag123","################################");

                    saveArrayListToPreferences(ibeacon, context);

                }
            }
        }
    }

    private void saveArrayListToPreferences(iBeaconClass.iBeacon ibeacon, Context context){
        SharedPreferences preference = context.getSharedPreferences("preferences", MODE_PRIVATE);
        ArrayList<beaconObject> arrayList = getArrayListFromPreferences("beaconList",context);
        arrayList = addToBeaconList(ibeacon, arrayList);

        Gson gson = new Gson();
        String jsonStr = gson.toJson(arrayList);
        Log.d("TAG","jsonStr = " + jsonStr);

        SharedPreferences.Editor editor = preference.edit();
        editor.putString("beaconList", jsonStr);
        editor.commit();


    }

    private ArrayList<beaconObject> getArrayListFromPreferences(String key, Context context){
        SharedPreferences preference = context.getSharedPreferences("preferences", MODE_PRIVATE);
        String jsonStr = preference.getString(key,"");
        Type type = new TypeToken<ArrayList<beaconObject>>(){}.getType();
        Gson gson = new Gson();
        ArrayList<beaconObject> beaconList = new ArrayList<>();
        if(!TextUtils.isEmpty(jsonStr)){
            beaconList = gson.fromJson(jsonStr, type);
        }

        return beaconList;

    }

    private ArrayList<beaconObject> addToBeaconList(iBeaconClass.iBeacon ibeacon, ArrayList<beaconObject> beaconList){
        boolean isUpdated = false;


        if(beaconList.size() > 0){
            for (int i=0;i<beaconList.size();i++){

                if(beaconList.get(i).getProximityUuid().equals(ibeacon.proximityUuid)  && beaconList.get(i).getMajor() == ibeacon.major
                        && beaconList.get(i).getMinor() == ibeacon.minor){
                    if(ibeacon.rssi != 127) {
                        beaconList.get(i).setRssi(ibeacon.rssi);
                    }
                    isUpdated = true;
                    Log.i("MyTag", "A.1.1");
                }
            }

            if(!isUpdated){
                beaconObject tempObj = new beaconObject();
                tempObj.setRssi(ibeacon.rssi);
                tempObj.setProximityUuid(ibeacon.proximityUuid);
                tempObj.setMajor(ibeacon.major);
                tempObj.setMinor(ibeacon.minor);
                if(ibeacon.rssi != 127) {
                    beaconList.add(tempObj);
                }
            }
        }
        else {


            beaconObject tempObj = new beaconObject();
            tempObj.setRssi(ibeacon.rssi);
            tempObj.setProximityUuid(ibeacon.proximityUuid);
            tempObj.setMajor(ibeacon.major);
            tempObj.setMinor(ibeacon.minor);
            if(ibeacon.rssi != 127){
                beaconList.add(tempObj);
            }

        }

        return  beaconList;
    }
}
