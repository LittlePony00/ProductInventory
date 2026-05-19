package com.android.rut.miit.productinventory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform