package com.streamliners.timify.di

import android.app.Application
import com.streamliners.timify.BuildConfig
import com.streamliners.timify.android.helper.DataStoreUtil
import com.streamliners.timify.android.helper.TTSHelper
import com.streamliners.timify.data.local.LocalDB
import com.streamliners.timify.data.local.LocalRepo
import com.streamliners.timify.feature.ai.AIProvider
import com.streamliners.timify.feature.ai.ClaudeProvider
import com.streamliners.timify.feature.chat.ChatViewModel
import com.streamliners.timify.feature.home.HomeViewModel
import com.streamliners.timify.feature.insights.InsightsViewModel
import com.streamliners.timify.feature.pieChart.PieChartViewModel
import com.streamliners.timify.feature.stats.StatsViewModel
import com.streamliners.timify.feature.voice.sarvam.SarvamSTTService
import com.streamliners.timify.feature.voice.sarvam.SarvamTTSService
import com.streamliners.timify.feature.voiceCapture.VoiceCaptureViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun Application.koinSetup() {
    startKoin {
        androidLogger()
        androidContext(this@koinSetup)
        modules(appModule, viewModelModule)
    }
}

private val appModule = module {
    single {
        HttpClient(CIO) {
            expectSuccess = true
        }
    }
    single {
        LocalDB.create(androidApplication())
    }
    single {
        get<LocalDB>().chatHistoryDao()
    }
    single {
        get<LocalDB>().taskInfoDao()
    }
    single {
        get<LocalDB>().customAttributeDao()
    }
    single { DataStoreUtil.create(androidApplication()) }
    single { LocalRepo(get()) }

    // AI Provider - Claude (primary) via Koog framework
    single<AIProvider> {
        ClaudeProvider(BuildConfig.anthropicApiKey)
    }

    // Sarvam AI Voice Services (Hindi)
    single {
        SarvamTTSService(
            httpClient = get(),
            apiKey = BuildConfig.sarvamApiKey,
            cacheDir = androidApplication().cacheDir
        )
    }
    single {
        SarvamSTTService(
            httpClient = get(),
            apiKey = BuildConfig.sarvamApiKey
        )
    }

    // TTS Helper with Sarvam support for Hindi
    single { TTSHelper(androidApplication(), get<SarvamTTSService>()) }
}

private val viewModelModule = module {
    // New screens
    viewModel { HomeViewModel(get()) }
    viewModel { VoiceCaptureViewModel(get(), get(), get(), get()) }
    viewModel { StatsViewModel(get()) }
    viewModel { InsightsViewModel(get(), get()) }

    // Legacy screens
    viewModel { ChatViewModel(get(), get(), get(), get()) }
    viewModel { PieChartViewModel(get(), get()) }
}
