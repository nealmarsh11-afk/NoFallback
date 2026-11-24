package io.nofallback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class CtorHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam p) {
        Object svc = p.thisObject;
        if (svc == null) return;
        Class<?> k = svc.getClass();
        String name = k.getName();
        if (name == null) return;
        if (name.startsWith("com.android.server.wifi")) {
            HookMain.onWifiService(svc, k);
        }
    }
}
