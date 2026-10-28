package com.kingzcheung.xime.home

import org.junit.Assert.*
import org.junit.Test

class HomeEmailSuffixPolicyTest {
    @Test fun defaultsAreValidUniqueDomains(){assertEquals(HomeEmailSuffixPolicy.defaults,HomeEmailSuffixPolicy.parse(HomeEmailSuffixPolicy.defaults.joinToString("\n")))}
    @Test fun normalizeAndDeduplicateKeepsPersonalOrder(){assertEquals(listOf("qq.com","example.org"),HomeEmailSuffixPolicy.parse(" @QQ.com \nexample.org\nqq.com"))}
    @Test fun rejectAddressesLinksAndMalformedDomains(){listOf("a@qq.com","https://qq.com","qq..com","-a.com","a-.com","*.com","邮箱.com","a.123","").forEach{assertTrue(it,runCatching{HomeEmailSuffixPolicy.parse(it)}.isFailure)}}
    @Test fun listAndSizeBounds(){assertTrue(runCatching{HomeEmailSuffixPolicy.parse((0..12).joinToString("\n"){"a$it.com"})}.isFailure);assertTrue(runCatching{HomeEmailSuffixPolicy.parse("a".repeat(4097))}.isFailure)}
    @Test fun privacySelectionCompositionAndVisibilityBlockReads(){assertTrue(HomeEmailSuffixPolicy.canRead(true,false,true,false,false,true));assertFalse(HomeEmailSuffixPolicy.canRead(false,false,true,false,false,true));assertFalse(HomeEmailSuffixPolicy.canRead(true,true,true,false,false,true));assertFalse(HomeEmailSuffixPolicy.canRead(true,false,false,false,false,true));assertFalse(HomeEmailSuffixPolicy.canRead(true,false,true,true,false,true));assertFalse(HomeEmailSuffixPolicy.canRead(true,false,true,false,true,true));assertFalse(HomeEmailSuffixPolicy.canRead(true,false,true,false,false,false))}
    @Test fun onlyAnAtSignTriggersAndChoiceNeverDuplicatesIt(){val candidates=HomeEmailSuffixPolicy.suggestions("@",listOf("qq.com","163.com"));assertEquals(listOf("@qq.com","@163.com"),candidates);assertEquals("qq.com",HomeEmailSuffixPolicy.completion("@qq.com",candidates));assertNull(HomeEmailSuffixPolicy.completion("@bad.com",candidates));listOf(null,"","a","＠","x@").forEach{assertTrue(HomeEmailSuffixPolicy.suggestions(it,listOf("qq.com")).isEmpty())}}
}
