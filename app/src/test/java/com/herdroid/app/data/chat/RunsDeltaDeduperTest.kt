package com.herdroid.app.data.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class RunsDeltaDeduperTest {

    @Test
    fun mergeDelta_inCatchUpSkipsReplayedChunk() {
        val acc = StringBuilder("hello world")
        val merged = RunsDeltaDeduper.mergeDelta(
            accumulated = acc,
            incoming = "hello",
            catchUpCursor = 0,
        )
        assertEquals("", merged.emittable)
        assertEquals(5, merged.nextCatchUpCursor)
        assertEquals("hello world", acc.toString())
    }

    @Test
    fun mergeDelta_inCatchUpEmitsOnlyNewTail() {
        val acc = StringBuilder("hello world")
        val merged = RunsDeltaDeduper.mergeDelta(
            accumulated = acc,
            incoming = "world!!!",
            catchUpCursor = 6,
        )
        assertEquals("!!!", merged.emittable)
        assertEquals("hello world!!!", acc.toString())
    }

    @Test
    fun mergeDelta_afterCatchUpAppendsWholeDelta() {
        val acc = StringBuilder("abc")
        val merged = RunsDeltaDeduper.mergeDelta(
            accumulated = acc,
            incoming = "def",
            catchUpCursor = 3,
        )
        assertEquals("def", merged.emittable)
        assertEquals("abcdef", acc.toString())
        assertEquals(6, merged.nextCatchUpCursor)
    }
}
