-dontwarn android.support.**
-dontwarn org.blinkenlights.jid3.**

-keep class me.wcy.cfix.lib.** { *; }

# glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}