package com.shavebuddy.demo

import android.app.Application
import com.shavebuddy.demo.data.ShaveDatabase
import com.shavebuddy.demo.data.ShaveRepository

class ShaveApplication : Application() {
    val repository by lazy { ShaveRepository(ShaveDatabase.open(this)) }
}
