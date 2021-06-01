package com.dullyoung.utils.engine;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import com.zhy.http.okhttp.OkHttpUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/*
 *  Created by Dullyoung in  2021/4/23
 */
public class BaseEngine<T> {
    private static final String TAG = "HttpEngine";
    private static Map<String, String> defaultParams = new HashMap<>();

    public static void setDefaultParams(Map<String, String> defaultParams) {
        BaseEngine.defaultParams = defaultParams;
    }

    private void addDefaultParams(Map<String, String> targetParams) {
        for (String s : defaultParams.keySet()) {
            targetParams.put(s, defaultParams.get(s));
        }
    }

    //< 同步请求post 1
    private T post(String url, Type type, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        addDefaultParams(params);
        Log.i(TAG,"客户端请求地址-------->" + url);
        Log.i(TAG,"客户端请求数据-------->" + JSONObject.toJSONString(params));
        T resultInfo = null;
        try {
            Response response = OkHttpUtils.post().url(url)
                    .params(params).build().execute();
            //for Location error when request error
            //Log.i("securityHttp", "客户器返回源数据: " + response.body);
            String responseString = response.body().string();
            Log.i(TAG,responseString);
            resultInfo = getResultInfo(responseString, type);
        } catch (Exception e) {
            String body = "{\"code\":500, \"message\":\"" + e.getMessage().replaceAll("\"", "'") + "\"}";
            resultInfo = getResultInfo(body, type);
            Log.i(TAG,"异常->" + e);
        }
        return resultInfo;
    }

    //< 同步请求post 1
    private T postFile(String url, Type type, Map<String, String> params, File file) {
        if (params == null) {
            params = new HashMap<>();
        }
        addDefaultParams(params);
        Log.i(TAG,"客户端请求地址-------->" + url);
        Log.i(TAG,"客户端请求数据-------->" + JSONObject.toJSONString(params));
        Log.i(TAG,"客户端上传文件地址-------->" + file.getAbsolutePath());
        T resultInfo = null;
        try {
            String fileName = "pic-" + System.currentTimeMillis() +
                    file.getName().substring(file.getName().lastIndexOf("."));
            Response response = OkHttpUtils.post().url(url).addFile("file", fileName, file)
                    .params(params).build().execute();
            //for Location error when request error
            //Log.i("securityHttp", "返回源数据: " + response.body);
            String responseString = response.body().string();
            Log.i(TAG,responseString);
            resultInfo = getResultInfo(responseString, type);
        } catch (Exception e) {
            String body = "{\"code\":500, \"message\":\"" + e.getMessage().replaceAll("\"", "'") + "\"}";
            resultInfo = getResultInfo(body, type);
            Log.i(TAG,"异常->" + e);
        }
        return resultInfo;
    }

    private T get(String url, Type type, Map<String, String> params) {
        T resultInfo = null;
        try {
            Response response = OkHttpUtils.get().url(url).params(params).build().execute();
            resultInfo = getResultInfo(response.body().string(), type);
        } catch (Exception e) {
            Log.i(TAG,"异常->" + e);
        }
        return resultInfo;
    }

    private T getResultInfo(String body, Type type) {
        T resultInfo;
        if (type != null) {
            resultInfo = JSON.parseObject(body, type);
        } else {
            resultInfo = JSON.parseObject(body, new TypeReference<T>() {
            });
        }
        return resultInfo;
    }


    public Observable<T> rxget(String url, final Type type, final Map<String, String> params) {
        return Observable.just("").map(new Func1<Object, T>() {
            @Override
            public T call(Object o) {
                return get(url, type, params);
            }
        }).subscribeOn(Schedulers.newThread()).onErrorReturn(new Func1<Throwable, T>() {
            @Override
            public T call(Throwable throwable) {
                Log.i(TAG,throwable.getMessage());
                return null;
            }
        });
    }


    public Observable<T> rxpost(String url, Type type, final Map<String, String>
            params) {
        return Observable.just("").map((Func1<Object, T>) o -> post(url, type, params))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> {
                    Log.i(TAG,throwable.getMessage());
                    return null;
                });
    }

    public Observable<T> rxpostFile(String url, Type type, final Map<String, String>
            params, File file) {
        return Observable.just("").map((Func1<Object, T>) o -> postFile(url, type, params, file))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> {
                    Log.i(TAG,throwable.getMessage());
                    return null;
                });
    }

}
