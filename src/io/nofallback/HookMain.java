package io.nofallback;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Minimal discovery plus the MIUI soft-AP band check override. */
public class HookMain implements IXposedHookLoadPackage {
    static final String MIUI_SVC = "com.android.server.wifi.MiuiWifiService";
    static final String AML_SVC = "com.android.server.wifi.AmlMiuiWifiServiceImp";
    static final String[] WIFI_CLASSES = {
            "com.android.server.wifi.WifiService",
            "com.android.server.wifi.WifiServiceImpl",
            "com.android.server.wifi.MiuiWifiServiceImpl"
    };

    static volatile boolean done;
    private static final Set<Class<?>> HOOKED =
            Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        String pkg = lp.packageName == null ? "" : lp.packageName;
        String proc = lp.processName == null ? "" : lp.processName;
        boolean systemServer = "android".equals(pkg) || "system".equals(pkg) || "system_server".equals(pkg)
                || "system_server".equals(proc);
        if (!systemServer) return;

        XposedBridge.log("[NoFallback] active in system_server");
        hookServiceJarEntry(lp.classLoader);
        observeWifiServiceConstructors(lp.classLoader);
    }

    private static void hookServiceJarEntry(ClassLoader cl) {
        try {
            Class<?> manager = XposedHelpers.findClassIfExists(
                    "com.android.server.SystemServiceManager", cl);
            if (manager == null) return;
            for (Method method : manager.getDeclaredMethods()) {
                if ("startServiceFromJar".equals(method.getName())) {
                    XposedBridge.hookMethod(method, new ServiceJarHook());
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("[NoFallback] service entry hook failed: " + t);
        }
    }

    private static void observeWifiServiceConstructors(ClassLoader cl) {
        for (String name : WIFI_CLASSES) {
            Class<?> service = XposedHelpers.findClassIfExists(name, cl);
            if (service == null) continue;
            try {
                XposedHelpers.findAndHookConstructor(service, Context.class, new CtorHook());
            } catch (Throwable t) {
                XposedBridge.log("[NoFallback] Wi-Fi service observation failed: " + t);
            }
        }
    }

    static void onWifiService(Object service, Class<?> serviceClass) {
        ClassLoader cl = serviceClass.getClassLoader();
        if (cl == null) cl = ClassLoader.getSystemClassLoader();
        hookClass(cl, MIUI_SVC);
        hookClass(cl, AML_SVC);
        scanFields(service, serviceClass);
    }

    private static void scanFields(Object service, Class<?> serviceClass) {
        try {
            for (Class<?> c = serviceClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    String typeName = field.getType().getName();
                    if (!typeName.contains("MiuiWifiService") && !typeName.contains("WifiServiceImp")) continue;
                    try {
                        field.setAccessible(true);
                        Object value = field.get(service);
                        hookOn(field.getType());
                        if (value != null) hookOn(value.getClass());
                    } catch (Throwable ignored) {
                        // The service loader probe below may still find the target class.
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("[NoFallback] Wi-Fi service scan failed: " + t);
        }
    }

    static void hookClass(ClassLoader cl, String name) {
        if (cl == null) return;
        try {
            hookOn(Class.forName(name, false, cl));
        } catch (Throwable ignored) {
            // This loader does not contain the MIUI Wi-Fi service.
        }
    }

    static boolean hookOn(Class<?> target) {
        String name = target.getName();
        if (!MIUI_SVC.equals(name) && !AML_SVC.equals(name)) return false;
        synchronized (HOOKED) {
            if (HOOKED.contains(target)) return true;
            try {
                XposedHelpers.findAndHookMethod(
                        target, "isSapSupportedBand", int.class, new BandHook(name));
                HOOKED.add(target);
                done = true;
                XposedBridge.log("[NoFallback] hooked " + name + ".isSapSupportedBand");
                return true;
            } catch (Throwable t) {
                XposedBridge.log("[NoFallback] target hook failed: " + t);
                return false;
            }
        }
    }
}
