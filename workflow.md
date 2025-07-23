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

### Building

```bash
./gradlew build
jar cf patches.jar -C patches/build/classes/kotlin/main . -C patches/build/resources/main .
mv patches.jar patches.rvp
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

## Logs

```bash
adb shell
logcat | grep yourapp
```
