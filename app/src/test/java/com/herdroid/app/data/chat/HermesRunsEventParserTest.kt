package com.herdroid.app.data.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesRunsEventParserTest {

    @Test
    fun extractRunId_prefersRunIdField() {
        val id = HermesRunsEventParser.extractRunId("""{"run_id":"run_123","id":"fallback"}""")
        assertEquals("run_123", id)
    }

    @Test
    fun extractRunId_fallsBackToIdField() {
        val id = HermesRunsEventParser.extractRunId("""{"id":"run_from_id"}""")
        assertEquals("run_from_id", id)
    }

    @Test
    fun extractTextDelta_fromChatCompletionsChunk() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "chat.completion.chunk",
            rawData = """{"choices":[{"delta":{"content":"hello"}}]}""",
        )
        assertEquals("hello", delta)
    }

    @Test
    fun extractTextDelta_fromResponsesStyleEvent() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "response.output_text.delta",
            rawData = """{"delta":" world"}""",
        )
        assertEquals(" world", delta)
    }

    @Test
    fun extractTextDelta_decodesUnicodeEscapes() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "response.output_text.delta",
            rawData = """{"delta":"\u6ca1\u95ee\u9898"}""",
        )
        assertEquals("没问题", delta)
    }

    @Test
    fun extractTextDelta_ignoresTerminalCompletedPayload() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "response.completed",
            rawData = """{"status":"completed","content":"full text"}""",
        )
        assertEquals(null, delta)
    }

    @Test
    fun extractTextDelta_ignoresDoneEventPayload() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "response.output_text.done",
            rawData = """{"content":"full text"}""",
        )
        assertEquals(null, delta)
    }

    @Test
    fun extractTextDelta_prefersDeltaOverContentWhenBothExist() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "response.output_text.delta",
            rawData = """{"delta":"x","content":"already_full"}""",
        )
        assertEquals("x", delta)
    }

    @Test
    fun extractTextDelta_ignoresNonDeltaSnapshotEvent() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = "response.output_text",
            rawData = """{"content":"full text snapshot"}""",
        )
        assertEquals(null, delta)
    }

    @Test
    fun extractTextDelta_ignoresContentOnlyWithoutDeltaHint() {
        val delta = HermesRunsEventParser.extractTextDelta(
            eventName = null,
            rawData = """{"content":"full text snapshot"}""",
        )
        assertEquals(null, delta)
    }

    @Test
    fun isTerminalEvent_handlesDoneAndCompleted() {
        assertTrue(HermesRunsEventParser.isTerminalEvent(eventName = "response.completed", rawData = "{}"))
        assertTrue(HermesRunsEventParser.isTerminalEvent(eventName = null, rawData = "[DONE]"))
        assertFalse(HermesRunsEventParser.isTerminalEvent(eventName = "response.output_text.delta", rawData = """{"delta":"x"}"""))
    }
}
