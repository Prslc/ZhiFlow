package com.prslc.zhiflow.core.exception

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

/**
 * How the app takes a failed [Result].
 *
 * `onApiFailure` is the only way a ViewModel handles one, and the two things it promises are the
 * two things `Result.onFailure` does not do: hand the callback an [ApiException] whatever the
 * failure actually was, and let a cancellation past. Both matter in the same way -- a failure that
 * reaches no handler is an error the reader is never told about, and a cancellation that reaches
 * one is an error they are told about for no reason.
 */
class ResultExtensionsTest {

    @Test
    fun `a success is not a failure and is left alone`() {
        var handled = false

        val result = Result.success(7).onApiFailure { handled = true }

        assertFalse(handled)
        assertEquals(7, result.getOrNull())
    }

    @Test
    fun `a failure the app cannot name still reaches the handler`() {
        // The whole point: never a silent no-op. An unrecognised throwable arrives as the unknown
        // failure rather than as nothing at all.
        var seen: ApiException? = null

        Result.failure<Unit>(IllegalStateException("boom")).onApiFailure { seen = it }

        assertTrue(seen is ApiException.UnknownException)
    }

    @Test
    fun `a failure the app can name arrives as itself`() {
        var seen: ApiException? = null
        val failure = ApiException.NetworkException()

        Result.failure<Unit>(failure).onApiFailure { seen = it }

        assertSame(failure, seen)
    }

    @Test
    fun `cancellation is let past rather than reported`() {
        // Cancellation is control flow: a request the reader has already navigated away from is
        // not an error state to raise. `Result.onFailure` would have swallowed it.
        var reported = false

        assertThrows(CancellationException::class.java) {
            Result.failure<Unit>(CancellationException("superseded"))
                .onApiFailure { reported = true }
        }

        assertFalse(reported)
    }

    @Test
    fun `ignoreOutcome takes a failure without touching it`() {
        // The counterpart to `onApiFailure`: best-effort work says so by name, and nothing is read
        // here, so nothing is thrown -- a cancellation included, which nothing here has an opinion
        // about either way.
        assertEquals(Unit, Result.failure<Unit>(IllegalStateException("boom")).ignoreOutcome())
        assertEquals(Unit, Result.failure<Unit>(CancellationException("superseded")).ignoreOutcome())
    }

    @Test
    fun `a page is mapped off the thread that asked for it`() {
        // A repository is called from a ViewModel's scope, which is the main thread; turning a page
        // of models into what a screen draws is not the main thread's work.
        val caller = Thread.currentThread().name

        val mappedOn = runBlocking {
            Result.success("page").mapOffMain { Thread.currentThread().name }
        }.getOrNull()

        assertNotEquals(caller, mappedOn)
    }

    @Test
    fun `a failure stays a failure through the mapping`() {
        val mapped = runBlocking {
            Result.failure<Int>(ApiException.NetworkException()).mapOffMain { it * 2 }
        }

        assertTrue(mapped.isFailure)
        assertTrue(mapped.exceptionOrNull() is ApiException.NetworkException)
    }

    @Test
    fun `a mapping that throws is a failure rather than an escape`() {
        // A parser or a mapper reads the server's data and is not written to be total. Left to
        // escape, a throw from one goes past every `onApiFailure`, out of the scope it was
        // launched in and to the thread's uncaught handler, which for a ViewModel is the process.
        val mapped = runBlocking {
            Result.success("a page").mapOffMain { error("a parser that met something new") }
        }

        assertTrue(mapped.isFailure)
        assertTrue(mapped.exceptionOrNull() is ApiException.UnknownException)
    }

    @Test
    fun `a mapping that is cancelled stays cancelled`() {
        // Cancellation stays control flow rather than becoming something the reader is shown. The
        // instance is not the one thrown: the hop through `withContext` wraps it.
        val thrown = runCatching {
            runBlocking {
                Result.success("a page").mapOffMain<String, Unit> {
                    throw CancellationException("superseded")
                }
            }
        }.exceptionOrNull()

        assertTrue("a cancellation must stay one", thrown is CancellationException)
        assertEquals("superseded", thrown?.message)
    }
}
