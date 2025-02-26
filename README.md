# SDDL Android SDK

This is the official Android SDK for SDDL, providing seamless integration with deferred deep links using **App Links**.

---

## 🚀 **Integration Steps**

### 📦 **Step 1: Add JitPack Repository**
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

---

### 🔗 **Step 2: Add Dependency**
In your app-level `build.gradle.kts`, add the dependency:

```kotlin
dependencies {
    implementation("com.github.nonanerz:sddl-android-sdk:1.1.0")
}
```

> Replace `1.1.0` with the latest release version.

---

## 📲 **App Links Setup**

### 🔍 **1. Configure AndroidManifest.xml:**
Add the **App Link** configuration to your **AndroidManifest.xml**:

```xml
<uses-permission android:name="android.permission.INTERNET" />

<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop">

    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="sddl.me"
            android:pathPrefix="/" />
    </intent-filter>

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

</activity>
```

> **Important:** Make sure to set `android:autoVerify="true"` to enable automatic verification of App Links. Without this, the links might open in a browser instead of your app.

---

### 🌐 **2. Configure Digital Asset Links:**

To properly link your app with **https://sddl.me**, you need to set up **assetlinks.json**.

The **assetlinks.json** file is automatically generated via the SDDL interface. You cannot manually upload this file. Instead, follow these steps:

1. **Log in** to [https://sddl.me](https://sddl.me).
2. Navigate to the **App Links Configuration** section.
3. Enter your app's **Package Name** and **SHA256 Certificate Fingerprints**.
4. Save the configuration to automatically publish **assetlinks.json**.

Example **assetlinks.json** content:

```json
 "9B:22:AE:55:31:43:F9:95:6B:B4:93:F7:16:60:9A:DE:18:6C:34:E5:0C:C3:0A:A7:72:04:50:E6:03:84:A0:1F"
```

---

### 🔑 **How to Get SHA256 Certificate Fingerprints:**

#### **For Debug Key (default):**

```sh
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android
```

#### **For Release Key:**

```sh
keytool -list -v -alias your-release-key-alias -keystore /path/to/your-release-key.jks
```

- **Alias:** The name of your key alias, e.g., `androiddebugkey` for debug builds.
- **Keystore:** The path to your **.jks** or **.keystore** file.
- **Storepass & Keypass:** The passwords for your keystore and key.

The output will include **SHA256 Fingerprints**, which need to be added to **SDDL** via the **App Links Configuration**.

```plaintext
Certificate fingerprints:
    SHA256: 9B:22:AE:55:31:43:F9:95:6B:B4:93:F7:16:60:9A:DE:18:6C:34:E5:0C:C3:0A:A7:72:04:50:E6:03:84:A0:1F
```

---

## 🧑‍💻 **Usage Example**

### **MainActivity.kt:**

```kotlin
package com.simplelink.applink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.gson.JsonObject
import com.simplelink.sddl_sdk.SDDLSDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null) {
            Log.d("AppLinkTest", "Received URL: ${data.toString()}")
            SDDLSDK.fetchDetails(data, object : SDDLSDK.SDDLCallback {
                override fun onSuccess(data: JsonObject) {
                    Log.d("SDDLSDK", "Fetched Data: $data")
                }

                override fun onError(error: String) {
                    Log.e("SDDLSDK", "Error: $error")
                }
            })
        } else {
            Log.d("AppLinkTest", "No URL received")
        }
    }
}
```

---

## 📄 **License**
This SDK is licensed under the MIT License.

For more details, visit [GitHub Repository](https://github.com/nonanerz/sddl-android-sdk).

