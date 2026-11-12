package com.kingzcheung.xime.model

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ModelFileIntegrityTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun expectedBytesAndHashAreBothRequired() {
        val file=temp.newFile();file.writeText("abc")
        val hash="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        verifyModelFile(file,hash,3)
        assertThrows(IOException::class.java){verifyModelFile(file,hash,4)}
        file.writeText("abd")
        assertThrows(IOException::class.java){verifyModelFile(file,hash,3)}
    }
    @Test fun emptyFilesNeverCountAsDownloaded() {
        assertThrows(IOException::class.java){verifyModelFile(temp.newFile(),"")}
        assertThrows(IOException::class.java){verifyModelFile(File(temp.root,"missing"),"")}
    }
    @Test fun extractionRejectsPathsOutsideItsOwnDirectory() {
        val root=temp.newFolder("models")
        assertEquals(File(root,"nested/model.onnx").canonicalFile,safeModelEntry(root,"nested/model.onnx"))
        listOf("../escaped","nested/../../escaped",root.parentFile.absolutePath+"/escaped").forEach { name ->
            assertThrows(IllegalArgumentException::class.java){safeModelEntry(root,name)}
        }
    }

    @Test fun archivesNeedTheirCompletedVerificationMarker() {
        val root=temp.newFolder();val sha="a".repeat(64)
        assertFalse(hasVerifiedModelMarker(root,sha))
        File(root,".verified-model").writeText("a".repeat(63));assertFalse(hasVerifiedModelMarker(root,sha))
        File(root,".verified-model").writeText(sha);assertTrue(hasVerifiedModelMarker(root,sha))
        assertFalse(hasVerifiedModelMarker(root,"b".repeat(64)))
    }
}
