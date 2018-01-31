package main.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class cmdThread implements Runnable {

    private String cmdString;
    private String exeName;
    private PrintWriter logWriter;

    public cmdThread(String to_execute, String minerExecutable, String logFileName) {
        // Allow use in run

        try {
            logWriter = new PrintWriter(logFileName, "UTF-8");
        } catch (IOException e) {
            stacktraceAlert.create("Log file error", "cmdThread.logWriter threw IOException", "Cannot create new PrintWriter", e);
        }

        cmdString = to_execute;
        exeName = minerExecutable;
        System.out.println("exeName: " + exeName);
    }

    public void run(){

        System.out.println("sgminer_cmd_thread started");

        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", cmdString);
        builder.redirectErrorStream(true);

        try {

            Process p = builder.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Kill CMD process
                p.destroy();
                try {
                    // Kill miner executable (the CMD processes child)
                    Runtime.getRuntime().exec("taskkill /f /t /im " + exeName);
                } catch (IOException e) {
                    // Do nothing
                }
                // Finally, close log file
                logWriter.close();
            }));

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true) {
                // Read lines from output and make sure everything is still working
                // This is needed for it to work
                String line = r.readLine();
                if (line == null) break;
                // Don't log API info (no point, will just create massive file)
                if (!line.contains("API: ") && !line.trim().equals("") && !line.trim().equals("'")) logWriter.println(line);
            }

        }

        catch (IOException e) {
            stacktraceAlert.create("IOException", "Error in sgminer.exe thread", "IOException in sgminer.exe thread", e);
        }

    }

}
