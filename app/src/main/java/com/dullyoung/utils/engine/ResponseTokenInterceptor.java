package com.dullyoung.utils.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Description :
 *
 * @author Dullyoung
 * Date : 2022-02-24  10:58
 *
 * <pre>
 *       val okHttpClient: OkHttpClient =
 *             OkHttpClient.Builder()
 *                 .connectTimeout(20000L, TimeUnit.MILLISECONDS)
 *                 .callTimeout(40000L, TimeUnit.MILLISECONDS)
 *                 .readTimeout(20000L, TimeUnit.MILLISECONDS) //其他配置
 *                 .addInterceptor(ResponseTokenInterceptor())
 *                 .build()
 *
 *         OkHttpUtils.initClient(okHttpClient)
 * </pre>
 */
public class ResponseTokenInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        //  this.logForRequest(request);
        Response response = chain.proceed(request);
        return this.resultCodeForResponse(response);
    }

    private Request logForRequest(Request request) {
        return request;
    }

    private Response resultCodeForResponse(Response response) {
        try {
            if (response.isSuccessful()) {
                if (response.request().url().toString().startsWith("your base url")) {
                    Response.Builder builder = response.newBuilder();
                    Response clone = builder.build();
                    ResponseBody body = clone.body();
                    if (body != null) {
                        MediaType mediaType = body.contentType();
                        if (mediaType != null) {
                            if (this.isText(mediaType)) {
                                String resp = body.string();
                                JsonObject jsonObject = new Gson().fromJson(resp, JsonObject.class);
                                int code = jsonObject.get("code").getAsInt();
                                //需要全局拦截单独处理的code 如他方登录挤下线等
                                int yourNeedResolveCode = 0;
                                if (code == yourNeedResolveCode) {
                                    //do sendEvent
                                }
                                body = ResponseBody.create(mediaType, jsonObject.toString());
                                return response.newBuilder().body(body).build();
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {

        }
        return response;
    }

    private boolean isText(@Nullable MediaType mediaType) {
        if (mediaType != null && mediaType.type().equals("text")) {
            return true;
        } else {
            return mediaType != null && (mediaType.subtype().equals("json") || mediaType.subtype().equals("xml") || mediaType.subtype().equals("html") || mediaType.subtype().equals("webviewhtml"));
        }
    }
}

