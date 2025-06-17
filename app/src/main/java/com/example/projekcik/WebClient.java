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
    private String ipAddress = "127.0.0.1";

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
                Log.i("Websocket","WebSocket opened");
//                webSocket.send("Hello, WebSocket!");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.i("Websocket", "Msg: " + text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.i("Websocket", "Msg: " + bytes.hex());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.i("Websocket" ,"WebSocket closing: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
                latch.countDown();
            }
        });
        client.dispatcher().executorService().shutdown();
    }

    public void send(@NonNull String str) {
        webSocket.send(str);
    }

    public void close() {
        webSocket.close(1000, "End of recording");
    }

}
