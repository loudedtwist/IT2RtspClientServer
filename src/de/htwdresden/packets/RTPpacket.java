package de.htwdresden.packets;

import de.htwdresden.Utils.Bytes;

import java.util.Arrays;


public class RtpPacket extends Packet {


    //size of the RTP header:
    static int HEADER_SIZE_RTP = 12;

    /*
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |V=2|P|X|  CC   |M|     PT      |       sequence number         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                           timestamp                           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |           synchronization source (SSRC) identifier            |
   +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
   |            contributing source (CSRC) identifiers             |
   |                             ....                              |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */


    //Bit stream of the RTP header
    byte[] header;
    //Bit stream of the RTP payload
    byte[] payload;
    //size of the RTP payload
    int payload_size;

    public RtpPacket(byte[] header, byte[] payload) {
        //interpret the changing fields of the header:
        PayloadType = getPayloadTypeFromHeader();
        SequenceNumber = getSeqNummerFromHeader();
        TimeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5]) + 16777216 * unsigned_int(header[4]);
        this.header = header;
        this.payload = payload;
        this.payload_size = payload.length;
    }

    //--------------------------
    //Constructor of an RTPpacket object from header fields and payload bitstream
    //FEC PType -> 127
    //--------------------------
    public RtpPacket(int PType, int FrameNr, int time, byte[] data, int data_length) {

        //fill changing header fields:
        //int - 32bit, 4byte
        SequenceNumber = FrameNr;
        TimeStamp = time;
        PayloadType = PType;

        //build the header bistream:
        //--------------------------
        header = getInitializedHeader(PayloadType, SequenceNumber, TimeStamp);


        //fill the payload bitstream:
        //--------------------------
        payload_size = data_length;
        payload = new byte[data_length];

        //fill payload array of byte from data (given in parameter of the constructor)
        payload = Arrays.copyOf(data, payload_size);

    }

    public static byte[] getInitializedHeader(int payloadType, int sequenceNumber, int timeStamp) {

        byte[] header = new byte[HEADER_SIZE_RTP];
        header[0] = (byte) (VERSION << 6 | PADDING << 5 | EXTENSION << 4 | CC);
        header[1] = (byte) (MARKER << 7 | payloadType & 0x000000FF);

        header[2] = (byte) (sequenceNumber >> 8);
        // 00000000 00000000 22222222 11111111 >> 8 => 00000000 00000000 00000000 22222222

        header[3] = (byte) (sequenceNumber & 0xFF);
        //0xff is an int literal (00 00 00 ff) -> value(fe) & 0xff => 00 00 00 fe
        //0xff => 00000000 00000000 00000000 11111111
        //00000000 00000000 22222222 11111111 & 0xff = //00000000 00000000 00000000 11111111

        header[4] = (byte) (timeStamp >> 24);
        // 44444444 33333333 22222222 11111111 >> 24 = 00000000 00000000 00000000 44444444

        header[5] = (byte) (timeStamp >> 16);
        // 44444444 33333333 22222222 11111111 >> 16 = 00000000 00000000 44444444 33333333 (cast to byte) = 33333333

        header[6] = (byte) (timeStamp >> 8);
        // 44444444 33333333 22222222 11111111 >> 8 = 00000000 44444444 33333333 22222222 (cast to byte) = 22222222

        header[7] = (byte) (timeStamp & 0xFF);
        // 44444444 33333333 22222222 11111111 & 0xFF = 00000000 00000000 00000000 11111111 (cast to byte) = 11111111

        header[8] = (byte) (SSRC >> 24);
        header[9] = (byte) (SSRC >> 16);
        header[10] = (byte) (SSRC >> 8);
        header[11] = (byte) (SSRC & 0xFF);
        return header;
    }

    //--------------------------
    //Constructor of an RTPpacket object from the packet bistream
    //--------------------------
    public RtpPacket(byte[] packet, int packet_size) {

        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE_RTP) {
            //get the header bitsream:
            header = new byte[HEADER_SIZE_RTP];
            for (int i = 0; i < HEADER_SIZE_RTP; i++)
                header[i] = packet[i];

            //get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE_RTP;
            payload = new byte[payload_size];
            for (int i = HEADER_SIZE_RTP; i < packet_size; i++)
                payload[i - HEADER_SIZE_RTP] = packet[i];

            //interpret the changing fields of the header:
            PayloadType = getPayloadTypeFromHeader();
            SequenceNumber = getSeqNummerFromHeader();
            TimeStamp = getTimeStampFromHeader();
        }
    }

    public int getTimeStampFromHeader() {
        return unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5]) + 16777216 * unsigned_int(header[4]);
    }

    public int getSeqNummerFromHeader() {
        return unsigned_int(header[3]) + 256 * unsigned_int(header[2]);
    }

    public int getPayloadTypeFromHeader() {
        return header[1] & 127;
    }

    //--------------------------
    //getPayload: return the payload bistream of the RTPpacket and its size
    //--------------------------
    public int getPayload(byte[] data) {

        for (int i = 0; i < payload_size; i++)
            data[i] = payload[i];

        return payload_size;
    }

    //--------------------------
    //getPayload: return the payload bistream of the RTPpacket and its size
    //--------------------------
    public byte[] getPayloadCopy() {
        byte[] copy = new byte[payload_size];
        getPayload(copy);
        return copy;
    }



    //--------------------------
    //getTimeStamp
    //--------------------------
    public int getTimeStamp() {
        return TimeStamp;
    }

    //--------------------------
    //getsequencenumber
    //--------------------------
    public int getsequencenumber() {
        return SequenceNumber;
    }

    //--------------------------
    //getPayloadType
    //--------------------------
    public int getPayloadType() {
        return PayloadType;
    }


    //--------------------------
    //print headers without the SSRC
    //--------------------------
    public void printheader() {
        for (int i = 0; i < (HEADER_SIZE_RTP - 4); i++) {
            for (int j = 7; j >= 0; j--)
                if (((1 << j) & header[i]) != 0)
                    System.out.print("1");
                else
                    System.out.print("0");
            System.out.print(" ");
        }

        System.out.println();
    }


    //--------------------------
    //copyPacketBytesTo: returns the packet bit stream and its length
    //--------------------------
    public int copyPacketBytesTo(byte[] packet) {
        //construct the packet = header + payload
        for (int i = 0; i < HEADER_SIZE_RTP; i++)
            packet[i] = header[i];
        for (int i = 0; i < payload_size; i++)
            packet[i + HEADER_SIZE_RTP] = payload[i];

        //return total size of the packet
        return (payload_size + HEADER_SIZE_RTP);
    }

    //--------------------------
    //getPayloadLength: return the length of the payload
    //--------------------------
    public int getPayloadLength() {
        return payload_size;
    }


    //--------------------------
    //getLength: return the total length of the RTP packet
    //--------------------------
    public int getLength() {
        return payload_size + HEADER_SIZE_RTP;
    }

    //xor to packets, the function takes the length of the biggest array . Remaining fields will be filled with 0
    static RtpPacket xor(RtpPacket p1, RtpPacket p2) {
        byte[] newHeader = Bytes.xor(p1.header, p2.header);
        byte[] newPaload = Bytes.xor(p1.payload, p2.payload);

        return new RtpPacket(newHeader, newPaload);
    }

    //xor to packets, the function takes the length of the biggest array . Remaining fields will be filled with 0
    public void xor(RtpPacket p2) {
        this.header = Bytes.xor(this.header, p2.header);
        this.payload = Bytes.xor(this.payload, p2.payload);
    }
    //xor to packets, the function takes the length of the biggest array . Remaining fields will be filled with 0
    public void xorPayload(RtpPacket p2) {
        this.payload = Bytes.xor(this.payload, p2.payload);
    }
    @Override
    public String toString() {
        String output = super.toString();
        output += "Got RTP packet with SeqNum # " + getsequencenumber() + " TimeStamp " + getTimeStamp() + " ms, of type " + getPayloadType() + "\n";
        return output;
    }

    public void printPacket() {
        //print important header fields of the RTP packet received:
        System.out.printf(toString());
        //print header bitstream:
        printheader();
        System.out.println("");
    }
}
