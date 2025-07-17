package com.example.projekcik;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import java.util.concurrent.CountDownLatch;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.OkHttpClient;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebClient{
    private final OkHttpClient client;
    private WebSocket webSocket;
    private final CountDownLatch latch;
    private String ipAddress = "192.168.0.137";

    public WebClient(String ipAddress) {
        client = new OkHttpClient();
        latch = new CountDownLatch(1);
        this.ipAddress = ipAddress;
    }

    public void start() {
        Request request = new Request.Builder()
                .url(String.format("ws://%s:8765", ipAddress))
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.i("Websocket", "Połączono z serwerem");
//                webSocket.send("Dane z kamery: Hello from Android!");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.i("Websocket", "Otrzymano: " + text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.i("Websocket", "Zamykanie: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("Websocket", "Błąd połączenia: " + t.getMessage(), t);
                latch.countDown();
            }
        });
    }


    public void send(@NonNull String str) {
        webSocket.send(str);
    }

    public void close() {
        webSocket.close(1000, "End of recording");
        client.dispatcher().executorService().shutdown();
    }

}
