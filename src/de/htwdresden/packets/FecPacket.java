package de.htwdresden.packets;

public class FecPacket extends Packet {
    //header
    public static final int PAYLOAD_TYPE_FEC = 127;
    static final int HEADER_SIZE_FEC = 10;
    /*
        FEC HEADER

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |K|L|P|X|  CC   |M| PT recovery |            SN base            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          TS recovery                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |        length recovery        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       K - Anzahl der RTP Packeten im FEC Packet
     */

    /*
        RTP HEADER OF FEC PACKET EXAMPLE

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |1 0|0|0|0 0 0 0|0|1 1 1 1 1 1 1|0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1| v_,P,X,_CC__,__PT__,SEQ_NR
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 1| timestamp
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0| SSRC
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   */

    /*
        FEC HEADER EXAMPLE
            0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |0|0|0|0|0 0 0 0|0|0 0 0 0 0 0 0|0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0|
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0|
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |0 0 0 0 0 0 0 1 0 1 1 1 0 1 0 0|
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

      E:         0     [this specification]
      L:         0     [short 16-bit mask]
      P rec.:    0     [0 XOR 0 XOR 0 XOR 0]
      X rec.:    0     [0 XOR 0 XOR 0 XOR 0]
      CC rec.:   0     [0 XOR 0 XOR 0 XOR 0]
      M rec.:    0     [1 XOR 0 XOR 1 XOR 0]
      PT rec.:   0     [11 XOR 18 XOR 11 XOR 18]
      SN base:   8     [min(8,9,10,11)]
      TS rec.:   8     [3 XOR 5 XOR 7 XOR 9]
      len. rec.: 372   [200 XOR 140 XOR 100 XOR 340]

     */
    private static final int LONG_MASK = 0;

    //The P recovery field, the X recovery field, the CC recovery field,
    //the M recovery field, and the PT recovery field are obtained via the
    //protection operation applied to the corresponding P, X, CC, M, and PT
    //values from the RTP header of the media packets associated with the
    //FEC packet.

    private byte[] rtpHeader;
    private byte[] fecHeader;
    public byte[] fecPayload;


    /*
        Timestamp (TS): The timestamp MUST be set to the value of the media
        RTP clock at the instant the FEC packet is transmitted.  Thus, the TS
        value in FEC packets is always monotonically increasing. ????
     */
    public FecPacket(RtpPacket xoredPacket, int k, int lastSeqNr, int lastTimeStamp, int xoredLengthOfAllEncodedPackets) {
        this.PayloadType = PAYLOAD_TYPE_FEC;
        this.TimeStamp = xoredPacket.getTimeStampFromHeader();
        //this.SequenceNumber = rtpHeaderSeqNr;

        this.fecPayload = xoredPacket.payload;

        this.rtpHeader = RtpPacket.getInitializedHeader(PAYLOAD_TYPE_FEC, lastSeqNr, lastTimeStamp);

        this.fecHeader = FecPacket.getInitializedFecHeader(
                k,
                xoredPacket.getPayloadTypeFromHeader(),
                lastSeqNr,
                xoredPacket.getTimeStampFromHeader(),
                xoredLengthOfAllEncodedPackets
        );
    }

    private static byte[] getInitializedFecHeader(int k, int payloadType, int sequenceNumber, int timeStamp, int lengthOfAllEncodedPackets) {
        byte[] fecHeader = new byte[HEADER_SIZE_FEC];
        fecHeader[0] = (byte) (k << 7 | LONG_MASK << 6 | PADDING << 5 | EXTENSION << 4 | CC);
        fecHeader[1] = (byte) (MARKER << 7 | payloadType & 0x00FFFFFF);
        fecHeader[2] = (byte) (sequenceNumber >> 8);
        fecHeader[3] = (byte) (sequenceNumber & 0xFF);
        fecHeader[4] = (byte) (timeStamp >> 24);
        fecHeader[5] = (byte) (timeStamp >> 16);
        fecHeader[6] = (byte) (timeStamp >> 8);
        fecHeader[7] = (byte) (timeStamp & 0xFF);
        fecHeader[8] = (byte) (lengthOfAllEncodedPackets >> 8);
        fecHeader[9] = (byte) (lengthOfAllEncodedPackets & 0xFF);
        return fecHeader;
    }

    @Override
    public int getLength() {
        return fecHeader.length + fecPayload.length + rtpHeader.length;
    }

    @Override
    public int copyPacketBytesTo(byte[] packet) {
        return 0;
    }
}