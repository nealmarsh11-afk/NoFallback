package io.nofallback;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Observes only jar-backed service startup and probes loaders associated with Wi-Fi. */
public class ServiceJarHook extends XC_MethodHook {
    @Override
    protected void afterHookedMethod(MethodHookParam p) {
        try {
            inspect(p);
        } catch (Throwable t) {
            // Instrumentation must never change whether system_server starts a service.
            XposedBridge.log("[NoFallback] service startup inspection failed: " + t);
        }
    }

    private static void inspect(MethodHookParam p) {
        StringBuilder args = new StringBuilder();
        boolean wifi = false;
        if (p.args != null) {
            for (Object arg : p.args) {
                if (arg instanceof String) {
                    String s = (String) arg;
                    if (s.contains("wifi") || s.contains("Wifi")) {
                        wifi = true;
                    }
                    if (args.length() > 0) args.append(" | ");
                    args.append(s);
                }
            }
        }
        Object result = p.getResult();
        if (result != null && result.getClass().getName().contains("wifi")) wifi = true;
        if (!wifi) return;

        XposedBridge.log("[NoFallback] startServiceFromJar args=" + args
                + " result=" + (result == null ? "null" : result.getClass().getName()));

        for (Object arg : p.args == null ? new Object[0] : p.args) {
            if (arg instanceof ClassLoader) probe((ClassLoader) arg, "argument");
        }
        if (result != null) {
            Class<?> c = result.getClass();
            probe(c.getClassLoader(), "result:" + c.getName());
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    if (!f.getType().getName().contains("wifi")) continue;
                    try {
                        f.setAccessible(true);
                        Object value = f.get(result);
                        if (value != null) probe(value.getClass().getClassLoader(), "field:" + f.getName());
                    } catch (Throwable t) {
                        XposedBridge.log("[NoFallback] field probe failed " + f.getName() + ": " + t);
                    }
                }
            }
        }
    }

    private static void probe(ClassLoader cl, String source) {
        if (cl == null || HookMain.done) return;
        for (String name : new String[] {HookMain.MIUI_SVC, HookMain.AML_SVC}) {
            try {
                Class<?> c = Class.forName(name, false, cl);
                if (HookMain.hookOn(c)) {
                    XposedBridge.log("[NoFallback] found " + name + " from " + source);
                    return;
                }
            } catch (Throwable ignored) {
                // This loader is not the MIUI Wi-Fi service loader.
            }
        }
    }
}
