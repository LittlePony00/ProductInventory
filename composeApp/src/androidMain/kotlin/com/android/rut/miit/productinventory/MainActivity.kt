package com.android.rut.miit.productinventory

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.rut.miit.productinventory.core.di.appModules
import com.android.rut.miit.productinventory.di.roomModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class ProductInventoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ProductInventoryApp)
            modules(appModules + roomModule)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
