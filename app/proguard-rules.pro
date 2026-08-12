# GeckoView reaches into these reflectively from native code.
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keepclassmembers class * {
    @org.mozilla.gecko.annotation.WrapForJNI *;
}

# Android Components use kotlinx.serialization + reflection in a few places.
-keep class mozilla.components.concept.engine.** { *; }
-keep class mozilla.components.browser.engine.gecko.** { *; }

-dontwarn org.mozilla.**
-dontwarn mozilla.components.**
