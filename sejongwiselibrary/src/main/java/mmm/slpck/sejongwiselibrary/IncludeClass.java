package mmm.slpck.sejongwiselibrary;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.nabinbhandari.android.permissions.Permissions;
import com.nabinbhandari.android.permissions.PermissionHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by idoit on 29/04/2019.
 */

public class IncludeClass{

    ArrayList<beaconObject> beaconList;
    private Handler mHandler;
    private Context context;
    private WebView webView = null;
    private String user_id = "";


    public IncludeClass(String user_id, Context context, WebView webView) {
        Log.i("MyTag", "user_id: "+user_id);
        this.context = context;
        preferences = this.context.getSharedPreferences("preferences", MODE_PRIVATE);
        this.webView = webView;
        this.user_id = user_id;

    }


    @JavascriptInterface
    public void getBeaconFirst(String str) {
        requestLocation();


    }

    @JavascriptInterface
    public String getUser(final String param) {

        webView.post(new Runnable() {
            public void run() {

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("userId", user_id);
                Log.i("MyTag", "getUser, param: "+param);
                webView.loadUrl(String.format("javascript:setUser('%s')", jsonObject.toString()));
            }
        });

        return user_id;
    }

    private SharedPreferences preferences;
    private beaconObject nearestBeacon = null;
    Runnable handler = new Runnable(){

        @Override
        public void run() {
            LocationManager mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if(!mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)){

                showMessage("위치 서비스 활성화하셔서 다시 시도하여 주십시오.", context);

                Log.i("MyTag", "Notified");
            }

            else {

                final BluetoothAdapter mBluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();

                if (mBluetoothAdapter.isEnabled()) {

                    int delay = 5000;
//                    int delay = 15000;

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                mBluetoothAdapter.getBluetoothLeScanner().stopScan(getScanCallbackIntent(context));
                            }
                            else if(Build.VERSION.SDK_INT >= 18) {
                                Log.i("MyTag1","stopLeScan");
                                mBluetoothAdapter.stopLeScan(mScanCallback);
                            }
                            else {
                                showMessage("비콘 지원되지 않은 기기입니다.", context);
                                Log.i("MyTag", "SDK version is too low. Unable to stop searching for beacons");
                            }




                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                beaconList = getArrayListFromPreferences("beaconList", context);
                            }

                            if(beaconList.size() <= 0){

                            }

                            WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            if (!manager.isWifiEnabled()) {
                                manager.setWifiEnabled(true);
                            }
                            int ipAddress = manager.getDhcpInfo().ipAddress;
                            Object [] ipPartsArr = new Object[4];
                            ipPartsArr[0] = Integer.valueOf(ipAddress & 255);
                            ipPartsArr[1] = Integer.valueOf((ipAddress >> 8) & 255);
                            ipPartsArr[2] = Integer.valueOf((ipAddress >> 16) & 255);
                            ipPartsArr[3] = Integer.valueOf((ipAddress >> 24) & 255);
                            String format = String.format("%d.%d.%d.%d", ipPartsArr);

                            JSONObject mainObj = new JSONObject();
                            JSONArray beaconArr = new JSONArray();
                            try {


                            Log.i("MyTag1","##===========================================");
                            for (int i=0;i<beaconList.size();i++){
                                Log.i("MyTag1", "rssi:"+beaconList.get(i).getRssi());
                                Log.i("MyTag1", "uuid:"+beaconList.get(i).getProximityUuid());
                                Log.i("MyTag1", "major:"+beaconList.get(i).getMajor());
                                Log.i("MyTag1", "minor:"+beaconList.get(i).getMinor());

                                    JSONObject jsonObject = new JSONObject();

                                    jsonObject.put("uuid", beaconList.get(i).getProximityUuid());
                                    jsonObject.put("major", beaconList.get(i).getMajor());
                                    jsonObject.put("minor", beaconList.get(i).getMinor());


                                beaconArr.put(jsonObject);

                            }

                                JSONObject obj = new JSONObject();
                                obj.put("ipAddress", format);
                                JSONArray apArr = new JSONArray();
                                apArr.put(obj);

                                mainObj.put("userId", user_id);
                                mainObj.put("beaconInfo", beaconArr);
                                mainObj.put("apInfo", apArr);


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Log.i("MyTag1","##===========================================");

                            Log.i("MyTag", "json: "+mainObj.toString());
                            final JSONObject obj = mainObj;

                                //call javascript method
                                webView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        webView.loadUrl(String.format("javascript:beaconFirstCallback('%s')", obj.toString()));
                                    }
                                });


                        }
                    }, delay);

                    beaconList = new ArrayList<>();

                    //reset arraylist in pref
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("beaconList", "");
                        editor.commit();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ScanSettings settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
                        List<ScanFilter> filters = getScanFilter(); // Make a scan filter matching the beacons I care about
                        BluetoothManager bluetoothManager =
                                (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
                        Intent intent = new Intent(context, MyBeaconScanResultsReceiver.class);
                        intent.putExtra("o-scan", true);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, pendingIntent);
                    }
                    else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            mBluetoothAdapter.startLeScan(mScanCallback);
                        }
                        else {
                            showMessage("비콘 지원되지 않은 기기입니다.", context);
                            Log.i("MyTag", "SDK version is too low. Unable to search for beacons");
                        }
                    }

                } else {
                    mBluetoothAdapter.enable();
                    mHandler.postDelayed(this, 1000);
                }

            }
        }
    };

    private void searchForBeacon(){

        mHandler = new Handler();
        mHandler.postDelayed(handler, 1000);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public List<ScanFilter> getScanFilter()
    {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setManufacturerData(0x004c, new byte[] {});
        ScanFilter filter = builder.build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        return filters;

    }

    private BluetoothAdapter.LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            String sr = "ScanRecord:";
            final int my_rssi;

            for(byte b : scanRecord){
                sr += String.format("%02x ", b);
            }


            final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(device, rssi, scanRecord);
            if(ibeacon != null){
                Log.i("MyTag", "[iBeacon LOG] iBeacon Uuid = " + ibeacon.proximityUuid);
                Log.i("MyTag", "[iBeacon LOG] iBeacon Major = " + ibeacon.major);
                Log.i("MyTag", "[iBeacon LOG] iBeacon Minor = " + ibeacon.minor);
                Log.i("MyTag", "[iBeacon LOG] iBeacon Address = " + ibeacon.bluetoothAddress);
                Log.i("MyTag", "[iBeacon LOG] iBeacon RSSI = " + ibeacon.rssi);
                Log.i("MyTag", "[iBeacon LOG] iBeacon Distance = " + ibeacon.distance);

                addToBeaconList(ibeacon);

            }
            else {

            }
        }
    };

    public void addToBeaconList(iBeaconClass.iBeacon ibeacon){
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
    }

    PendingIntent getScanCallbackIntent(Context context) {
        Intent intent = new Intent(context, MyBeaconScanResultsReceiver.class);
//        intent.putExtra("o-scan", false);
        return PendingIntent.getBroadcast(context,0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static String intToString(int value){
        if(value == 0){
            return "";
        }
        else {
            return String.valueOf(value);
        }

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

    private void showMessage(String msg, Context context){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setMessage(msg);
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void requestLocation() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        String rationale = "비콘 및 AP 체크하기 위하여 위치 서비스 권한을 부여하여 주십시오.";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Info")
                .setSettingsDialogTitle("Warning");

        Permissions.check(context, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                searchForBeacon();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                Toast.makeText(context, "위치 서비스 권한을 부여하여 주십시오.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

}
