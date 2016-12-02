package de.htwdresden;

import java.nio.ByteBuffer;

public class ByteArrayTest {

    byte[] toByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value };
    }

    int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
    // packing an array of 4 bytes to an int, big endian
    int fromByteArray2(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }


    @org.junit.Test
    public void testConvertingIntToBytes(){
        byte[] bytes = toByteArray(123);
        System.out.println(fromByteArray(bytes));
        System.out.println(fromByteArray2(bytes));
    }
}
