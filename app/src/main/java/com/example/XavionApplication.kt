package com.example

import android.app.Application
import com.example.ai.GeminiLiveClient
import com.example.ai.GeminiRestClient
import com.example.ai.XavionAudioEngine
import com.example.commands.DeviceCommandExecutor
import com.example.data.local.ConversationRepository
import com.example.data.local.MemoryRepository
import com.example.data.local.ProductivityRepository
import com.example.data.local.XavionDatabase
import com.example.data.preferences.SettingsRepository
import com.example.data.remote.WeatherRepository
import com.example.security.KeystoreVault
import com.example.service.FloatingBubbleManager
import com.example.service.WakeWordEngine
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(application: Application) {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    val keystoreVault = KeystoreVault(application)
    val settingsRepository = SettingsRepository(application)
    val database = XavionDatabase.getInstance(application)

    val conversationRepository = ConversationRepository(database.conversationDao())
    val memoryRepository = MemoryRepository(database.memoryDao())
    val productivityRepository = ProductivityRepository(
        taskDao = database.taskDao(),
        noteDao = database.noteDao(),
        toolLogDao = database.toolLogDao()
    )

    val commandExecutor = DeviceCommandExecutor(
        context = application,
        settingsRepository = settingsRepository,
        memoryRepository = memoryRepository,
        productivityRepository = productivityRepository
    )

    val weatherRepository = WeatherRepository(httpClient)
    val audioEngine = XavionAudioEngine(application)
    val restClient = GeminiRestClient(httpClient, commandExecutor)
    val liveClient = GeminiLiveClient(httpClient, audioEngine, commandExecutor)
    val wakeWordEngine = WakeWordEngine(application)
    val floatingBubbleManager = FloatingBubbleManager(application)
}

class XavionApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTerminate() {
        container.audioEngine.release()
        super.onTerminate()
    }
}
