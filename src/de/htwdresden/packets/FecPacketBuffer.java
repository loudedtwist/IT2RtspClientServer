package de.htwdresden.packets;
import java.util.Stack;

/**
 * This is Fec Buffer, which stores packets, sent to client.
 * After number of sent packets riches the k,
 * it sends a Fec packet with xored rtp packets stored previously.
 */
public class FecPacketBuffer  {
    private IPacketsSender packetsSender = null;
    private Stack<RtpPacket> packets;
    private int k = 2;

    public FecPacketBuffer(IPacketsSender packetsSender, int k){
        packets = new Stack<>();
        this.packetsSender = packetsSender;
        this.k = k;
    }

    public void addPacket(RtpPacket packet){
        packets.push(packet);
        if(packets.size() % k == 0){
            FecPacket newFacPacket = buildNewFecPacket();
            packetsSender.sendPacket(newFacPacket);
        }
    }

    /*
    * The length recovery field is used to determine the length of any
   recovered packets.  It is computed via the protection operation
   applied to the unsigned network-ordered 16-bit representation of the
   sums of the lengths (in bytes) of the media payload, CSRC list,
   extension and padding of each of the media packets associated with
   this FEC packet (in other words, the CSRC list, RTP extension, and
   padding of the media payload packets, if present, are "counted" as
   part of the payload).  This allows the FEC procedure to be applied
   even when the lengths of the protected media packets are not
   identical.  For example, assume that an FEC packet is being generated
   by xor'ing two media packets together.  The length of the payload of
   two media packets is 3 (0b011) and 5 (0b101) bytes, respectively.
   The length recovery field is then encoded as 0b011 xor 0b101 = 0b110. */

    private FecPacket buildNewFecPacket(){
        RtpPacket packet = packets.pop();
        int xoredPayloadLength = packet.getPayloadLength();
        int latestTimeStamp = 0 ;
        int latestSeqNr = 0;

        for (int i=0;i<k-1; i++) {
            RtpPacket p = packets.pop();
            xoredPayloadLength ^= p.getPayloadLength();
            packet.xor(p);
            latestSeqNr = packet.SequenceNumber;
            latestTimeStamp = packet.getTimeStamp();
        }
        return new FecPacket(packet,k,latestSeqNr,latestTimeStamp,xoredPayloadLength);
    }
}
