#!/bin/bash
# Build NoFallback-1.2.apk (legacy Xposed/Vector module)
set -e
BT="D:/Android/sdk/build-tools/34.0.0"
AJ="D:/Android/sdk/platforms/android-34/android.jar"
cd "$(dirname "$0")"

rm -rf classes dex base.apk unsigned.apk aligned.apk NoFallback-1.2.apk NoFallback-1.2.apk.idsig
mkdir -p classes dex stubs-classes
javac --release 8 -nowarn -cp "$AJ" -d stubs-classes $(find stubs -name "*.java")
jar cf stubs.jar -C stubs-classes .

javac --release 8 -nowarn -cp "$AJ;stubs" -d classes src/io/nofallback/*.java
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --min-api 29 --lib "$AJ" --lib stubs.jar \
     --output dex classes/io/nofallback/*.class

rm -rf res_compiled && mkdir -p res_compiled
"$BT/aapt2.exe" compile --dir res -o res_compiled
"$BT/aapt2.exe" link -I "$AJ" --manifest AndroidManifest.xml -o base.apk res_compiled/*.flat
"/d/Anaconda/python.exe" - <<'PY'
import zipfile, shutil
shutil.copy('base.apk', 'unsigned.apk')
with zipfile.ZipFile('unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    z.write('dex/classes.dex', 'classes.dex')
    z.writestr('assets/xposed_init', 'io.nofallback.HookMain')
PY
"$BT/zipalign.exe" -p -f 4 unsigned.apk aligned.apk
"$BT/apksigner.bat" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias dh --out NoFallback-1.2.apk aligned.apk
ls -l NoFallback-1.2.apk
"$BT/aapt2.exe" dump badging NoFallback-1.2.apk | head -3
