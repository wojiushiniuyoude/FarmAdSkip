package io.github.huajiqaq.farmadskip;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class MainHook implements IXposedHookLoadPackage {
    private static final Set<String> HOOKED_CLASSES = new CopyOnWriteArraySet<>();
    private static final String[] TARGET_PACKAGES = {"com.UCMobile", "com.quark.browser"};

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!isTargetPackage(lpparam)) return;
        hookRewardedVideoAd(lpparam);
    }

    private boolean isTargetPackage(LoadPackageParam lpparam) {
        String pkg = lpparam.packageName;
        for (String target : TARGET_PACKAGES) {
            if (target.equals(pkg) && target.equals(lpparam.processName)) {
                return true;
            }
        }
        return false;
    }

    private void hookRewardedVideoAd(LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookConstructor(
                    "com.noah.api.RewardedVideoAd",
                    lpparam.classLoader,
                    "com.noah.api.RewardedVideoAd$AdListener",
                    "com.noah.remote.IRewardAdRemote",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object listener = param.args[0];
                            if (listener != null) {
                                hookAdListener(listener.getClass().getName(), lpparam);
                            }
                        }
                    }
            );
            log("✓ 已Hook RewardedVideoAd构造函数");
        } catch (Throwable t) {
            log("✗ Hook构造函数失败: " + t.getMessage());
        }
    }

    private void hookAdListener(String className, LoadPackageParam lpparam) {
        if (!HOOKED_CLASSES.add(className)) return;

        try {
            XposedHelpers.findAndHookMethod(className,
                    lpparam.classLoader,
                    "onVideoStart",
                    "com.noah.api.RewardedVideoAd",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            triggerReward(param);
                        }
                    }
            );
            log("✓ 已Hook监听器: " + className);
        } catch (Throwable t) {
            log("✗ Hook监听器失败: " + t.getMessage());
        }
    }

    private void triggerReward(XC_MethodHook.MethodHookParam param) {
        try {
            XposedHelpers.callMethod(param.thisObject, "onRewarded", param.args[0]);
            log("✓ 已触发奖励");
        } catch (Throwable t) {
            log("✗ 触发奖励失败: " + t.getMessage());
        }
    }

    private void log(String msg) {
        XposedBridge.log("[FarmSkip] " + msg);
    }
}