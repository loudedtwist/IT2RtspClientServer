package de.htwdresden.client;

import com.sun.istack.internal.NotNull;
import de.htwdresden.Statistic;
import de.htwdresden.packets.FecPacket;
import de.htwdresden.packets.RtpPacket;

import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class PacketBuffer {


    private Statistic stats;
    private ClientView view;
    private int receivedPackets = 0;
    private int expectedPacketIndex = 0;        //Expected Sequence number of RTP messages within the session

    Stack<RtpPacket> fecPackets;
    Stack<FecPacket> rtpPackets;

    private Timer timer;
    private boolean firstPacket = true;

    public PacketBuffer(@NotNull Statistic stats, ClientView view) {
        this.stats = stats;
        this.view = view;
        fecPackets = new Stack<>();
        rtpPackets = new Stack<>();
        timer = new Timer();
    }

    public void addPacket(byte[] data, int length) {

        startPlayAtFirstPacket();

        RtpPacket rtpPacket = new RtpPacket(data, length);
        int pt = rtpPacket.getPayloadType();
        if (pt == RtpPacket.MJPEG_TYPE) {
            rtpPackets.add(rtpPacket);
        }
        else if( pt == FecPacket.PAYLOAD_TYPE_FEC){
            fecPackets.add(rtpPacket);
        }

        receivedPackets++;
    }

    private void startPlayAtFirstPacket() {
        if (firstPacket) {
            firstPacket = false;
            timer.scheduleAtFixedRate(new DrawImageTimer(), 2000, 40);
        }
    }

    private void drawPacket(RtpPacket rtpPacket) {

        expectedPacketIndex++;
        int seqNr = rtpPacket.getsequencenumber();

        //print important header fields of the RTP packet received:
        System.out.println("Got RTP packet with SeqNum # " + rtpPacket.getsequencenumber() + " TimeStamp " + rtpPacket.getTimeStamp() + " ms, of type " + rtpPacket.getPayloadType());

        //print header bitstream:
        rtpPacket.printheader();

        //get the payload bitstream from the RTPpacket object
        int payload_length = rtpPacket.getPayloadLength();
        byte[] payload = new byte[payload_length];
        rtpPacket.getPayload(payload);

        //STATISTIC

        if (seqNr > stats.getHighestSeqNr()) {
            stats.setHighestSeqNr(seqNr);
        }
        if (expectedPacketIndex != seqNr) {
            stats.incrementPacketsLost();
            expectedPacketIndex = seqNr;
        }
        stats.increaseTotalBytes(payload_length);

        view.setImage(payload);
    }

    public void updateStatsViewGui() {
        if (stats == null) return;
        view.updateStatsGui(
                stats.getTotalBytes(),
                stats.getPacketsLostFraction(),
                stats.getLostPackets(),
                stats.getDataRate(),
                stats.getHighestSeqNr()
        );
    }

    private class DrawImageTimer extends TimerTask {
        @Override
        public void run() {
            if (rtpPackets.size() == 0) return;
            RtpPacket rtpPacket = rtpPackets.remove(0);
            drawPacket(rtpPacket);
        }
    }
}
