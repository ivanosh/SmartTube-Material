package io.github.ivanosh.stmaterial.logic

/**
 * Whether the panel is laid out as the open one (header, buttons, rows). A hidden panel is, so that opening it changes
 * layer properties and builds nothing; only the seek preview, which shows the bar alone, collapses the rest.
 */
fun panelLayoutOpen(open: Boolean, seekPreview: Boolean): Boolean = open || !seekPreview
