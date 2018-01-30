package main.view;

import java.io.*;

import java.util.Map;

// http://www.tutorialspoint.com/java/java_serialization.htm was useful

public class settings {

    private static String settingsPath = "settings/settings.ser";

    @SuppressWarnings("unchecked")  // mapObj will create stacktraceAlert if not correct type
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
            System.out.println("Serialized settings map loaded");
        } catch (IOException | ClassNotFoundException e) {
            new stacktraceAlert().create("Exception occurred", "Does resources/settings/settings.ser exist?", "Exception in settings.getSettings", e);
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
            System.out.println("Serialized settings map saved");
        } catch (IOException e) {
            new stacktraceAlert().create("Exception occurred", "Does resources/settings/settings.ser exist?", "Exception in settings.setSettings", e);
        }
    }
}
