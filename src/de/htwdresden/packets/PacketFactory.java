package de.htwdresden.packets;

public class PacketFactory {
    /*
    //--------------------------
    //Constructor of an RTPpacket object from the packet bistream
    //--------------------------
    public Packet GetInstanceFromByteArray(byte[] packet, int packet_size) {

        //fill default fields:
        VERSION = 2;
        PADDING = 0;
        EXTENSION = 0;
        CC = 0;
        MARKER = 0;
        SSRC = 0;

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
            PayloadType = header[1] & 127;
            SequenceNumber = unsigned_int(header[3]) + 256 * unsigned_int(header[2]);
            TimeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5]) + 16777216 * unsigned_int(header[4]);
        }
    }
    */
}
