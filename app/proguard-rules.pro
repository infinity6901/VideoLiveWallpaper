# ProGuard / R8 rules for VideoLiveWallpaper

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Android service and activity entry points
-keep public class com.infinity.videowallpaper.VideoWallpaperApp
-keep public class com.infinity.videowallpaper.MainActivity
-keep public class com.infinity.videowallpaper.WallpaperSettingsActivity
-keep public class com.infinity.videowallpaper.service.VideoWallpaperService
-keep public class com.infinity.videowallpaper.service.WallpaperQuickTileService

# Keep Media3 / ExoPlayer reflection hooks
-dontwarn androidx.media3.**
