package com.vibehub.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple in-memory rate limiter for user actions.
 *
 * Usage:
 *   if (!rateLimiter.tryAcquire(RateLimiter.Action.POST)) { showError("Too many posts") }
 */
@Singleton
class RateLimiter @Inject constructor() {

    enum class Action(
        val maxPerWindow: Int,
        val windowMs: Long,
        val displayName: String,
    ) {
        POST(3, 60_000L, "post"),              // 3 posts per minute
        COMMENT(10, 60_000L, "comment"),        // 10 comments per minute
        STORY(5, 60_000L, "story"),             // 5 stories per minute
        MESSAGE(30, 60_000L, "message"),        // 30 messages per minute
        FOLLOW(20, 60_000L, "follow"),          // 20 follows per minute
        LIKE(60, 60_000L, "like"),              // 60 likes per minute
        SEARCH(20, 60_000L, "search"),          // 20 searches per minute
        REPORT(3, 300_000L, "report"),          // 3 reports per 5 minutes
    }

    private val buckets = mutableMapOf<Action, ArrayDeque<Long>>()

    /**
     * Returns true if the action is allowed.
     * Returns false (rate limited) if the limit has been exceeded.
     */
    @Synchronized
    fun tryAcquire(action: Action): Boolean {
        val now = System.currentTimeMillis()
        val deque = buckets.getOrPut(action) { ArrayDeque() }

        // Evict timestamps outside the current window
        while (deque.isNotEmpty() && now - deque.first() > action.windowMs) {
            deque.removeFirst()
        }

        if (deque.size >= action.maxPerWindow) return false
        deque.addLast(now)
        return true
    }

    /**
     * How many milliseconds until the next slot becomes available.
     * Returns 0 if already allowed.
     */
    @Synchronized
    fun cooldownMs(action: Action): Long {
        val now = System.currentTimeMillis()
        val deque = buckets[action] ?: return 0L
        if (deque.size < action.maxPerWindow) return 0L
        val oldest = deque.first()
        return maxOf(0L, action.windowMs - (now - oldest))
    }

    fun errorMessage(action: Action): String {
        val secs = (cooldownMs(action) / 1000L).coerceAtLeast(1L)
        return "You're ${action.displayName}ing too fast. Try again in ${secs}s."
    }

    @Synchronized
    fun reset(action: Action) {
        buckets.remove(action)
    }
}
