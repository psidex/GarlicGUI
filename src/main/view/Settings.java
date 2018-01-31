package main.view;

import java.io.*;

import java.util.Map;

// http://www.tutorialspoint.com/java/java_serialization.htm was useful

public class Settings {

    private static String settingsPath = "Settings/Settings.ser";

    @SuppressWarnings("unchecked")  // mapObj will create StacktraceAlert if not correct type
    public static Map<String, String> getSettings() {
        Map<String, String> mapObj = null;
        try {
            FileInputStream fileIn = new FileInputStream(new File(settingsPath));
            ObjectInputStream in = new ObjectInputStream(fileIn);

            Object inObj = in.readObject();
            if (inObj instanceof Map) mapObj = (Map<String, String>) inObj;
            else throw new IOException(settingsPath + ": incorrect file contents");

            in.close();
            fileIn.close();
            System.out.println("Serialized Settings map loaded");
        } catch (IOException | ClassNotFoundException e) {
            StacktraceAlert.create("Exception occurred", "Does Settings/Settings.ser exist?", "Exception in Settings.getSettings", e);
        }
        return mapObj;
    }

    public static void setSettings(Map<String, String> mapObj) {
        try {
            FileOutputStream fileOut = new FileOutputStream(settingsPath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(mapObj);
            out.close();
            fileOut.close();
            System.out.println("Serialized Settings map saved");
        } catch (IOException e) {
            StacktraceAlert.create("Exception occurred", "Does Settings/Settings.ser exist?", "Exception in Settings.setSettings", e);
        }
    }
}
