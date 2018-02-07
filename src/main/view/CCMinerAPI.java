package main.view;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CCMinerAPI {

    // Takes an already established socket and gets info from CCMiner api
    public static Map<String, String> pingInfo(SocketObject socket) throws IOException {
        socket.startConnection("127.0.0.1", 4028);
        String resp = socket.sendMessage("summary");
        socket.stopConnection();

        Map<String, String> api_return = new HashMap<>();
        String[] splitResp = resp.split(";");

        for (String arg : splitResp) {
            String[] splitArg = arg.split("=");

            switch (splitArg[0]) {
                case "KHS":
                    api_return.put("KHS", splitArg[1]);
                    break;
                case "ACC":
                    api_return.put("Accepted", splitArg[1]);
                    break;
                case "REJ":
                    api_return.put("Rejected", splitArg[1]);
                    break;
                case "UPTIME":
                    api_return.put("Uptime", splitArg[1]);
                    break;
            }

        }

        return api_return;
    }

}
