package de.htwdresden.Utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.lang.System.out;


public class BytesTest {

    byte [] header = new byte[20];
    byte [] header2 = new byte[10];

    @Before
    public void setUp() throws Exception {

        header[0] = 12;
        header[1] = 13;
        header[2] = 14;
        header[3] = 44;

        header2[3] = 3;
        header2[4] = 4;
        header2[5] = 5;
        header2[6] = 6;
    }

    @Test
    public void byteXorTest() throws Exception {

        byte b1 = (byte)(header[0]^header2[0]);
        Assert.assertEquals(b1,header[0]);
        byte b2 = (byte)(header[3]^header2[3]);
        Assert.assertEquals(b2, header[3] + header2[3]);
        byte b3 = (byte)(header[5]^header2[5]);
        Assert.assertEquals(b3, header2[5]);
    }

    @Test
    public void xor() throws Exception {
        byte[] result = Bytes.xor(header,header2);
        for (byte b : result) {
            out.print(b + ",");
        }
        Assert.assertEquals(result.length, Math.max(header.length,header2.length));
        out.println(" : New bytes length : " + result.length );;

    }

    @Test
    public void createsArrayWithMaxLengthOfTwo() throws Exception {
        byte[] result = Bytes.xor(header,header2);
        Assert.assertEquals(result.length, Math.max(header.length,header2.length));
    }

    @Test
    public void fillsFreeArrayPlaceWith0() throws Exception {
        byte[] result = Bytes.xor(header,header2);
        Assert.assertEquals(result[10], 0);
    }

    @Test
    public void payloadTypeTestXorWith4Packets() throws Exception {
        byte[] a1  = { 26,127};
        byte[] a2  = { 26,127};
        byte[] a3  = { 26,127};
        byte[] a4  = { 26,127};
        byte[] r1 = Bytes.xor(a1,a2);
        Bytes.print(r1);
        byte[] r2 = Bytes.xor(r1,a2);
        Bytes.print(r2);
        byte[] r3 = Bytes.xor(r2,a3);
        Bytes.print(r3);
        byte[] r4 = Bytes.xor(r3,a4);
        Bytes.print(r4);
    }
}