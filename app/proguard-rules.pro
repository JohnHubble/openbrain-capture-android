# Keep JNI native method declarations and the classes that hold them so the
# C symbol names in app/src/main/cpp/jni.c continue to bind at load time.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# whisper.cpp JNI shim: the JNIEXPORT names target this exact class/companion.
-keep class com.whispercpp.whisper.WhisperLib { *; }
-keep class com.whispercpp.whisper.WhisperLib$* { *; }

# Tink (used transitively by androidx.security.crypto) references error-prone
# annotations that are compile-time only and not on the runtime classpath.
-dontwarn com.google.errorprone.annotations.**
