package com.herdroid.app.ui.main

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MainViewModelThreadingTest {

    @Test
    fun runNetworkOnIo_switchesOffCallerThread() = runBlocking {
        val callerThread = Thread.currentThread().name
        val networkThread = runNetworkOnIo { Thread.currentThread().name }
        assertNotEquals(callerThread, networkThread)
    }
}
