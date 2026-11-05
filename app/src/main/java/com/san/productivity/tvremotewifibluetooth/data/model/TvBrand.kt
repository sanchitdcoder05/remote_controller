package com.san.productivity.tvremotewifibluetooth.data.model

enum class TvBrand(val displayName: String, val description: String) {
    SAMSUNG("Samsung", "Tizen OS Smart TVs & Non-Smart TVs"),
    LG("LG Smart TV", "WebOS Smart TVs & LG IR Remotes"),
    SONY("Sony Bravia", "Android TV, Google TV & Sony IR Remotes"),
    ANDROID_TV("Android TV / Google TV", "TCL, Hisense, Xiaomi, Nvidia Shield"),
    FIRE_TV("Amazon Fire TV", "Fire OS Sticks & Fire Edition TVs"),
    ROKU("Roku TV", "Roku Express, Streaming Stick & Roku TVs")
}
