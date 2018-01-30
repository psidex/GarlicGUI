package main.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class cmdThread implements Runnable {

    public static String cmd_string;

    public cmdThread(String to_execute) {
        // Allow use in run
        cmd_string = to_execute;
    }

    public void run(){

        System.out.println("sgminer_cmd_thread started");

        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", cmd_string);
        builder.redirectErrorStream(true);

        try {

            Process p = builder.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Kill CMD process
                p.destroy();
                try {
                    // ToDo: Kill richard & ccminer.exe - maybe kill by using exe name from path selection
                    // Kill miner executable (the CMD processes child)
                    Runtime.getRuntime().exec("taskkill /f /t /im sgminer.exe");
                } catch (IOException e) {
                    // Do nothing
                }
            }));

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true) {
                // Read lines from output and make sure everything is still working
                // This is needed for it to work
                if (r.readLine() == null) break;

                // Debugging - print output from child process
                // // String line = r.readLine();
                // // if (line == null) { break; }
                // // System.out.println("DEBUG: " + line);
            }

        }

        catch (IOException e) {
            stacktraceAlert.create("IOException", "Error in sgminer.exe thread", "IOException in sgminer.exe thread", e);
        }

    }

}
