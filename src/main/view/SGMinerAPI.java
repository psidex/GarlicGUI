package main.view;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

public class SGMinerAPI {

    // Takes an already established socket and gets info from SGMiner api
    public static JSONObject pingInfo(SocketObject socket) throws IOException {
        // For some reason the API only responds to 1 request, so a new connection has to be made for each api request
        socket.startConnection("127.0.0.1", 4028);
        String resp = socket.sendMessage("{\"command\": \"summary\"}");
        socket.stopConnection();

        JSONObject api_return = new JSONObject(resp);
        JSONArray api_summary_array = (JSONArray) api_return.get("SUMMARY");
        Iterator api_summary_itr = api_summary_array.iterator();

        // Should only over iterate once
        Object slide = api_summary_itr.next();
        return (JSONObject) slide;
    }

}
