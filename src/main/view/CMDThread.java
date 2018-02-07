package main.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class CMDThread implements Runnable {

    private String cmdString;
    private String exeName;
    private PrintWriter logWriter;
    private boolean loggingEnabled;

    CMDThread(String toExecute, String minerExecutable, String logFileName, boolean loggingEnabledParam) {

        if (loggingEnabledParam) {
            try {
                logWriter = new PrintWriter(logFileName, "UTF-8");
            } catch (IOException e) {
                StacktraceAlert.create(
                        "Log file error",
                        "Cannot create new PrintWriter to " + logFileName,
                        "CMDThread.logWriter threw IOException",
                        e
                );
            }
        }

        cmdString = toExecute;
        exeName = minerExecutable;
        loggingEnabled = loggingEnabledParam;

    }

    public void run(){

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
                if (loggingEnabled) logWriter.close();
            }));

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true) {
                // Read lines from output and make sure everything is still working
                // This is needed for it to work
                String line = r.readLine();
                if (line == null) break;
                // Don't log API info (no point, will just create massive file)
                if (!line.contains("API: ") && !line.trim().equals("") && !line.trim().equals("'") && loggingEnabled) logWriter.println(line);
            }

        }

        catch (IOException e) {
            StacktraceAlert.create("IOException", "Error in miner thread", "IOException in miner thread", e);
        }

    }

}
