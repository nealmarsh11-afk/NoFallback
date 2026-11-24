package de.robv.android.xposed;
public final class XposedHelpers {
    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) { return null; }
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) { return null; }
    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) { return null; }
    public static Class<?> findClassIfExists(String className, ClassLoader cl) { return null; }
    public static Object callMethod(Object obj, String methodName, Object... args) { return null; }
}
