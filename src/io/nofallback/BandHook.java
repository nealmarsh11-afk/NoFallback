package io.nofallback;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class BandHook extends XC_MethodHook {
    private final String tag;

    public BandHook(String tag) { this.tag = tag; }

    @Override
    protected void beforeHookedMethod(MethodHookParam p) {
        Object a = (p.args != null && p.args.length > 0) ? p.args[0] : null;
        if (!(a instanceof Integer) || ((Integer) a).intValue() != 4) return;
        p.setResult(Boolean.TRUE);
        XposedBridge.log("[NoFallback] " + tag + " isSapSupportedBand(band=" + a + ") -> true");
    }
}
