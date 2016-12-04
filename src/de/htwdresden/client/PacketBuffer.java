package de.htwdresden.client;

import com.sun.istack.internal.NotNull;
import de.htwdresden.Statistic;
import de.htwdresden.Utils.Bytes;
import de.htwdresden.packets.FecPacket;
import de.htwdresden.packets.RtpPacket;

import java.util.*;

public class PacketBuffer {


    private Statistic stats;
    private ClientView view;
    private int receivedPackets = 0;
    private int expectedPacketIndex = 0;        //Expected Sequence number of RTP messages within the session

    private List<FecPacket> fecPackets;
    private Queue<RtpPacket> rtpPackets;
    private List<RtpPacket> rtpPacketBuffer;

    private Timer timer;
    private boolean firstPacket = true;

    public PacketBuffer(@NotNull Statistic stats, ClientView view) {
        this.stats = stats;
        this.view = view;
        fecPackets = new ArrayList<>();
        rtpPackets = new ArrayDeque<>();
        rtpPacketBuffer = new ArrayList<>();
        timer = new Timer();
    }

    public void addPacket(byte[] data, int length) {

        startPlayAtFirstPacket();

        RtpPacket rtpPacket = new RtpPacket(data, length);
        int pt = rtpPacket.getPayloadType();
        if (pt == RtpPacket.MJPEG_TYPE) {
            rtpPackets.add(rtpPacket);
            rtpPacketBuffer.add(rtpPacket);
        } else if (pt == FecPacket.PAYLOAD_TYPE_FEC) {
            FecPacket fecPacket = new FecPacket(data, length);
            fecPackets.add(fecPacket);
            //System.out.println(fecPacket);
        }
        receivedPackets++;
    }

    private void startPlayAtFirstPacket() {
        if (firstPacket) {
            firstPacket = false;
            timer.scheduleAtFixedRate(new DrawImageTimer(), 2000, 40);//40
        }
    }

    private void drawPacket(RtpPacket rtpPacket) {
        if (rtpPacket == null) return;
        expectedPacketIndex++;
        //rtpPacket.printPacket();
        int seqNr = rtpPacket.getsequencenumber();


        //STATISTIC
        stats.updateHighestNr(seqNr);
        stats.updateLostPacketsCounter(seqNr, expectedPacketIndex);

        byte[] payload;
        int payloadLength;
        if (expectedPacketIndex == seqNr) {
            erasePacketFromQueue();
            //get the payload bitStream from the RtpPacket object
            payloadLength = rtpPacket.getPayloadLength();
            payload = new byte[payloadLength];
            rtpPacket.getPayload(payload);
            System.out.println("Playing Seq: " + seqNr + "(O) OK");
        } else {
            FecPacket fecWithLostPacket = findFec(expectedPacketIndex);
            if (fecWithLostPacket == null) {
                System.out.println("");
                System.out.println("Fec packet for the lost packet has not been found.");
                System.out.println("Playing Seq: " + expectedPacketIndex + "(X) LOST AND NOT FOUND");
                System.out.println("");
                return;
            }
            byte[] lostPayload = getPayloadFromFec(fecWithLostPacket, expectedPacketIndex);
            if (lostPayload == null) {
                System.out.println("");
                System.out.println("Playing Seq: " + expectedPacketIndex + "(X) LOST AND NOT FOUND");
                System.out.println("There are not enough packets to restore lost packet from FEC");
                System.out.println(fecWithLostPacket);
                return;
            }
            System.out.println("");
            System.out.println("Playing Seq: " + expectedPacketIndex + "(R) LOST AND HAS BEEN FOUND IN FEC AND RECOVERED");
            System.out.println(fecWithLostPacket);

            payload = lostPayload;
            payloadLength = lostPayload.length;
        }

        stats.increaseTotalBytes(payloadLength);
        view.setImage(payload);
    }


    private void erasePacketFromQueue() {
        rtpPackets.poll();
    }

    private byte[] getPayloadFromFec(FecPacket p, int lostSeqNr) {
        byte[] result = new byte[0];
        for (int i = p.firstSeqNr(); i <= p.lastSeqNr; i++) {
            if (i != lostSeqNr) {
                RtpPacket otherP = getRtpPacketFromBuffer(i);
                //if one of packet, that we need to decode the packet, had not been found
                //we cant decode packet we are looking for, so return null
                if (otherP == null) return null;
                byte[] pl1 = new byte[otherP.getPayloadLength()];
                otherP.getPayload(pl1);
                result = Bytes.xor(pl1, result);
            }
        }
        if (result.length != 0) {
            return Bytes.xor(p.fecPayload, result);
        } else {
            return null;
        }
    }

    private FecPacket findFec(int lostSeqNr) {

        for (Iterator<FecPacket> it = fecPackets.iterator(); it.hasNext(); ) {
            FecPacket p = it.next();
            if( lostSeqNr > p.lastSeqNr) {
                it.remove();
            }
            if (lostSeqNr <= p.lastSeqNr && lostSeqNr >= p.firstSeqNr()) {
                return p;
            }
        }
        return null;
    }

    private RtpPacket getRtpPacketFromBuffer(int seqNr) {
        for (Iterator<RtpPacket> it = rtpPackets.iterator(); it.hasNext(); ) {
            RtpPacket p = it.next();
            if (p.getsequencenumber() == seqNr) return p;
        }
        return null;
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
            RtpPacket rtpPacket = rtpPackets.peek();
            drawPacket(rtpPacket);
        }
    }
}
