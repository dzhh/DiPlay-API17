"""Build/install a separate instrumentation APK on the owned API17 emulator only."""
import os,subprocess,zipfile,shutil,sys
from pathlib import Path
import build

out=build.BUILD/'ui-test';out.mkdir(exist_ok=True)
classes=out/'classes'
assert classes.resolve()==(build.ROOT/'build/ui-test/classes').resolve()
if classes.exists():shutil.rmtree(classes)
classes.mkdir()
dex=out/'dex';dex.mkdir(exist_ok=True)
workspace=Path(os.environ['DIPLAY_TEST_WORKSPACE'])
bt=workspace/'headunit-probe/tools/build-tools/android-11'
adb=workspace/'headunit-probe/tools/adb/platform-tools/adb.exe'
serial=os.environ.get('DIPLAY_TEST_SERIAL','emulator-5582')
assert serial in {'emulator-5582','emulator-5584'}, 'Only owned API17 test emulators are allowed'
def device(*args):
 p=subprocess.run([str(adb),'-P','5041','-s',serial,*args],capture_output=True,timeout=600 if '--soak' in sys.argv else 90,check=True)
 return p.stdout.decode('utf-8','replace').replace('\r','').strip()
assert device('shell','getprop','ro.kernel.qemu')=='1'
assert device('shell','getprop','ro.build.version.sdk')=='17'
cp=os.pathsep.join(str(p) for p in [build.BUILD/'java.jar',build.BUILD/'core.jar',build.TC/'android17.jar',build.TC/'kotlin-stdlib.jar'])
build.run([build.JAVA.with_name('javac.exe'),'-encoding','UTF-8','-source','8','-target','8','-bootclasspath',build.TC/'android17.jar','-classpath',cp,'-d',classes,build.ROOT/'tests/android/ProjectionUiInstrumentation.java'],'ui-test-compile')
jar=out/'tests.jar'
with zipfile.ZipFile(jar,'w') as z:
 for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes).as_posix())
build.run([build.JAVA,'-cp',build.TC/'r8.jar','com.android.tools.r8.D8','--min-api','17','--lib',build.TC/'android17.jar','--classpath',build.BUILD/'java.jar','--classpath',build.BUILD/'core.jar','--output',dex,jar],'ui-test-dex')
manifest=out/'AndroidManifest.xml'
manifest.write_text('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="local.headunit.diplay.uitest"><uses-sdk android:minSdkVersion="17" android:targetSdkVersion="17"/><application android:label="DiPlay UI Test"/><instrumentation android:name="local.headunit.diplay.ProjectionUiInstrumentation" android:targetPackage="local.headunit.diplay"/></manifest>''',encoding='utf-8')
unsigned=out/'unsigned.apk'
subprocess.run([str(bt/'aapt.exe'),'package','-f','-M',os.path.relpath(manifest,build.ROOT),'-I',os.path.relpath(build.TC/'android17.jar',build.ROOT),'-F',os.path.relpath(unsigned,build.ROOT)],cwd=build.ROOT,check=True)
with zipfile.ZipFile(unsigned,'a') as z:
 for f in dex.glob('*.dex'):z.write(f,f.name)
apk=out/'ui-test.apk'
build.run([build.JAVA,'-jar',bt/'lib/apksigner.jar','sign','--ks',Path(os.environ['DIPLAY_KEYSTORE']),'--ks-key-alias',os.environ['DIPLAY_KEY_ALIAS'],'--ks-pass','env:DIPLAY_STORE_PASSWORD','--min-sdk-version','17','--out',apk,unsigned],'ui-test-sign')
if '--build-only' in sys.argv:
 print('Instrumentation APK built; no device installation or execution',flush=True)
 raise SystemExit(0)
print(device('install','-r',str(build.ROOT/'dist/DiPlay_API17_Wired.apk')),flush=True)
print(device('install','-r',str(apk)),flush=True)
soak='--soak' in sys.argv
extra=['-e','soakRounds','30'] if soak else []
result=device('shell','am','instrument','-r','-w',*extra,'local.headunit.diplay.uitest/local.headunit.diplay.ProjectionUiInstrumentation')
(build.BUILD/('media-soak-results.txt' if soak else 'projection-ui-results.txt')).write_text(result,encoding='utf-8');print(result,flush=True)
if soak:(build.BUILD/'media-soak.json').write_text(device('shell','cat','/data/data/local.headunit.diplay/files/media-soak.json'),encoding='utf-8')
assert 'PASS:' in result and 'INSTRUMENTATION_CODE: -1' in result
