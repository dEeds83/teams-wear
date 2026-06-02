# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class de.streamonkey.teamswear.** {
    kotlinx.serialization.KSerializer serializer(...);
}
