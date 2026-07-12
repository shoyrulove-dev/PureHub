package com.purehub.app.di

import com.purehub.app.data.CommunityPreferencesRepository
import com.purehub.app.data.local.PureHubDatabase
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.feature.billsplitter.BillPresetRepository
import com.purehub.app.feature.cleaner.CleanerRepository
import com.purehub.app.feature.docpdf.DocPdfRepository
import com.purehub.app.feature.pomodoro.PomodoroAudioManager
import com.purehub.app.feature.zenhabit.ZenHabitRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val appModule = module {
    single<PureHubDatabase> { PureHubDatabaseProvider.get(androidApplication()) }
    single { get<PureHubDatabase>().habitDao() }
    single { get<PureHubDatabase>().habitCheckInDao() }
    single { get<PureHubDatabase>().expenseDao() }
    single { ZenHabitRepository(get(), get()) }
    single { CommunityPreferencesRepository(androidApplication()) }
    single { BillPresetRepository(androidApplication()) }
    single { CleanerRepository(androidApplication().contentResolver) }
    single { DocPdfRepository(androidApplication()) }
    single { PomodoroAudioManager(androidApplication()) }
}
