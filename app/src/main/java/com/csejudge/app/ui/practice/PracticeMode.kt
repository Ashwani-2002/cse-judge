package com.csejudge.app.ui.practice

/**
 * Mutually exclusive practice modes.
 */
enum class PracticeMode {
    /**
     * Questions can come uniformly from all available subjects.
     * Subject and microtheme filters are inactive.
     */
    RANDOM,

    /**
     * Questions are restricted to a mandatory selected subject,
     * with an optional microtheme filter.
     */
    SUBJECT
}
