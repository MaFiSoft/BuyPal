# gemini
# Add project specific ProGuard rules here.
# By default, Android Studio generates a ProGuard file with a few useful rules.
# You can add more rules to this file to fine-tune the obfuscation process.

# See https://developer.android.com/studio/build/shrink-code for more information.

# If you use Room Persistence Library, uncomment the following rule
-keep class androidx.room.** { *; }
-keep class org.sqlite.database.sqlite.** { *; }

# For Kotlin reflection (if you use it)
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }

# Keep all annotations
-keepattributes Signature,Exceptions,InnerClasses,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# Keep all classes that extend Application
-keep public class * extends android.app.Application {
    <init>(android.content.Context);
}

# Keep all methods of Activity, Fragment, Service, BroadcastReceiver, ContentProvider
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep methods that might be called from JNI
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep all public methods in classes that extend View
-keep public class * extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
    void set*(...);
}

# Keep all public methods of classes that implement a Listener
-keepclasseswithmembers class * implements android.view.View.OnClickListener {
    void onClick(android.view.View);
}
-keepclasseswithmembers class * implements android.widget.AdapterView.OnItemClickListener {
    void onItemClick(android.widget.AdapterView,android.view.View,int,long);
}

# Keep members of Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep members of Serializable classes
-keepnames class * implements java.io.Serializable

# For Dagger or Hilt (if you use them)
# -keep class **.Dagger*
# -keep class **.*_Factory
# -keep class **.*_MembersInjector
# -keep class **.*_Provide*Factory

# For Data Binding (if you use it)
# -keep public class * extends androidx.databinding.ViewDataBinding {
#     public <fields>;
#     public <methods>;
# }

# For LiveData (if you use it)
-keep class androidx.lifecycle.LiveData { *; }

# For Jetpack Compose
# The Compose compiler plugin adds ProGuard rules automatically, but sometimes
# manual rules are needed for specific cases or custom Composables.
# For debugging Compose issues with ProGuard:
# -keepattributes Annotation
# -keep class * extends androidx.compose.ui.tooling.preview.PreviewParameterProvider
# -keep class * implements androidx.compose.runtime.RecomposeScope

# For GSON or other JSON libraries (if you use them)
# -keep class com.google.gson.reflect.TypeToken { *; }
# -keep class * implements com.google.gson.TypeAdapter

# Required for OkHttp and Retrofit (if you use them)
# -dontwarn okio.**
# -dontwarn retrofit2.**
# -dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# For Firebase (if you use it)
# -keep class com.google.firebase.** { *; }
# -keep class com.google.android.gms.** { *; }

# For other libraries, check their documentation for specific ProGuard rules.
