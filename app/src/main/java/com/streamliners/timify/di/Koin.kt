package com.streamliners.timify.di

import android.app.Application
import com.streamliners.timify.BuildConfig
import com.streamliners.timify.android.helper.DataStoreUtil
import com.streamliners.timify.android.helper.GoogleOAuthTokensFetcher
import com.streamliners.timify.android.helper.TTSHelper
import com.streamliners.timify.data.local.LocalDB
import com.streamliners.timify.data.local.LocalRepo
import com.streamliners.timify.feature.ai.AIProvider
import com.streamliners.timify.feature.ai.ClaudeProvider
import com.streamliners.timify.feature.ai.GeminiProvider
import com.streamliners.timify.feature.chat.ChatViewModel
import com.streamliners.timify.feature.pieChart.PieChartViewModel
import com.streamliners.timify.feature.sheetSync.SheetSyncViewModel
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
    single { TTSHelper(androidApplication()) }
    single { GoogleOAuthTokensFetcher(get()) }
    single { DataStoreUtil.create(androidApplication()) }
    single { LocalRepo(get()) }

    // AI Provider - Claude (primary) via Koog framework
    // To switch to Gemini, change to: GeminiProvider(BuildConfig.apiKey)
    single<AIProvider> {
        ClaudeProvider(BuildConfig.anthropicApiKey)
    }
}

private val viewModelModule = module {
    viewModel { ChatViewModel(get(), get(), get(), get()) }
    viewModel { PieChartViewModel(get(), get()) }
    viewModel { SheetSyncViewModel(get(), get(), get(), get()) }
}
