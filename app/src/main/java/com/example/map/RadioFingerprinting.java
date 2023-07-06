package com.example.map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
public class RadioFingerprinting {
    // Здесь предполагается, что у вас есть база данных с известными значениями RSSI для каждой точки
    private Map<Point, Map<String, Integer>> radioMap;

    public RadioFingerprinting(String filePath) {
        radioMap = new HashMap<>();
        readDataFromFile(filePath);
    }
    private void readDataFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                int numMeasurements = tokens.length / 2;
                Point location = parsePoint(tokens[tokens.length - 1]);

                Map<String, Integer> rssiMap = new HashMap<>();
                for (int i = 0; i < numMeasurements; i++) {
                    String macAddress = tokens[i * 2];
                    int rssiValue = Integer.parseInt(tokens[i * 2 + 1]);
                    rssiMap.put(macAddress, rssiValue);
                }

                radioMap.put(location, rssiMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Point parsePoint(String pointString) {
        int startIndex = pointString.indexOf('(');
        int commaIndex = pointString.indexOf(',');
        int endIndex = pointString.indexOf(')');
        double x = Double.parseDouble(pointString.substring(startIndex + 1, commaIndex).trim());
        double y = Double.parseDouble(pointString.substring(commaIndex + 1, endIndex).trim());
        return new Point(x, y);
    }
    public List<Map.Entry<Point, Double>> getLocation(Map<String, Integer> measuredRSSI) {
        List<Map.Entry<Point, Double>> bestMatchLocations = new ArrayList<>();

        // Перебираем все известные точки в базе данных
        for (Map.Entry<Point, Map<String, Integer>> entry : radioMap.entrySet()) {
            Point location = entry.getKey();
            Map<String, Integer> knownRSSIMap = entry.getValue();

            // Вычисляем расстояние между измеренным значением RSSI и известным значением RSSI для каждой точки
            double distance = calculateDistance(measuredRSSI, knownRSSIMap);

            // Создаем новую запись (Entry) для точки и расстояния и добавляем ее в список
            Map.Entry<Point, Double> distanceEntry = new AbstractMap.SimpleEntry<>(location, distance);
            bestMatchLocations.add(distanceEntry);
        }

        // Сортируем список местоположений на основе расстояния
        Collections.sort(bestMatchLocations, new Comparator<Map.Entry<Point, Double>>() {
            public int compare(Map.Entry<Point, Double> entry1, Map.Entry<Point, Double> entry2) {
                return Double.compare(entry1.getValue(), entry2.getValue());
            }
        });

        return bestMatchLocations;
    }
    public Point getWeightedLocation(List<Map.Entry<Point, Double>> bestMatchLocations) {
        double totalWeight = 0.0;
        double weightedX = 0.0;
        double weightedY = 0.0;
        int count = 0;
        // Вычисляем общий вес и взвешенные суммы координат X и Y
        for (Map.Entry<Point, Double> entry : bestMatchLocations) {

            if (count < 3) {
                Point location = entry.getKey();
                double distance = entry.getValue();
                double weight = 1.0 / distance;
                if (distance == 0) {
                    totalWeight = 0;
                    weightedX = location.getX();
                    weightedY = location.getY();
                    break;
                }
                totalWeight += weight;
                weightedX += weight * location.getX();
                weightedY += weight * location.getY();
                count++;
            }
            else {
                break;
            }

        }

        // Нормализуем веса
        if (totalWeight != 0.0) {
            weightedX /= totalWeight;
            weightedY /= totalWeight;
        }

        return new Point(weightedX, weightedY);
    }



    // Метод для вычисления расстояния между измеренным значением RSSI и известным значением RSSI
    private double calculateDistance(Map<String, Integer> measuredRSSIMap, Map<String, Integer> knownRSSIMap) {
        double distance = 0.0;

        for (Map.Entry<String, Integer> knownEntry : knownRSSIMap.entrySet()) {
            String macAddress = knownEntry.getKey();
            int knownValue = knownEntry.getValue();

            // Если ключ отсутствует в measuredRSSIMap, добавляем его со значением -1000
            if (measuredRSSIMap.containsKey(macAddress)) {
                int measuredValue = measuredRSSIMap.get(macAddress);
                distance += Math.abs(measuredValue - knownValue);
            }

            //int measuredValue = measuredRSSIMap.get(macAddress);

            // Выполняйте необходимые вычисления или алгоритмы для получения расстояния
            // В этом примере мы просто суммируем разницы между измеренными и известными значениями RSSI
            //distance += Math.abs(measuredValue - knownValue);
        }

        return distance;
    }

    // Вспомогательный метод для создания карты значений RSSI
    private Map<String, Integer> createRSSIMap(int... values) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            map.put("AP" + (i + 1), values[i]);
        }
        return map;
    }

    public static void main(String[] args) {

    }
    class Point{
        private double x;
        private double y;
        public Point(double x, double y)
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
    }
}
