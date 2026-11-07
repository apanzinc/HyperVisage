# Xposed 模块入口不能被裁剪，且要跟随混淆改名同步 java_init.list
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowobfuscation,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>(...);
    public void onModuleLoaded(...);
    public void onPackageLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}
-keepclassmembers,allowoptimization class ** implements io.github.libxposed.api.XposedInterface$Hooker {
    public *** intercept(...);
}

# Hooker 树依赖反射/日志可读性，保留类名
-keepnames class * extends dev.apanzinc.hypervisage.hook.base.BaseHooker

# Koin
-keepnames class org.koin.** { *; }

# KavaRef / 隐藏 API
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn org.lsposed.hiddenapibypass.**

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
