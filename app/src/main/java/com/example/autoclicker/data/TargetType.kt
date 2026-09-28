package com.example.autoutil.data

/**
 * How a Step's target node should be located on screen.
 * Search order in NodeFinder is always: VIEW_ID -> TEXT -> CONTENT_DESC -> hierarchy fallback.
 */
enum class TargetType {
    VIEW_ID,
    TEXT,
    CONTENT_DESC
}
