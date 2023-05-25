package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;


public class CommunicationThread extends Thread {

    private final ServerThread serverThread;
    private final Socket socket;

    // Constructor of the thread, which takes a ServerThread and a Socket as parameters
    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    // run() method: The run method is the entry point for the thread when it starts executing.
    // It's responsible for reading data from the client, interacting with the server,
    // and sending a response back to the client.
    @Override
    public void run() {
        // It first checks whether the socket is null, and if so, it logs an error and returns.
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            // Create BufferedReader and PrintWriter instances for reading from and writing to the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client !");

            // Read the city and informationType values sent by the client
            String key = bufferedReader.readLine();
            String value = bufferedReader.readLine();
            String method = bufferedReader.readLine();
            if (key == null || key.isEmpty() || value == null || value.isEmpty() || method == null || method.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client !");
                return;
            }

            // It checks whether the serverThread has already received the weather forecast information for the given city.
            HashMap<String, DataModel> data = serverThread.getData();
            DataModel dataModel = null;

            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
            HttpClient httpClient = new DefaultHttpClient();
            String pageSourceCode = "";

            // make the HTTP request to the web service
            HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS);
            HttpResponse httpGetResponse = httpClient.execute(httpGet);
            HttpEntity httpGetEntity = httpGetResponse.getEntity();

            if (httpGetEntity != null) {
                pageSourceCode = EntityUtils.toString(httpGetEntity);
            }
            if (pageSourceCode == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                return;
            } else Log.i(Constants.TAG, pageSourceCode);

            JSONObject content = new JSONObject(pageSourceCode);
            String time = content.getString("unixtime");

            String result = "none";
            if("PUT".equals(method)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] PUT to the server...");
                serverThread.setData(key, new DataModel(value, time));
                result = "put successful";
            } else {
                if (data.containsKey(key)) {
                    Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the server...");
                    dataModel = data.get(key);

                    Log.i(Constants.TAG, "[COMMUNICATION THREAD] time is: " + time + " and dataModel time is: " + dataModel.getTime());

                    if(Long.parseLong(time) - Long.parseLong(dataModel.getTime()) > 10) {
                        result = "expired\n";
                    } else {
                        result = dataModel.getValue();
                    }
                } else {
                    result = "none\n";
                }
            }

            // Send the result back to the client
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException | JSONException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}
