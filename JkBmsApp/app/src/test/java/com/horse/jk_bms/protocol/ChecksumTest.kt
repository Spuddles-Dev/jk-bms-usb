package com.horse.jk_bms.protocol

import org.junit.Assert.*
import org.junit.Test

class ChecksumTest {

    @Test
    fun testCalculateValidFrame() {
        val frame = ByteArray(300)
        frame[0] = 0x55.toByte()
        frame[1] = 0xAA.toByte()
        frame[2] = 0xEB.toByte()
        frame[3] = 0x90.toByte()
        frame[4] = 0x01.toByte()
        frame[5] = 0x00.toByte()
        
        val checksum = Checksum.calculate(frame)
        val expectedSum = (0x55 + 0xAA + 0xEB + 0x90 + 0x01 + 0x00) and 0xFF
        assertEquals(expectedSum.toByte(), checksum)
    }

    @Test
    fun testValidateCorrectChecksum() {
        val frame = ByteArray(300)
        frame[0] = 0x55.toByte()
        frame[1] = 0xAA.toByte()
        frame[2] = 0xEB.toByte()
        frame[3] = 0x90.toByte()
        frame[4] = 0x01.toByte()
        frame[5] = 0x00.toByte()
        val checksum = Checksum.calculate(frame)
        frame[299] = checksum
        
        assertTrue("Should validate with correct checksum", Checksum.validate(frame))
    }

    @Test
    fun testValidateIncorrectChecksum() {
        val frame = ByteArray(300)
        frame[0] = 0x55.toByte()
        frame[1] = 0xAA.toByte()
        frame[2] = 0xEB.toByte()
        frame[3] = 0x90.toByte()
        frame[4] = 0x01.toByte()
        frame[5] = 0x00.toByte()
        frame[299] = 0x00.toByte()
        
        assertFalse("Should not validate with incorrect checksum", Checksum.validate(frame))
    }

    @Test
    fun testValidateTooShortFrame() {
        val frame = ByteArray(200)
        assertFalse("Should not validate frame shorter than 300 bytes", Checksum.validate(frame))
    }

    @Test
    fun testWriteChecksum() {
        val frame = ByteArray(300)
        frame[0] = 0x55.toByte()
        frame[1] = 0xAA.toByte()
        frame[2] = 0xEB.toByte()
        frame[3] = 0x90.toByte()
        frame[4] = 0x01.toByte()
        frame[5] = 0x00.toByte()
        
        Checksum.writeChecksum(frame)
        
        val expectedSum = (0x55 + 0xAA + 0xEB + 0x90 + 0x01 + 0x00) and 0xFF
        assertEquals(expectedSum.toByte(), frame[299])
    }

    @Test
    fun testChecksumWrapAround() {
        val frame = ByteArray(300)
        for (i in 0 until 299) {
            frame[i] = 0xFF.toByte()
        }
        
        val checksum = Checksum.calculate(frame)
        val expectedSum = (299 * 0xFF) and 0xFF
        assertEquals(expectedSum.toByte(), checksum)
    }
}
