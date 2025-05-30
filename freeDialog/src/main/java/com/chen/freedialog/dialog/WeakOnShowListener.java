package com.chen.freedialog.dialog;

import android.content.DialogInterface;

import java.lang.ref.WeakReference;

public class WeakOnShowListener implements DialogInterface.OnShowListener {
    private WeakReference<DialogInterface.OnShowListener> mRef;

    public WeakOnShowListener(DialogInterface.OnShowListener real) {
        this.mRef = new WeakReference<>(real);
    }


    @Override
    public void onShow(DialogInterface dialog) {
        DialogInterface.OnShowListener real = mRef.get();
        if (real != null) {
            real.onShow(dialog);
        }
    }
    public static WeakOnShowListener proxy(DialogInterface.OnShowListener real) {
        return new WeakOnShowListener(real);
    }

}