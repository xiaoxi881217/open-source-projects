package com.kingzcheung.xime.rime

import org.junit.Assert.*
import org.junit.Test

class HomeEmojiPatchTest {
    @Test fun firstInstallAndRepeatedUpgradeAreIdempotent() {
        val next=HomeEmojiPatch.patch("")
        assertTrue(next.contains("lua_filter@*little_home_emoji"))
        assertEquals(next,HomeEmojiPatch.patch(next))
    }
    @Test fun userDictionaryAndExistingFiltersArePreserved() {
        val old="# personal settings\npatch:\n  \"translator/dictionary\": my_words\n  \"engine/filters\": [t9_filter, uniquifier]\n"
        val next=HomeEmojiPatch.patch(old)
        assertTrue(next.startsWith("# personal settings\npatch:"))
        assertTrue(next.contains("  \"translator/dictionary\": my_words\n"))
        assertTrue(next.contains("  \"engine/filters\": [t9_filter, uniquifier]\n"))
        assertEquals(next,HomeEmojiPatch.patch(next))
    }
    @Test fun existingAppendKeyIsNotOverwritten() {
        val old="patch:\n  \"engine/filters/+\": [user_filter]\n"
        val next=HomeEmojiPatch.patch(old)
        assertTrue(next.contains("\"engine/filters/+\": [user_filter]"))
        assertTrue(next.contains("\"engine/filters/@next\": lua_filter@*little_home_emoji"))
        assertEquals(next,HomeEmojiPatch.patch(next))
    }
    @Test fun unsupportedInlineAndMalformedYamlFailWithoutRewritingSource() {
        val inline="patch: {engine/filters: [user_filter]}"
        try{HomeEmojiPatch.patch(inline);fail("must preserve unsupported inline patch")}catch(_:Exception){}
        val malformed="patch: [broken"
        try{HomeEmojiPatch.patch(malformed);fail("must reject malformed YAML")}catch(_:Exception){}
        assertEquals("patch: {engine/filters: [user_filter]}",inline)
    }
}
