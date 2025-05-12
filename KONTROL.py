# ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

from Kekik.cli    import konsol
from cloudscraper import CloudScraper
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import os, re, base64, json

class MainUrlUpdater:
    def __init__(self, base_dir="."):
        self.base_dir = base_dir
        self.oturum   = CloudScraper()

    @property
    def eklentiler(self):
        return sorted([
            dosya for dosya in os.listdir(self.base_dir)
                if os.path.isdir(os.path.join(self.base_dir, dosya))
                    and not dosya.startswith(".")
                        and dosya not in {"gradle", "CanliTV", "OxAx", "__Temel", "SineWix", "YouTube", "NetflixMirror", "HQPorner", "WebteIzle", "TrDiziIzle"}
        ])

    def _kt_dosyasini_bul(self, dizin, dosya_adi):
        for kok, alt_dizinler, dosyalar in os.walk(dizin):
            if dosya_adi in dosyalar:
                return os.path.join(kok, dosya_adi)

        return None

    @property
    def kt_dosyalari(self):
        return [
            kt_dosya_yolu for eklenti in self.eklentiler
                if (kt_dosya_yolu := self._kt_dosyasini_bul(eklenti, f"{eklenti}.kt"))
        ]

    def _mainurl_bul(self, kt_dosya_yolu):
        with open(kt_dosya_yolu, "r", encoding="utf-8") as file:
            icerik = file.read()
            if mainurl := re.search(r'override\s+var\s+mainUrl\s*=\s*"([^"]+)"', icerik):
                return mainurl[1]

        return None

    def _mainurl_guncelle(self, kt_dosya_yolu, eski_url, yeni_url):
        with open(kt_dosya_yolu, "r+", encoding="utf-8") as file:
            icerik = file.read()
            yeni_icerik = icerik.replace(eski_url, yeni_url)
            file.seek(0)
            file.write(yeni_icerik)
            file.truncate()

    def _versiyonu_artir(self, build_gradle_yolu):
        with open(build_gradle_yolu, "r+", encoding="utf-8") as file:
            icerik = file.read()
            if version_match := re.search(r'version\s*=\s*(\d+)', icerik):
                eski_versiyon = int(version_match[1])
                yeni_versiyon = eski_versiyon + 1
                yeni_icerik = icerik.replace(f"version = {eski_versiyon}", f"version = {yeni_versiyon}")
                file.seek(0)
                file.write(yeni_icerik)
                file.truncate()
                return yeni_versiyon

        return None

    def _rectv_ver(self):
        istek = self.oturum.post(
            url     = "https://firebaseremoteconfig.googleapis.com/v1/projects/791583031279/namespaces/firebase:fetch",
            headers = {
                "X-Goog-Api-Key"    : "AIzaSyBbhpzG8Ecohu9yArfCO5tF13BQLhjLahc",
                "X-Android-Package" : "com.rectv.shot",
                "User-Agent"        : "Dalvik/2.1.0 (Linux; U; Android 12)",
            },
            json    = {
                "appBuild"      : "81",
                "appInstanceId" : "evON8ZdeSr-0wUYxf0qs68",
                "appId"         : "1:791583031279:android:1",
            }
        )
        return istek.json().get("entries", {}).get("api_url", "").replace("/api/", "")

    def _golgetv_ver(self):
        istek = self.oturum.get("https://raw.githubusercontent.com/sevdaliyim/sevdaliyim/main/ssl2.key").text
        cipher = AES.new(b"trskmrskslmzbzcnfstkcshpfstkcshp", AES.MODE_CBC, b"trskmrskslmzbzcn")
        encrypted_data = base64.b64decode(istek)
        decrypted_data = unpad(cipher.decrypt(encrypted_data), AES.block_size).decode("utf-8")
        return json.loads(decrypted_data, strict=False)["apiUrl"]

    @property
    def mainurl_listesi(self):
        return {
            dosya: self._mainurl_bul(dosya) for dosya in self.kt_dosyalari
        }

    def guncelle(self):
        for dosya, mainurl in self.mainurl_listesi.items():
            eklenti_adi = dosya.split("/")[0]

            print("\n")
            konsol.log(f"[~] Kontrol Ediliyor : {eklenti_adi}")
            if eklenti_adi == "RecTV":
                try:
                    final_url = self._rectv_ver()
                    konsol.log(f"[+] Kontrol Edildi   : {mainurl}")
                except Exception as hata:
                    konsol.log(f"[!] Kontrol Edilemedi : {mainurl}")
                    konsol.log(f"[!] {type(hata).__name__} : {hata}")
                    continue
            elif eklenti_adi == "GolgeTV":
                try:
                    final_url = self._golgetv_ver()
                    konsol.log(f"[+] Kontrol Edildi   : {mainurl}")
                except Exception as hata:
                    konsol.log(f"[!] Kontrol Edilemedi : {mainurl}")
                    konsol.log(f"[!] Kontrol Edilemedi : {mainurl}")
                    konsol.log(f"[!] {type(hata).__name__} : {hata}")
                    continue
            else:
                try:
                    istek = self.oturum.get(mainurl, allow_redirects=True)
                    konsol.log(f"[+] Kontrol Edildi   : {mainurl}")
                except Exception as hata:
                    konsol.log(f"[!] Kontrol Edilemedi : {mainurl}")
                    konsol.log(f"[!] {type(hata).__name__} : {hata}")
                    continue

                final_url = istek.url[:-1] if istek.url.endswith("/") else istek.url

            if mainurl == final_url:
                continue

            self._mainurl_guncelle(dosya, mainurl, final_url)

            if self._versiyonu_artir(f"{eklenti_adi}/build.gradle.kts"):
                konsol.log(f"[»] {mainurl} -> {final_url}")


if __name__ == "__main__":
    updater = MainUrlUpdater()
    updater.guncelle()
