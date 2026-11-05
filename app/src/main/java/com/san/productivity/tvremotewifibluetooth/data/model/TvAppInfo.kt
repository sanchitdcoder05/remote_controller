package com.san.productivity.tvremotewifibluetooth.data.model

data class TvAppInfo(
    val appId: String,
    val name: String,
    val iconResId: Int? = null,
    val isPreinstalled: Boolean = true
) {
    companion object {
        val POPULAR_APPS = listOf(
            TvAppInfo("3201512006780", "YouTube"),
            TvAppInfo("3201907018807", "Netflix"),
            TvAppInfo("3201901017640", "Prime Video"),
            TvAppInfo("3201606009684", "Disney+ Hotstar"),
            TvAppInfo("3201512006963", "Plex"),
            TvAppInfo("111299001912", "Web Browser"),
            TvAppInfo("3201710015037", "Spotify"),
            TvAppInfo("3201807016597", "Apple TV")
        )
    }
}
