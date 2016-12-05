package de.htwdresden.packets;

import static de.htwdresden.packets.RtpPacket.HEADER_SIZE_RTP;

/**
 * FecPacket handles creation from xor'ed packet and parameters
 * and reconstruction from byte array, which represent the Fec packet.
 * Fec packet has:
 * @see FecPacket#rtpHeader RTP Header(for back compatibilities with clients,
 * who don't implement Fec correction),
 * @see FecPacket#fecHeader FEC Header with fields:
 *      @see FecPacket#k number of encodet packets and
 *      @see FecPacket#lastSeqNr last RTP packet encoded with this FEC packet and
 * @see FecPacket#fecPayload wich has encoded ( XOR ) payload of K packets
 * encoded in this FEC packet.
 * This FecPacket has simplified FEC header and contains only k, sn, ts and length.
 */
public class FecPacket extends Packet {

    public static final int PAYLOAD_TYPE_FEC = 127;
    static final int HEADER_SIZE_FEC = 10;
    /*
        OUR FEC HEADER

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | K                             |            SN                 |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          TS recovery                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |        length recovery        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       K - Anzahl der RTP Packeten im FEC Packet
       SN - Sequence index of last stored rtp packet
     */

    /*
       FEC HEADER

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |E|L|P|X|  CC   |M| PT recovery |            SN base            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                          TS recovery                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |        length recovery        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
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
    //RTP HEADER - payload = 127, lastSeq = ...
    //FEC HEADER - Kx xored RTP HEADER or simpler [2Bytes K]
    //XORED PAYLOAD - from Kx xored rtp packets

    private byte[] rtpHeader;
    private byte[] fecHeader;
    public byte[] fecPayload;

    public int k;
    public int lastSeqNr;
    int lastTimeStamp;

    /*
        Timestamp (TS): The timestamp MUST be set to the value of the media
        RTP clock at the instant the FEC packet is transmitted.  Thus, the TS
        value in FEC packets is always monotonically increasing. ????
     */
    public FecPacket(RtpPacket xoredPacket, int k, int lastSeqNr, int lastTimeStamp, int xoredLengthOfAllEncodedPackets) {
        this.PayloadType = PAYLOAD_TYPE_FEC;
        this.TimeStamp = xoredPacket.getTimeStampFromHeader();

        this.fecPayload = xoredPacket.payload;
        this.k = k;
        this.lastSeqNr = lastSeqNr;
        this.lastTimeStamp = lastTimeStamp;

        this.rtpHeader = RtpPacket.getInitializedHeader(PAYLOAD_TYPE_FEC, lastSeqNr, lastTimeStamp);

        this.fecHeader = FecPacket.getInitializedFecHeader(
                k,
                xoredPacket.getPayloadTypeFromHeader(),
                lastSeqNr,
                xoredPacket.getTimeStampFromHeader(),
                xoredLengthOfAllEncodedPackets
        );
    }

    public FecPacket(byte[] packet, int packet_size) {
        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE_RTP + HEADER_SIZE_FEC) {
            //get the rtpHeader bitsream:
            rtpHeader = new byte[HEADER_SIZE_RTP];
            System.arraycopy(packet, 0, rtpHeader, 0, HEADER_SIZE_RTP);

            //get the fecHeader bitsream:
            fecHeader = new byte[HEADER_SIZE_FEC];
            System.arraycopy(packet, HEADER_SIZE_RTP, fecHeader, 0, HEADER_SIZE_FEC);

            //get the payload bitstream:
            int fecPayloadSize = packet_size - HEADER_SIZE_RTP - HEADER_SIZE_FEC;
            fecPayload = new byte[fecPayloadSize];
            System.arraycopy(packet, HEADER_SIZE_RTP + HEADER_SIZE_FEC, fecPayload, 0, packet_size - (HEADER_SIZE_RTP + HEADER_SIZE_FEC));

            k = unsigned_int(fecHeader[1]) + 256 * unsigned_int(fecHeader[0]);
            lastSeqNr = unsigned_int(fecHeader[3]) + 256 * unsigned_int(fecHeader[2]);
        }
    }

    private static byte[] getInitializedFecHeader(int k, int payloadType, int sequenceNumber, int timeStamp, int lengthOfAllEncodedPackets) {
        byte[] fecHeader = new byte[HEADER_SIZE_FEC];
        fecHeader[0] = (byte) (k >> 8);
        fecHeader[1] = (byte) (k & 0xFF);
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
        System.arraycopy(rtpHeader, 0, packet, 0, HEADER_SIZE_RTP);
        System.arraycopy(fecHeader, 0, packet, HEADER_SIZE_RTP, HEADER_SIZE_FEC);
        System.arraycopy(fecPayload, 0, packet, HEADER_SIZE_RTP + HEADER_SIZE_FEC, fecPayload.length);
        return HEADER_SIZE_RTP + HEADER_SIZE_FEC + fecPayload.length;
    }

    public int firstSeqNr() {
        return lastSeqNr - k + 1;
    }

    /**
     * returns text representation of this object
     * @return text representation
     */
    @Override
    public String toString() {
        String output = super.toString() + "\n";
        output += "     FEC PACKET" + "\n";
        output += "     K:" + k + "\n";
        output += "     LastSeqNr: " + lastSeqNr + "\n";
        return output;
    }
}
