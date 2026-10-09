package com.horse.jk_bms.connection

enum class SessionState { DISCONNECTED, PERMISSION_PENDING, CONNECTING, LIVE, STALE, RECOVERING }

fun telemetryAge(now: Long, lastReceived: Long): Long =
    if (lastReceived <= 0L) Long.MAX_VALUE else (now - lastReceived).coerceAtLeast(0L)
