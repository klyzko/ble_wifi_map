package com.example.map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.map.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Looper;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {
    private Runnable updateRunnable;
    private ImageView mapImageView;
    private Bitmap mapBitmap;
    private Canvas canvas;
    private Paint pointPaint;
    private Canvas canvass;
    private RadioFingerprinting fingerprinting_wifi = new RadioFingerprinting("/data/data/com.example.map/files/data_wifi.txt");
    private RadioFingerprinting fingerprinting_ble=new RadioFingerprinting("/data/data/com.example.map/data/data_ble.txt");
    private Handler handler = new Handler();
    private Handler handler2 = new Handler();
    final static String filename="known_devices.txt";
    private static final int PERMISSIONS_REQUEST_CODE = 123;
    public static List<String> deviseadres = new ArrayList<>();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SAVE_FILE = 4;
    private static final int REQUEST_PERMISSION_LOCATION = 2;
    private  static final int REQUEST_PERMISSION_STORAGE=3;
    private static final long SCAN_INTERVAL = 5000;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private List<String> printt;

    private List<BluetoothDevice> deviceList;//лист список устройств
    //private
    private Map<String, Integer> rssiMap;
    private Timer scanTimer;
    private Timer timer;
    private EditText xEditText;
    private EditText yEditText;
    private WifiManager wifiManager;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<ScanResult> scanResults;
    private Handler handlerr;
    private Map<String,Integer> wifimap;
    private Map<String, Queue<Integer>> rssiFilterMap_w_w_w_w_w ; // Карта для хранения фильтрованных значений RSSI
   // private List<String> print_wifi;
    private static final int FILTER_WINDOW_SIZE = 20; // Размер окна фильтра скользящего среднего
    private static final int FILTER_WINDOW_SIZE_w = 2;
    private Map<String, Queue<Integer>> rssiFilterMap ; // Карта для хранения фильтрованных значений RSSI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapImageView = findViewById(R.id.mapImageView);

        // Load the map image
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.map_image);


        Map<String, Integer> measuredRSSI = new HashMap<>();
        measuredRSSI.put("AP1", -49);



        // Create a mutable copy of the bitmap
        mapBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Create a Canvas for drawing
        canvas = new Canvas(mapBitmap);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // wifi
        wifimap = new HashMap<>();
        rssiFilterMap_w_w_w_w_w= new HashMap<>();
        handlerr = new Handler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
        } else {
            startScanLoop();
        }
        //ble
        readFile();
        printt=new ArrayList<>();


        KnownDevicesList.getKnownDeviceNames();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            checkLocationPermission();
        }
        //обновление по таймеру
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Обновляем значения списка
                handler.post(new Runnable() {
                    @Override
                    public void run() {



                        //если этот метод то FILTER_WINDOW_SIZE=5
                       /* IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(receiver, filter);*/

                        bluetoothAdapter.startLeScan(mLeScanCallback);
                    }
                });
            }
        }, 0, 1000);
        startUpdateProcess(0.5);
    }


    private void startUpdateProcess(double k) {
        // Удалите существующие обратные вызовы, чтобы избежать параллельного выполнения нескольких экземпляров
        handler2.removeCallbacks(updateRunnable);

        // Определите логику обновления внутри Runnable
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.map_image);
                // Удалите предыдущую точку
                ImageView imageView = findViewById(R.id.mapImageView);
                mapBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(mapBitmap);

// Создайте новый объект Canvas, связанный с Bitmap
                canvas = new Canvas(mapBitmap);

// Рисуйте точки на Canvas
// ... ваш код для рисования точек ...

