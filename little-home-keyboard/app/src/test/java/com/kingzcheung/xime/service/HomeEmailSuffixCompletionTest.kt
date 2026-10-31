package com.kingzcheung.xime.service

import android.view.inputmethod.InputConnection
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class HomeEmailSuffixCompletionTest {
    private class Editor(initial:String) {
        val text=StringBuilder(initial);var reads=0;var ended=0;var failDelete=false;var failCommit=false
        val ic=Proxy.newProxyInstance(InputConnection::class.java.classLoader,arrayOf(InputConnection::class.java)){_,method,args->
            when(method.name) {
                "getTextBeforeCursor"->{assertEquals(1,args!![0]);reads++;text.takeLast(1)}
                "beginBatchEdit"->true
                "endBatchEdit"->{ended++;true}
                "deleteSurroundingText"->{assertEquals(1,args!![0]);assertEquals(0,args[1]);if(failDelete)false else {text.deleteCharAt(text.lastIndex);true}}
                "commitText"->if(failCommit){failCommit=false;false}else{text.append(args!![0]);true}
                else->null
            }
        } as InputConnection
    }
    @Test fun asciiNeverLosesOrDuplicatesItsAtSign() {
        val editor=Editor("alice@");assertTrue(completeHomeEmailSuffix(editor.ic,"@qq.com",listOf("@qq.com")))
        assertEquals("alice@qq.com",editor.text.toString());assertEquals(1,editor.ended)
    }
    @Test fun fullWidthIsReplacedWithAValidAsciiAddress() {
        val editor=Editor("alice＠");assertTrue(completeHomeEmailSuffix(editor.ic,"@gmail.com",listOf("@gmail.com")))
        assertEquals("alice@gmail.com",editor.text.toString());assertEquals(1,editor.ended)
    }
    @Test fun changedPrefixAndUnknownCandidatesDoNotWriteAnything() {
        val editor=Editor("aliceX");assertFalse(completeHomeEmailSuffix(editor.ic,"@qq.com",listOf("@qq.com")))
        assertEquals("aliceX",editor.text.toString())
        val blocked=Editor("private@");assertFalse(completeHomeEmailSuffix(blocked.ic,"@qq.com",emptyList()))
        assertEquals(0,blocked.reads)
    }
    @Test fun deleteFailureDoesNotAppendADomainOrRemoveTheOriginalSign() {
        val editor=Editor("alice＠").apply{failDelete=true}
        assertFalse(completeHomeEmailSuffix(editor.ic,"@qq.com",listOf("@qq.com")))
        assertEquals("alice＠",editor.text.toString());assertEquals(1,editor.ended)
    }
    @Test fun failedCommitRestoresTheRemovedSignAndEndsBatch() {
        val editor=Editor("alice＠").apply{failCommit=true}
        assertFalse(completeHomeEmailSuffix(editor.ic,"@qq.com",listOf("@qq.com")))
        assertEquals("alice＠",editor.text.toString());assertEquals(1,editor.ended)
    }
}
