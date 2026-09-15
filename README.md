# Weather Slideshow Display

A weather dashboard and slideshow application designed specifically for always-on Android devices.

## Features
- **Today's Weather Dashboard**: Detailed real-time visualization of temperature, "Feels Like", humidity, wind metrics, UV index, precipitation probabilities, and sunrise/sunset timings.
- **5-Day Visual Forecast**: Expected temperature, UV index, precipitation, and conditions for the next 5 days.
- **Live New York Times News Feed**: Automatically pulls and displays the four most recent news article titles and summaries from the New York Times RSS Home Page stream.
- **Interactive Device-Level Configuration**: Prompts the user for their 5-digit zip code on the first application launch. The location configuration securely updates and saves on device storage.
- **Boot and Wake Persistence**: Configured to stay awake via automated display flags and immediately resume operation upon power-cycling the device hardware.

<img width="738" height="459" alt="slideshow_app" src="https://github.com/user-attachments/assets/4f2f45e8-196f-4bfa-b857-b894327fdddd" />



---

## Requirements
- An Android device or smart display screen running Android Oreo (API 26) or higher.
- A functional installation of ADB (Android Debug Bridge) or Android Studio to sideload or deploy to your target hardware.

---

## Deployment & Installation

### Option 1: Direct via Android Studio (Recommended for Development)
1. Launch Android Studio and open the `SlideshowDisplay` project workspace directory.
2. Connect your Android tablet or digital display frame to your computer via USB (Ensure **USB Debugging** is toggled on inside Developer Options).
3. Select your device from the deployment dropdown list near the top of the interface.
4. Click the **Run** button (or press `Shift + F10`) to automatically compile, push, and execute the app on your screen.

### Option 2: Sideloading via ADB Terminal Command
If you have an accumulated standalone debug APK or want to manually distribute the app to a device over your local terminal environment:

1. Compile the debug package from the project root using the Gradle wrapper tool:
   ```bash
   ./gradlew assembleDebug
   ```
2. Navigate to your compiled packages output folder: `app/build/outputs/apk/debug/`
3. Sideload and install the package with standard overwrite rules:
   ```bash
   adb install -r app-debug.apk
   ```
4. Run the package immediately from your computer via the Activity Manager tool:
   ```bash
   adb shell am start -n com.example.slideshowdisplay/.MainActivity
   ```

---

## Post-Installation Device Setup

### 1. Initial Interactive Configuration
- Upon launching the application for the very first time on a new terminal, the app will recognize that no location properties are initialized and will show a one-time dialog box.
- Input your **5-digit Zip Code** using the on-screen numeric panel and hit **Submit**.
- The app will resolve the coordinates via the geolocation service, download current weather metadata, and initiate the dashboard slideshow layout sequence.
- **Reboot Security**: This zip code persists natively in local device storage. If your hardware shuts down or loses electrical connection, it will automatically reload your location without any prompt when it turns back on.

### 2. Updating Location via ADB (Optional / Advanced Overrides)
If you want to dynamically reconfigure the slideshow zip code remotely without interacting with the application interface or wiping data files:
```bash
adb shell am start -n com.example.slideshowdisplay/.MainActivity --es zip 90210
```
- `--es zip`: Pass any desired valid 5-digit postal code value to instantly update and persist the new location configurations.
