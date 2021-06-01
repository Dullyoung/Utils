package com.dullyoung.utils.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.dullyoung.utils.R;
import com.dullyoung.utils.view.MyItemDivider;

/*
 *  Created  in  2021/6/1
 */
public class CommonUtils {
    public static void setItemDivider(Context context, RecyclerView recyclerView) {
        MyItemDivider myItemDivider = new MyItemDivider(context, DividerItemDecoration.VERTICAL)
                .setPadding(ScreenUtil.dip2px(context, 15))
                .setDrawable(ContextCompat.getDrawable(context, R.drawable.shape_line_divide))
                .setCountNotDraw(1);
        recyclerView.addItemDecoration(myItemDivider);
    }
    public static boolean isActivityDestory(Context context) {
        Activity activity = findActivity(context);
        return activity == null || activity.isFinishing() || (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed());
    }


    public static Activity findActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }
}
