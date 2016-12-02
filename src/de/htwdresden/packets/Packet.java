package de.htwdresden.packets;

public abstract class Packet {

    public static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

    /*
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |V=2|P|X|  CC   |M|     PT      |       sequence number         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   */

    static int VERSION = 2;
    static int PADDING = 0;
    static int EXTENSION = 0;
    static int CC = 0;
    static int MARKER = 0;
    static int SSRC = 0;
    int PayloadType;
    int SequenceNumber;
    int TimeStamp;

    //--------------------------
    //getLength: return the total length of the RTP packet
    //--------------------------
    abstract public int getLength();

    //--------------------------
    //copyPacketBytesTo: returns the packet bit stream and its length
    //--------------------------
    abstract public int copyPacketBytesTo(byte[] packet);

}
