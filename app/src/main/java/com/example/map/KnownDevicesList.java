package com.example.map;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class KnownDevicesList {
    private static List<String> knownDeviceNames = new ArrayList<>();
    private static final String fileName = "known_devices.txt";
    public static List<String> getKnownDeviceNames() {
        return knownDeviceNames;
    }

    public static void addKnownDeviceName(String deviceName) {
        if (!knownDeviceNames.contains(deviceName)) {
            knownDeviceNames.add(deviceName);
        }
    }

    public static void loadKnownDeviceNamesFromFile(Context context, String fileName) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File subDir = new File(downloadDir, "VK/Appmy");
        File file = new File(subDir, fileName);

        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            String line;
            while ((line = reader.readLine()) != null) {
                KnownDevicesList.addKnownDeviceName(line);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MainActivity", "Failed to read file: " + e.getMessage());
        }
    }
    public static void readFile() {
        File myFile = new File(Environment.getExternalStorageDirectory().toString() + "/" + fileName);
        try {
            FileInputStream inputStream = new FileInputStream(myFile);
            /*
             * Буфферезируем данные из выходного потока файла
             * Буфферезируем данные из выходного потока файла
             */
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            /*
             * Класс для создания строк из последовательностей символов
             */

            String line;
            try {
                /*
                 * Производим построчное считывание данных из файла в конструктор строки,

                 */
                while ((line = bufferedReader.readLine()) != null){
                    addKnownDeviceName(line);
                    //stringBuilder.append(line);
                }
                //textView.setText(stringBuilder);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("MainActivity", "Failedif to read file: " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("MainActivity", "Failedddddddddd to read file: " + e.getMessage());
        }
    }
    }

