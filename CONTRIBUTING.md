# Contributing

In addition to the [Revanced documentation](https://github.com/ReVanced/revanced-documentation), here is a list of tools and commands that can help you with development on this project.

Revanced can be hard to dig into. If you have any questions, feel free to contact me on Discord or Signal (`@mubelotix`) or by email (`mubelotix@gmail.com`).

## Reverse engineering tools

### Merge split APKs

Download [ApkEditor](https://github.com/REAndroid/APKEditor) from their latest release.

```bash
java -jar APKEditor.jar m -i split.xapk
```

### Decompile to smali

Install apktool : https://apktool.org/docs/install/#linux

```bash
apktool d -o output app.apk
```

### Decompile to Java

Download jadx : https://github.com/skylot/jadx/releases

```bash
export JAVA_OPTS="-Xmx7G -Xms512m"
../jadx/bin/jadx -d output2 app.apk
```

If it gets incredibly slow, increase RAM allocation and start again.

## Patches

### Setup

```bash
cd /tmp
sudo apt install sdkmanager
cp -r /usr/lib/android-sdk/ android-sdk
sudo chown -R $USER:$USER android-sdk
export ANDROID_HOME=/tmp/android-sdk
sdkmanager --licenses
```

### Writing a new patch

Each patch lives in `patches/src/main/kotlin/app/revanced/patches/<appname>/` and consists of a patch file (e.g. `AdsPatch.kt`), optional `Fingerprints.kt`, and a **`notes.md`** file.

**`notes.md`** is a reverse engineering journal for the app. It should be **lengthy — better too long than too short**. Include:

- App version, version code, package name
- Architecture diagram of the target subsystem (e.g. ad loading pipeline, premium checks)
- Interesting classes, methods, and their purposes (even if you didn't end up patching them)
- Strings and constants that helped you navigate (API fields, experiment flags, resource IDs)
- Dead ends you explored and why they didn't work
- Gotchas: unexpected behavior, obfuscation quirks, methods that look promising but crash the app
- Recommendations for future work: other features that could be patched, edge cases to watch for

The goal is that someone (including future you) can pick up `notes.md` and understand the app's internals without redoing all the decompilation work.

### Fingerprint guidelines

- **Start permissive, tighten only when needed.** Begin with `accessFlags` + `returns` + `parameters`. Add `opcodes()`, `strings()`, or `custom {}` only when the current set matches too many methods.
- **Prefer content over names.** Obfuscated identifiers change every update. Match on stable properties: opcode sequences, return types, JSON serialization keys in field annotations, string constants.
- **Gotchas:** `returnEarly(false)` on object types returns null — use `addInstructions` with a proper object return. `instructions` is `Iterable`, use `.count()` not `.size`.
- **Keep patches independent.** Split unrelated concerns so users can toggle them individually.

### Building

```bash
./gradlew build
jar cf patches.rvp -C patches/build/classes/kotlin/main . -C patches/build/resources/main .
```

### Running

```bash
java -jar revanced-cli.jar list-patches \
    --with-descriptions=true \
    --with-versions=true \
    --filter-package-name=com.instagram.android \
    patches.rvp

java -jar revanced-cli.jar patch \
    --patches patches.rvp \
    --out patched.apk \
    app.apk

java -jar revanced-cli.jar patch \
    --patches patches.rvp \
    --out patched.apk \
    --exclusive \
    --force \
    --ei 6 \
    --ei 39 \
    app.apk

adb install -r patched.apk
```

## Emulation

If you have a physical device, you might want to use that instead of an emulator for performance reasons.

```bash
~/Android/Sdk/emulator/emulator -avd Medium_Phone_API_35 -gpu host
```

## Testing on device

To install the application, run it and see the logs on a device connected via USB/ADB:

```bash
# Install the APK
adb install -r patched.apk

# View logs
adb logcat | grep ReVanced
```

## React Native REPL

This patch injects a REPL into React Native applications, allowing you to run JavaScript code within the app's context.

1.  **Expose a local port:**
    You may use `serveo.net` or a similar service to expose a local port (e.g., 1337) to the internet. This allows the app on your device to connect back to your computer.

    ```bash
    ssh -R 80:localhost:1337 serveo.net
    ```
    *Note the URL displayed in the output (e.g., `https://1b3c552c9b4e82ab-89-80-240-3.serveousercontent.com`).*

2.  **Start the REPL server:**
    Run the Python server script.

    ```bash
    python3 patches/src/main/kotlin/app/revanced/patches/reactrepl/repl_server.py
    ```

3.  **Build and patch:**
    Build the patches and apply the REPL patch, providing the `ws_server` option with your serveo URL.

    ```bash
    ./gradlew build && jar cf patches.rvp -C patches/build/classes/kotlin/main . -C patches/build/resources/main . && java -jar revanced-cli.jar patch --patches patches.rvp --enable "React Native REPL" --options ws_server=YOUR_SERVEO_URL --out patched.apk target_app.apk
    ```
    *Replace `YOUR_SERVEO_URL` with the URL from step 1 (without `https://`, e.g., `1b3c552c9b4e82ab.serveousercontent.com`) and `target_app.apk` with your APK path.*

4.  **Install and connect:**
    Install the patched APK. When the app starts, it will connect to your Python server.

    ```bash
    adb install -r patched.apk
    ```
