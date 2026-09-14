package com.focuslock.app.domain

/**
 * The internal event model. Designed so the full statistics UI can be added
 * later without changing the runtime. Only [UNLOCK_COMPLETED] ever affects the
 * daily counter.
 */
enum class FocusEventType {
    /** User opened a protected app while protection was active. Counter unchanged. */
    BLOCKED_ENCOUNTER,

    /** User saw the block screen and pressed "Go back". Counter unchanged. */
    RESISTED,

    /** User deliberately chose "Wait". Counter unchanged. */
    UNLOCK_STARTED,

    /** Required wait completed and temporary access was granted. */
    UNLOCK_COMPLETED,

    /** User started a wait and then cancelled it. Counter is NOT changed. */
    WAIT_CANCELLED,

    /** A temporary 10-minute grant expired. */
    GRANT_EXPIRED
}
