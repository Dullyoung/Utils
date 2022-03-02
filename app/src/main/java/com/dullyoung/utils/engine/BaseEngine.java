package com.dullyoung.utils.engine;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.dullyoung.utils.R;
import com.dullyoung.utils.util.CommonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.request.RequestCall;

import java.io.File;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Description : base engine
 *
 * @author Dullyoung
 * Date : 2021/12/2  11:30
 */
public class BaseEngine {
    private final Context mContext;
    private final List<RequestCall> mCallList = new ArrayList<>();

    public BaseEngine(Context context) {
        mContext = context;

        FragmentActivity activity = CommonUtils.findActivity(context);
        if (activity != null) {
            activity.getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                public void onDestroy() {
                    for (RequestCall call : mCallList) {
                        call.cancel();
                    }
                    activity.getLifecycle().removeObserver(this);
                }
            });
        }

    }

    public Context getContext() {
        return mContext;
    }

    private static final String TAG = "BaseHttpEngine";
    private static Map<String, String> defaultParams = new HashMap<>();


    public static void setDefaultParams(Map<String, String> defaultParams) {
        BaseEngine.defaultParams = defaultParams;
    }

    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        String token = "";
        headers.put("token", token);
        headers.put("os", "android");
        return headers;
    }


    private void addDefaultParams(Map<String, String> targetParams) {
        for (String s : defaultParams.keySet()) {
            targetParams.put(s, defaultParams.get(s));
        }
    }

    /**
     * @param url    url
     * @param type   response type
     * @param params param
     * @return Serialized response
     * 加密传输json类型
     */
    private <T> T securePostJson(String url, Type type, Map<String, String> params, boolean secure) {
        if (params == null) {
            params = new HashMap<>();
        }
        addDefaultParams(params);
        JsonObject jsonObject = new JsonObject();
        for (String key : params.keySet()) {
            jsonObject.addProperty(key, params.get(key));
        }
        jsonObject.addProperty("timespan", System.currentTimeMillis());
        String jsonString = jsonObject.toString();
        String secureString = "";

        T resultInfo = null;
        try {
            Log.i(TAG, "-----------------------请求数据-----------------------------");
            Log.i(TAG, "客户端请求地址->" + url);
            Log.i(TAG, "客户端请求头部->" + new Gson().toJson(getHeaders()));
            Log.i(TAG, "客户端请求数据-------->" + jsonString);
            if (secure) {
                //加密数据 secureString = CommonUtil.getEncryptString(jsonString);
                Log.i(TAG, "客户端请求加密数据-------->" + secureString);
            }
            Log.i(TAG, "------------------------请求数据结束----------------------------");
            String finalStr = secure ? secureString : jsonString;
            RequestCall call = OkHttpUtils.postString()
                    .url(url)
                    .headers(getHeaders())
                    .content(finalStr)
                    .mediaType(JSON)
                    .build();
            mCallList.add(call);
            Response response = call.execute();
            String responseString = response.body().string();
            String responseD = "";
            Log.i(TAG, "--------------------------返回数据--------------------------");
            Log.i(TAG, "客户端请求地址->" + url);
            Log.i(TAG, "客户端请求数据->" + jsonString);
            Log.i(TAG, "服务端返回数据->" + responseString);
            if (secure) {
                //解密数据  responseD = CommonUtil.getDecryptString(responseString);

                Log.i(TAG, "服务端返回解密数据->" + responseD);
            }
            Log.i(TAG, "--------------------------返回数据结束-------------------------");
            resultInfo = getResultInfo(secure ? responseD : responseString, type);
        } catch (Exception e) {
            resultInfo = getResultInfo(getDefaultStringOnError(e), type);
            Log.i(TAG, "" + e.toString());
        }
        return resultInfo;
    }

    private String getDefaultStringOnError(Exception e) {
        String msg = "";
        if (e instanceof UnknownHostException
                || e.toString().contains("Your Base Url")
                || e.toString().contains("<head><title>502 Bad Gateway</title></head>")) {
            msg = "网络异常";
        } else {
            msg = e.getMessage().replaceAll("\"", "'");
        }
        JsonObject object = new JsonObject();
        object.addProperty("code", -1);
        object.addProperty("msg", msg);
        return object.toString();
    }

    private <T> T postJson(String url, Type type, String jsonString) {
        T resultInfo = null;
        try {
            Log.i(TAG, "----------------------------------------------------");
            Log.i(TAG, "客户端请求地址->" + url);
            Log.i(TAG, "客户端请求头部->" + new Gson().toJson(getHeaders()));
            Log.i(TAG, "客户端请求数据-------->" + jsonString);
            Log.i(TAG, "----------------------------------------------------");

            RequestCall call = OkHttpUtils.postString()
                    .url(url)
                    .headers(getHeaders())
                    .content(jsonString)
                    .mediaType(JSON)
                    .build();
            mCallList.add(call);
            Response response = call.execute();
            String responseString = response.body().string();
            Log.i(TAG, "--------------------------返回数据--------------------------");
            Log.i(TAG, "客户端请求地址->" + url);
            Log.i(TAG, "客户端请求数据->" + jsonString);
            Log.i(TAG, "服务端返回数据->" + responseString);
            Log.i(TAG, "--------------------------返回数据结束-------------------------");
            resultInfo = getResultInfo(responseString, type);
        } catch (Exception e) {
            resultInfo = getResultInfo(getDefaultStringOnError(e), type);
            Log.i(TAG, "" + e.toString());
        }
        return resultInfo;
    }


    MediaType JSON = MediaType.parse("application/json;charset=utf-8");

    //< 同步请求post 1
    private <T> T post(String url, Type type, @Nullable Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        Log.i(TAG, "------------------------请求数据----------------------------");
        Log.i(TAG, "客户端请求地址->" + url);
        Log.i(TAG, "客户端请求头部->" + new Gson().toJson(getHeaders()));
        Log.i(TAG, "客户端请求数据->" + new Gson().toJson(params));
        Log.i(TAG, "-----------------------请求数据结束---------------------------");
        T resultInfo = null;
        try {
            RequestCall call = OkHttpUtils.post().url(url).headers(getHeaders())
                    .params(params).build();
            mCallList.add(call);
            Response response = call.execute();
            //for Location error when request error
            //Log.i("securityHttp", "客户器返回源数据: " + response.body);
            String responseString = response.body().string();
            Log.i(TAG, "--------------------------返回数据--------------------------");
            Log.i(TAG, "客户端请求地址->" + url);
            Log.i(TAG, "客户端请求数据->" + new Gson().toJson(params));
            Log.i(TAG, "服务端返回数据->" + responseString);
            Log.i(TAG, "---------------------------返回数据结束-------------------------");
            resultInfo = getResultInfo(responseString, type);
        } catch (Exception e) {
            resultInfo = getResultInfo(getDefaultStringOnError(e), type);
            Log.i(TAG, "" + e.toString());
        }
        return resultInfo;
    }

    //< 同步请求post 1
    private <T> T postFile(String url, Type type, Map<String, String> params, Map<String, File> fileMap) {
        if (params == null) {
            params = new HashMap<>();
        }

        Log.i(TAG, "------------------------请求数据----------------------------");
        Log.i(TAG, "客户端请求地址->" + url);
        Log.i(TAG, "客户端请求头部->" + new Gson().toJson(getHeaders()));
        Log.i(TAG, "客户端请求数据->" + new Gson().toJson(params));
        Log.i(TAG, "客户端上传文件->" + new Gson().toJson(fileMap));
        Log.i(TAG, "------------------------请求数据结束---------------------------");

        T resultInfo = null;
        try {
            PostFormBuilder postFormBuilder = OkHttpUtils.post().url(url).headers(getHeaders())
                    .params(params);
            for (String key : fileMap.keySet()) {
                File file = fileMap.get(key);
                if (file != null) {
                    postFormBuilder.addFile(key, file.getName(), file);
                }
            }
            RequestCall call = postFormBuilder.build();
            Response response = call.execute();
            //for Location error when request error
            //Log.i("securityHttp", "返回源数据: " + response.body);
            String responseString = response.body().string();
            Log.i(TAG, "--------------------------返回数据--------------------------");
            Log.i(TAG, "客户端请求地址->" + url);
            Log.i(TAG, "客户端请求数据->" + new Gson().toJson(params));
            Log.i(TAG, "服务端返回数据->" + responseString);
            Log.i(TAG, "---------------------------返回数据结束-------------------------");
            resultInfo = getResultInfo(responseString, type);
        } catch (Exception e) {
            resultInfo = getResultInfo(getDefaultStringOnError(e), type);
            Log.i(TAG, "" + e.toString());
        }
        return resultInfo;
    }

    private <T> T get(String url, Type type, Map<String, String> params) {
        T resultInfo = null;
        try {
            Response response = OkHttpUtils.get().url(url).params(params).build().execute();
            resultInfo = getResultInfo(response.body().string(), type);
        } catch (Exception e) {
            Log.i(TAG, "" + e.toString());
        }
        return resultInfo;
    }

    private <T> T getResultInfo(String body, Type type) {
        T resultInfo;
        if (type != null) {
            resultInfo = new Gson().fromJson(body, type);
        } else {
            resultInfo = new Gson().fromJson(body, new TypeToken<T>() {
            }.getType());
        }
        return resultInfo;
    }


    public <T> Observable<T> rxget(String url, final Type type, final Map<String, String> params) {
        return Observable.just("").map((Func1<Object, T>) o -> get(url, type, params))
                .subscribeOn(Schedulers.newThread()).onErrorReturn(throwable -> {
                    Log.i(TAG, throwable.getMessage());
                    return null;
                });
    }


    public <T> Observable<T> rxPost(String url, Type type, @Nullable final Map<String, String>
            params) {
        return Observable.just("").map((Func1<Object, T>) o -> post(url, type, params))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> {
                    Log.i(TAG, throwable.getMessage());
                    return null;
                });
    }

    public <T> Observable<T> rxPostJson(String url, Type type, String jsonStr) {
        return Observable.just("").map((Func1<Object, T>) o -> postJson(url, type, jsonStr))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.i(TAG, "rxPostJson: " + new Gson().toJson(throwable));
                });

    }

    public <T> Observable<T> rxPostSecureJson(String url, Type type, Map<String, String> params, boolean secure) {
        return Observable.just("").map((Func1<Object, T>) o -> securePostJson(url, type, params, secure))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.i(TAG, "rxPostSecureJson: " + new Gson().toJson(throwable));
                });

    }

    public <T> Observable<T> rxPostFile(String url, Type type, final Map<String, String>
            params, Map<String, File> file) {
        return Observable.just("").map((Func1<Object, T>) o -> postFile(url, type, params, file))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> {
                    Log.i(TAG, throwable.getMessage());
                    return null;
                });
    }
}
