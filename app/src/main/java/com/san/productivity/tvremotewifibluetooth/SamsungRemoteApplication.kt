package com.san.productivity.tvremotewifibluetooth

import android.app.Application
import com.san.productivity.tvremotewifibluetooth.data.local.TvPreferencesDataStore
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository

class SamsungRemoteApplication : Application() {

    lateinit var preferencesDataStore: TvPreferencesDataStore
        private set

    lateinit var repository: SamsungTvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesDataStore = TvPreferencesDataStore(this)
        repository = SamsungTvRepository(this, preferencesDataStore)
    }
}
