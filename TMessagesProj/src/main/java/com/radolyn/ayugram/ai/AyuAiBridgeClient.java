/*
 * AyuGram Android AI bridge client.
 */

package com.radolyn.ayugram.ai;

import android.text.TextUtils;

import com.radolyn.ayugram.AyuConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AyuAiBridgeClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface ResultCallback {
        void onSuccess(String text);

        void onError(Exception error);
    }

    public static void requestReply(String userPrompt, ResultCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", AyuConfig.getAiBridgeModel());

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", AyuConfig.getAiSystemPrompt()));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", userPrompt));
            body.put("messages", messages);
            body.put("temperature", 0.7);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(chatCompletionsUrl())
                    .post(RequestBody.create(body.toString(), JSON));

            String apiKey = AyuConfig.getAiBridgeApiKey();
            if (!TextUtils.isEmpty(apiKey)) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            client.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            callback.onError(new IOException("AI bridge HTTP " + response.code() + ": " + responseBody));
                            return;
                        }
                        callback.onSuccess(extractText(responseBody));
                    } catch (Exception e) {
                        callback.onError(e);
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            FileLog.e(e);
            callback.onError(e);
        }
    }

    private static String chatCompletionsUrl() {
        String base = AyuConfig.getAiBridgeUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }

    private static String extractText(String responseBody) throws Exception {
        JSONObject root = new JSONObject(responseBody);
        if (root.has("choices")) {
            JSONArray choices = root.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject first = choices.getJSONObject(0);
                if (first.has("message")) {
                    JSONObject message = first.getJSONObject("message");
                    return message.optString("content", "");
                }
                return first.optString("text", "");
            }
        }
        return root.optString("output_text", "");
    }
}
