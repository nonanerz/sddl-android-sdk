# SDDL Android SDK

This is the official Android SDK for SDDL, providing seamless integration with deferred deep links.

## Integration with JitPack

### Step 1: Add JitPack Repository
Add the JitPack repository to your project-level `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### Step 2: Add Dependency
In your app-level `build.gradle.kts`, add the dependency:

```kotlin
dependencies {
    implementation("com.github.nonanerz:sddl-android-sdk:1.0.0")
}
```

> Replace `1.0.0` with the latest release version.

## Usage

### Initialize SDK
In your `MainActivity.kt`:

```kotlin
import com.simplelink.sddl_sdk.SDDLSDK

// Call this method to fetch details
SDDLSDK.fetchDetails(this, intent?.data) { result ->
    when (result) {
        is SDDLSDK.Success -> {
            // Handle success
            val data = result.data
        }
        is SDDLSDK.Error -> {
            // Handle error
            val errorMessage = result.message
        }
    }
}
```

### AndroidManifest.xml
Add permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## License
This SDK is licensed under the MIT License.

---

For more details, visit [GitHub Repository](https://github.com/nonanerz/sddl-android-sdk).

