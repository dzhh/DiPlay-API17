"""Re-sign the frozen 0.34 payload with the current install number; never recompile it.

Requires DIPLAY_STABLE_APK, ANDROID_BUILD_TOOLS, DIPLAY_KEYSTORE,
DIPLAY_STORE_PASSWORD, DIPLAY_KEY_PASSWORD and optionally DIPLAY_KEY_ALIAS.
The original private APK is deliberately not part of the public source repository.
"""
import hashlib,json,os,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
import build

BASE_SHA='7597a89a17a72fff5132036e1ad8b10dd1155a8940ae761dfcac0a2bf3b0e6bc'
ROOT=build.ROOT

def payload(apk):
    with zipfile.ZipFile(apk) as z:
        return {n:z.read(n) for n in z.namelist() if n!='AndroidManifest.xml' and not n.startswith('META-INF/') and not n.endswith('/')}

def main():
    original=Path(os.environ['DIPLAY_STABLE_APK']).resolve()
    assert hashlib.sha256(original.read_bytes()).hexdigest()==BASE_SHA,'Stable baseline changed'
    ns='{http://schemas.android.com/apk/res/android}'
    current=ET.parse(ROOT/'AndroidManifest.xml').getroot()
    code=int(current.get(ns+'versionCode'));assert code>=35
    name='0.34.0-stable-r'+str(code)
    folder=build.BUILD/'stable';folder.mkdir(exist_ok=True)
    ET.register_namespace('android',ns[1:-1])
    manifest=ET.parse(ROOT/'stable/0.34/AndroidManifest.xml')
    manifest.getroot().set(ns+'versionCode',str(code));manifest.getroot().set(ns+'versionName',name)
    path=folder/'AndroidManifest.xml';manifest.write(path,encoding='utf-8',xml_declaration=True)
    bt=Path(os.environ['ANDROID_BUILD_TOOLS'])
    platform=Path(os.environ.get('ANDROID_AAPT_PLATFORM',str(build.TC/'android.jar')))
    template=folder/'manifest.apk'
    build.run([bt/'aapt.exe','package','-f','-M',path,'-S',ROOT/'stable/0.34/res','-I',platform,'-F',template],'stable-aapt')
    # Use only the new manifest. All executable code, resources, assets and native
    # libraries come verbatim from the tested APK, not the experimental sources.
    with zipfile.ZipFile(template) as z:
        manifest_bytes=z.read('AndroidManifest.xml')
        assert z.read('resources.arsc')==payload(original)['resources.arsc'],'Baseline resource IDs changed'
    unsigned=folder/'unsigned.apk'
    with zipfile.ZipFile(unsigned,'w',compression=zipfile.ZIP_DEFLATED) as z:
        z.writestr('AndroidManifest.xml',manifest_bytes)
        for n,data in payload(original).items():z.writestr(n,data)
    aligned=folder/'aligned.apk'
    build.run([bt/'zipalign.exe','-f','4',unsigned,aligned],'stable-align')
    target=ROOT/'dist/DiPlay_API17_Stable034.apk'
    build.run([build.JAVA,'-jar',bt/'lib/apksigner.jar','sign','--ks',Path(os.environ['DIPLAY_KEYSTORE']),'--ks-key-alias',os.environ.get('DIPLAY_KEY_ALIAS','diplay'),'--ks-pass','env:DIPLAY_STORE_PASSWORD','--key-pass','env:DIPLAY_KEY_PASSWORD','--min-sdk-version','17','--v1-signing-enabled','true','--v2-signing-enabled','true','--v3-signing-enabled','false','--v4-signing-enabled','false','--out',target,aligned],'stable-sign')
    build.run([build.JAVA,'-jar',bt/'lib/apksigner.jar','verify','--min-sdk-version','17','--max-sdk-version','17','--verbose',target],'stable-verify')
    assert payload(target)==payload(original),'Stable payload is no longer identical'
    meta={'available':True,'baseline':'0.34','baselineSha256':BASE_SHA,'versionCode':code,'versionName':name,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'size':target.stat().st_size,'notes':'恢复已实车验证的0.34有线功能（1024×600 / 15fps）。程序代码、资源、认证与原版逐项一致，仅安装编号和版本名调整并重新签名；保留数据，在线更新仍可返回实验版。'}
    (build.BUILD/'stable-release.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf-8')
    print('Frozen stable payload verified:',len(payload(target)),'entries; install code',code)

if __name__=='__main__':main()
