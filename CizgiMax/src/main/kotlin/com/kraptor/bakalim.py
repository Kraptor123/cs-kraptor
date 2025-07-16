# ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

from Kekik.cli       import konsol
from cloudscraper    import CloudScraper
from parsel          import Selector
from re              import search
from Kekik.Sifreleme import AESManager
from json            import loads

oturum = CloudScraper()
istek  = oturum.get("https://cizgimax.online/doraemon-1-sezon-13-bolum-izle-2")

secici  = Selector(istek.text)
linkler = secici.css("ul.linkler li a::attr(data-frame)").getall()

for link in linkler:
    konsol.log(link)
    oturum.headers.update({"Referer": "https://cizgimax.online/"})
    istek = oturum.get(link)

    be_player = search(r"bePlayer\('([^']+)',\s*'(\{[^\}]+\})'\);", istek.text).groups()
    konsol.print(be_player)

    be_player_pass = be_player[0]
    be_player_data = be_player[1]

    encrypted = AESManager.decrypt(be_player_data, be_player_pass).replace("\\", "")
    konsol.print(loads(encrypted))