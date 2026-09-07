package com.focuslock.app.util

/** Human formatting for waits/countdowns. */
object Format {

    fun waitLabel(seconds: Int): String = when {
        seconds < 60 -> "Wait $seconds seconds"
        seconds % 60 == 0 -> "Wait ${seconds / 60} ${plural(seconds / 60, "minute")}"
        else -> "Wait ${seconds / 60}m ${seconds % 60}s"
    }

    fun countdown(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val m = s / 60
        val r = s % 60
        return "%02d:%02d".format(m, r)
    }

    fun durationWords(seconds: Int): String = when {
        seconds < 60 -> "$seconds seconds"
        seconds % 60 == 0 -> "${seconds / 60} ${plural(seconds / 60, "minute")}"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }

    private fun plural(n: Int, word: String) = if (n == 1) word else "${word}s"
}
