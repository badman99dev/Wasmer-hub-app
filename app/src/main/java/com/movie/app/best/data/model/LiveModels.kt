package com.movie.app.best.data.model

data class LiveChannel(
    val id: Int,
    val name: String,
    val logoUrl: String,
    val category: String,
    val streamUrl: String,
)

object LiveChannels {
    val all = listOf(
        // ── Entertainment ──
        LiveChannel(1, "Colors HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/23/d83f7cfa0cead1c3be3addc876587c0a.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/16.m3u8"),
        LiveChannel(2, "Zee TV HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/25/d41be270d73a04627cd3d67d14bb0b50.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/18.m3u8"),
        LiveChannel(3, "Star Plus", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/22/d31b0c3c16bf45aae831701bc4003d78.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/15.m3u8"),
        LiveChannel(4, "Star Bharat HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/26/13cc0641742be5cf3e46d9d2ad109483.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/19.m3u8"),
        LiveChannel(5, "Zee Cinema HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/27/e3900972c1f48c7ccaf31dbc455b541e.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/20.m3u8"),
        LiveChannel(6, "Sony Max HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/28/673d3338616d451c9252fd1f4d8c7620.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/21.m3u8"),
        LiveChannel(7, "Sony Entertainment", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/24/83a38c5796110d892589dadd8d2a4929.jpg", "Entertainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/17.m3u8"),
        
        // ── Kids ──
        LiveChannel(8, "Cartoon Network HD+ Hindi", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/34/ee9196cfebb8f8dc15f0b53b75b138bb.jpg", "Kids", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/27.m3u8"),
        LiveChannel(9, "Pogo Hindi", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/35/cd1670f30605ebd8a2d1fd0fbf1febb3.jpg", "Kids", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/28.m3u8"),
        
        // ── Infotainment ──
        LiveChannel(10, "Discovery HD Hindi", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/36/00a041517603efa06d1360ee1b946647.jpg", "Infotainment", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/29.m3u8"),
        
        // ── News ──
        LiveChannel(11, "Aaj Tak", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/29/e92479685945168f85caf2c80ef68c22.jpg", "News", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/22.m3u8"),
        LiveChannel(12, "ABP News India", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/30/3d71d1e516a5e2bdb534f69e9b485871.jpg", "News", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/23.m3u8"),
        LiveChannel(13, "India TV", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/31/8c29c9a03f997a8c6d38c436aa38e382.jpg", "News", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/24.m3u8"),
        LiveChannel(14, "CNN NEWS18", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/32/c6d9e30822d003dbaaadb3eb5a8de450.jpg", "News", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/25.m3u8"),
        LiveChannel(15, "Republic Bharat", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/33/b6effa246efefc9f1454d83de6476798.jpg", "News", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/26.m3u8"),
        
        // ── Sports ──
        LiveChannel(16, "Star Sports 1 Hindi HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/21/eb19a8f6167539822f1df27848fff91b.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/3.m3u8"),
        LiveChannel(17, "Star Sports 2 Hindi HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/2/ef92782f8c961905fd83bbd9987c987c.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/1.m3u8"),
        LiveChannel(18, "Star Sports Select 1 HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/5/2e68185bbe22aa968f98dc6fa082a97e.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/4.m3u8"),
        LiveChannel(19, "Star Sports Select 2 HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/1/4242f1d69a97fe9cde15e94bc132d45d.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/5.m3u8"),
        LiveChannel(20, "Sony Ten 3 Hindi", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/39/3d5b5f198f57917e0e1f91cb229b1bdfd.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/32.m3u8"),
        LiveChannel(21, "Sony Ten 4 HD Telugu", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/38/b1f7bf2d1f07ae314240a5c94249634a.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/31.m3u8"),
        LiveChannel(22, "Sony Ten 4 HD Tamil", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/37/34bf967527bbda0fba779ef9f2cb54b9.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/30.m3u8"),
        LiveChannel(23, "Sony Sports 1", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/12/4ee3f4e5f806e1fba4b0bdd3cf08fc4d.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/6.m3u8"),
        LiveChannel(24, "Sony Sports 2", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/13/99ce9ef51f4b67acd7e896dbe1247857.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/7.m3u8"),
        LiveChannel(25, "Sony Sports 5", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/14/f0bf51affc417a34c20e4f931a95ad35.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/8.m3u8"),
        LiveChannel(26, "EuroSports", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/15/20eb4c9816512f3e169180ffdc546f47.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/9.m3u8"),
        LiveChannel(27, "Premier Sports HD", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/16/e724a32ea2d1f454ad80a8a5a57bade6.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/10.m3u8"),
        LiveChannel(28, "Willow Sports", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/17/026e134cc1d66aee9dd5b95373598c9e.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/11.m3u8"),
        LiveChannel(29, "Willow Cricket", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/18/5143d419c7513478715dad960fb9ea0b.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/12.m3u8"),
        LiveChannel(30, "Fancode Sports", "https://img.elochkaigolochla.com/340-500/Images/Broadcasts/Poster/3/6e46756c390d8f91ab63932e600091ea.jpg", "Sports", "https://libre4snd.site/aCMA3XCXUS/GKCgq9Nsq3/2.m3u8"),
    )

    val categories = all.map { it.category }.distinct().sorted()
}