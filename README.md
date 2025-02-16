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
    implementation("com.github.nonanerz:sddl-android-sdk:1.0.11")
}
```

> Replace `1.0.11` with the latest release version.

## Usage

### Initialize SDK
In your `MainActivity.kt`:

```kotlin
package com.simplelink.android_test

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.gson.JsonObject
import com.simplelink.sddl_sdk.SDDLSDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataUri: Uri? = intent?.data
        Log.d("MainActivity", "Received URI: $dataUri")

        SDDLSDK.fetchDetails(this, dataUri, "yourCustomScheme", object : SDDLSDK.SDDLCallback {
            override fun onSuccess(data: JsonObject) {
                Log.d("SDDLSDK", "Success: $data")
            }

            override fun onError(error: String) {
                Log.e("SDDLSDK", "Error: $error")
            }
        })
    }
}
```

### Custom URI Scheme Support
If you want to use a custom URI scheme, pass it as the third parameter to `fetchDetails`:

```kotlin
SDDLSDK.fetchDetails(this, null, "mycustomscheme", callback)
```

### AndroidManifest.xml
Add permissions and your custom scheme to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />

<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="mycustomscheme" />
    </intent-filter>
</activity>
```

## License
This SDK is licensed under the MIT License.

---

For more details, visit [GitHub Repository](https://github.com/nonanerz/sddl-android-sdk).