// Обновите ImageView, чтобы отобразить изменения
                imageView.invalidate();
                canvas = new Canvas(mapBitmap);
                canvas.drawBitmap(originalBitmap, 0, 0, null);
                // получение данных
                List<Map.Entry<RadioFingerprinting.Point, Double>> bestMatchLocations_ble = fingerprinting_ble.getLocation(rssiMap);
                List<Map.Entry<RadioFingerprinting.Point, Double>> bestMatchLocations_wifi = fingerprinting_wifi.getLocation(wifimap);
                RadioFingerprinting.Point location_ble = fingerprinting_ble.getWeightedLocation(bestMatchLocations_ble);
                RadioFingerprinting.Point location_wifi = fingerprinting_wifi.getWeightedLocation(bestMatchLocations_wifi);
                // Нарисуйте новую точку
                //ble

                float pointX_ble = (float)(100*location_ble.getX()); // Замените на нужную X-координату
                float pointY_ble = (float)(100*location_ble.getY()); // Замените на нужную Y-координату
                if (rssiMap.isEmpty())
                {
                    pointX_ble=0;
                    pointY_ble=0;

                }
                float radius = 50; // Замените на нужный радиус
                Paint paint_ble = new Paint();
                paint_ble.setColor(Color.RED);
                paint_ble.setStyle(Paint.Style.FILL);
                //canvas.drawCircle(pointX_ble, pointY_ble, radius, paint_ble);
                TextView textView_ble = findViewById(R.id.bleTextView);
                String coordinates_ble = "X: " + pointX_ble/100 + ", Y: " + pointY_ble/100;
                textView_ble.setText(coordinates_ble);

                //wifi
                float pointX_wifi = (float)(100*location_wifi.getX()); // Замените на нужную X-координату
                float pointY_wifi = (float)(100*location_wifi.getY()); // Замените на нужную Y-координату
                if (wifimap.isEmpty())
                {
                    pointX_wifi=0;
                    pointY_wifi=0;

                }
                Paint paint_wifi = new Paint();
                paint_wifi.setColor(Color.RED);
                paint_wifi.setStyle(Paint.Style.FILL);
                //canvas.drawCircle(pointX_wifi, pointY_wifi, radius, paint_wifi);
                TextView textView_wifi = findViewById(R.id.wifiTextView);
                String coordinates_wifi = "X: " + pointX_wifi/100 + ", Y: " + pointY_wifi/100;
                textView_wifi.setText(coordinates_wifi);
                //wifi+ble

                float pointX_wifi_ble = (float)(location_wifi.getX()*k+location_ble.getX()*k); // Замените на нужную X-координату
                float pointY_wifi_ble = (float)(location_wifi.getY()*k+location_ble.getY()*k); // Замените на нужную Y-координату
                if (wifimap.isEmpty() ||rssiMap.isEmpty() )
                {
                    pointX_wifi_ble=0;
                    pointY_wifi_ble=0;

                }
                Paint paint_wifi_ble = new Paint();
                paint_wifi_ble.setColor(Color.RED);
                paint_wifi_ble.setStyle(Paint.Style.FILL);
                canvas.drawCircle(pointX_wifi_ble*553+1105, pointY_wifi_ble*600+300, radius, paint_wifi_ble);
                TextView textView_wifi_ble = findViewById(R.id.nowTextView);
                String coordinates_wifi_ble = "X: " + pointX_wifi_ble + ", Y: " + pointY_wifi_ble;
                textView_wifi_ble.setText(coordinates_wifi_ble);




                // Запустите следующее обновление через 10 секунд
                handler2.postDelayed(this, 5000);
                imageView.invalidate();

            }
        };

        // Запустите процесс обновления
        handler2.post(updateRunnable);
    }



    private void startScanLoop() {
        handlerr.post(scanRunnable);
    }
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            scanWifiNetworks();
           // adapter.notifyDataSetChanged();
            handlerr.postDelayed(this, SCAN_INTERVAL);
        }
    };

    private void scanWifiNetworks() {
        wifiManager.startScan();
        scanResults = wifiManager.getScanResults();

        if (scanResults != null) {
            //adapter.clear();

            for (ScanResult scanResult : scanResults) {
                String name=scanResult.SSID;

                {
                    //если есть значение
                    if (wifimap.containsKey(scanResult.BSSID))
                    {
                        int rssi=scanResult.level;
                        int star=wifimap.get(scanResult.BSSID);
                        updateDeviceListAdapterr(scanResult,rssi,star);
                    }
                    else {
                        String networkInfo = ";" + scanResult.BSSID + ";" + scanResult.level;
                        //adapter.add(networkInfo);
                        //print_wifi.add(networkInfo);
                        wifimap.put(scanResult.BSSID,scanResult.level);
                        Queue<Integer> rssiFilter=new LinkedList<>();
                        rssiFilter.offer(scanResult.level);
                        rssiFilterMap_w_w_w_w_w.put(scanResult.BSSID, rssiFilter);

                    }}}
        }
    }

    private void updateDeviceListAdapterr(ScanResult deviceMac, int rssi, int star) {
        String networkInfo = ";" + deviceMac.BSSID + ";" + star;
       // int k = adapter.getPosition(networkInfo);

        updateFilteredRssi_w(deviceMac.BSSID,deviceMac.SSID,rssi);
        //deviceListAdapter.remove(deviceMac.getAddress() + deviceMac.getName()+" (RSSI: " + star + ")");
        // Удаляем старое значение RSSI из списка
        Queue<Integer> rssiFilter = rssiFilterMap_w_w_w_w_w.get(deviceMac.BSSID);





    }
    private void updateFilteredRssi_w(String deviceMac, String name,int rssi) {
        Queue<Integer> rssiFilter = rssiFilterMap_w_w_w_w_w.get(deviceMac);

        if (rssiFilter.size() >= FILTER_WINDOW_SIZE_w) {
            rssiFilter.poll(); // Удаляем самое старое значение из очереди, если размер окна превышен
        }

        rssiFilter.offer(rssi); // Добавляем новое значение RSSI в очередь фильтра

        // Вычисляем среднее значение фильтрованного RSSI
        int filteredRssi = calculateFilteredRssi(rssiFilter);
        rssiFilterMap_w_w_w_w_w.put(deviceMac,rssiFilter);
        // Обновляем фильтрованное значение в rssiMap
        if(rssiFilter.size()==FILTER_WINDOW_SIZE_w){
            int star=  wifimap.get(deviceMac);
            wifimap.put(deviceMac, filteredRssi);
            String networkInfos = ";" + deviceMac + ";" + star;
            String networkInfo = ";" + deviceMac + ";" + filteredRssi;
            //replaceValueInList_w(print_wifi,networkInfos,networkInfo);
        }
    }

    public static void replaceValueInList_w(List<String> list, String oldValue, String newValue) {
        int index = list.indexOf(oldValue);
        if (index != -1) {
            list.set(index, newValue);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScanLoop();
    }
    private void stopScanLoop() {
        handlerr.removeCallbacks(scanRunnable);
    }


    //ble_method


    public static void replaceValueInList(List<String> list, String oldValue, String newValue) {
        int index = list.indexOf(oldValue);
        if (index != -1) {
            list.set(index, newValue);
        }
    }
    // Метод checkLocationPermission() используется для проверки разрешения доступа к местоположению
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Показать объяснение пользователю, почему разрешение необходимо
                Toast.makeText(this, "Read storage permission is required to access known devices", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
            }
        } else {
            startBluetoothDiscovery();
        }
    }

    // Метод startBluetoothDiscovery() используется для запуска процесса обнаружения устройств Bluetooth.
    private void startBluetoothDiscovery() {
        deviceList = new ArrayList<>();
        //deviceListAdapter.clear();
        rssiMap = new HashMap<>();
        rssiFilterMap= new HashMap<>();

        bluetoothAdapter.startDiscovery();
        scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                bluetoothAdapter.startDiscovery();
            }
        }, 0, SCAN_INTERVAL);

    }
    //сохранение точки

    //фильтр rssi скользящим средним
    private void updateFilteredRssi(String deviceMac, String name,int rssi) {
        Queue<Integer> rssiFilter = rssiFilterMap.get(deviceMac);

        if (rssiFilter.size() >= FILTER_WINDOW_SIZE) {
            rssiFilter.poll(); // Удаляем самое старое значение из очереди, если размер окна превышен
        }

        rssiFilter.offer(rssi); // Добавляем новое значение RSSI в очередь фильтра

        // Вычисляем среднее значение фильтрованного RSSI
        int filteredRssi = calculateFilteredRssi(rssiFilter);
        rssiFilterMap.put(deviceMac,rssiFilter);
        // Обновляем фильтрованное значение в rssiMap
        if(rssiFilter.size()==FILTER_WINDOW_SIZE){
            int star=  rssiMap.get(deviceMac);
            rssiMap.put(deviceMac, filteredRssi);
            }
    }

    private int calculateFilteredRssi(Queue<Integer> rssiFilter) {
        int sum = 0;


        for (int rssi : rssiFilter) {
            sum += rssi;
        }

        return sum / rssiFilter.size(); // Возвращаем среднее значение из очереди фильтра
    }

    //обновление списка rssi
    private void updateDeviceList(BluetoothDevice device, int rssi, int num) {
        String deviceName = device.getName();
        String deviceMak=device.getAddress();
        if (deviceName != null && KnownDevicesList.getKnownDeviceNames().contains(deviceName)) {
            // Проверяем, есть ли уже устройство в списке
            int deviceIndex = deviceList.indexOf(device);
            if (deviceIndex != -1 && rssiMap.containsKey(deviceMak)) {
                // Если устройство уже существует в списке, обновляем только его Rssi

                //int k =deviceListAdapter.getPosition(deviceMak + deviceName );

                int star=rssiMap.get(deviceMak);
                updateDeviceListAdapter(device, rssi,star);


            } else {
                // Если устройства нет в списке, добавляем его со значением Rssi
                deviceList.add(device);

                rssiMap.put(device.getAddress(),rssi);
                rssiFilterMap.put(device.getAddress(), new LinkedList<>());
                Queue<Integer> rssiFilter=new LinkedList<>();
                rssiFilter.offer(rssi);
                rssiFilterMap.put(device.getAddress(), rssiFilter);

            }
        }
    }
    private void updateDeviceListAdapter(BluetoothDevice deviceMac, int rssi, int star) {
        // int k = deviceListAdapter.getPosition(deviceMac.getAddress() + deviceMac.getName()+" (RSSI: " + star + ")");

        updateFilteredRssi(deviceMac.getAddress(),deviceMac.getName(),rssi);
        //deviceListAdapter.remove(deviceMac.getAddress() + deviceMac.getName()+" (RSSI: " + star + ")");
        // Удаляем старое значение RSSI из списка
        // Queue<Integer> rssiFilter = rssiFilterMap.get(deviceMac.getAddress());





    }








    //запроса активации Bluetooth и запроса сохранения файла.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                checkLocationPermission();
            } else {
                Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        if (requestCode == REQUEST_SAVE_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();

            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                for (String deviceName : KnownDevicesList.getKnownDeviceNames()) {
                    writer.write(deviceName);
                    writer.newLine();
                }

                writer.close();
                Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1;




    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanLoop();
            }
        }
        if (requestCode == REQUEST_PERMISSION_STORAGE) {
            if (grantResults.length > 0) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    KnownDevicesList.loadKnownDeviceNamesFromFile(this, "known_devices.txt");
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }

            }
            else {
                Toast.makeText(this, "grantResults==null", Toast.LENGTH_SHORT).show();
            }
        }


        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveKnownDeviceNamesToFile("known_devices.txt");
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }



        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothDiscovery();
            } else {
                Toast.makeText(this, "Location permission is required for this app", Toast.LENGTH_SHORT).show();

                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the BroadcastReceiver for Bluetooth device discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        // Запускаем таймер для автоматического обновления значений RSSI
        //startRssiUpdateTimer();
    }
    //сохранение в файл
    private void saveKnownDeviceNamesToFile(String fileName) {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, REQUEST_SAVE_FILE);


    }
    private void dell(List<String> list, String oldValue)
    {
        int index = list.indexOf(oldValue);
        if (index != -1) {

            list.remove(index);
        }
    }
    //сканирование ble
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device.getName() != null /*&& device.getAddress() ==i*/) {

                                String deviceMak = device.getAddress();
                                String deviceName = device.getName();
                                if((rssiMap.containsKey(device.getAddress())))
                                {   updateDeviceList(device, rssi, 1);

                                    if(rssiMap.get(deviceMak)<-87) {
                                        int rssi_old = rssiMap.get(deviceMak);
                                        String oldvalue =";" + deviceMak + ";" + rssi_old ;
                                        dell(printt, oldvalue);
                                        rssiMap.remove(deviceMak);
                                        rssiFilterMap.remove(deviceMak);
                                    }
                                }}
                        }
                    });
                }
            };
    @Override
    protected void onPause() {
        super.onPause();
// Unregister the BroadcastReceiver when the activity is paused
        unregisterReceiver(receiver);
    }
    // BroadcastReceiver используется для приема событий обнаружения устройств Bluetooth.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice object and its RSSI value.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                String deviceName = device.getName();
                String deviceAdres = device.getAddress();

                /*for (String i:KnownDevicesList.getKnownDeviceNames()){
                //if(deviceAdres == i)*/
                if (deviceName != null /*&& deviceAdres ==i*/) {
                    //KnownDevicesList.addKnownDeviceName("Beacon2");


                    KnownDevicesList.addKnownDeviceName(deviceName);
                    deviseadres.add(device.getAddress());




                    updateDeviceList(device, rssi, 0);
                    if(rssiMap.get(deviceAdres)<-88)
                    {
                        int rssi_old=rssiMap.get(deviceAdres);
                        String oldvalue=";" +deviceAdres +";" + rssi_old ;
                        dell(printt,oldvalue);
                        rssiMap.remove(deviceAdres);
                        rssiFilterMap.remove(deviceAdres);
                    }


                    //saveKnownDeviceNamesToDCIM("MyDirectory","known_devices.txt");
                    //}

                }
            }
        }
    };

    public  void readFile() {
        FileInputStream fin = null;
        try {

            fin = openFileInput(filename);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fin));

            String line;
            while ((line = bufferedReader.readLine()) != null){

                KnownDevicesList.addKnownDeviceName(line);
                Log.e("MainActivity", "Failedhh to read file: " + line);
            }


        }
        catch(IOException e) {

            Log.e("MainActivity", "Failedhh to read file: " + e.getMessage());
        }
        finally {

            try {
                if (fin != null)
                    fin.close();
            } catch (IOException e) {
                Log.e("MainActivity", "Failedggggg to read file: " + e.getMessage());

            }
        }
    }


    class PointInfo {
        float x;
        float y;
        int color;

        public PointInfo(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }





    class Point1{
        private double x;
        private double y;
        public Point1(double x, double y)
        {
            this.x=x;
            this.y=y;
        }

        public double getX() {
            return x;
        }
        public double getY(){
            return y;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Point1 other = (Point1) obj;
            double epsilon = 1e-6; // Задайте эпсилон в зависимости от требуемой точности
            return Math.abs(other.x - x) <= epsilon && Math.abs(other.y - y) <= epsilon;
        }

        @Override
        public int hashCode() {
            int result = 17;
            long xBits = Double.doubleToLongBits(x);
            long yBits = Double.doubleToLongBits(y);
            result = 31 * result + (int) (xBits ^ (xBits >>> 32));
            result = 31 * result + (int) (yBits ^ (yBits >>> 32));
            return result;
        }
    }
}
