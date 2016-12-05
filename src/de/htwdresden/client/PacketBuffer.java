package de.htwdresden.client;

import com.sun.istack.internal.NotNull;
import de.htwdresden.Texts;
import de.htwdresden.Utils.Bytes;
import de.htwdresden.packets.FecPacket;
import de.htwdresden.packets.RtpPacket;

import java.util.*;

/**
 * PacketBuffer takes packets(in bytes) from the client and stores it in it's buffer.
 * It's parse and stores RTP and FEC packets differently in Queue(RTP) and List(FEC).
 * @see PacketBuffer#rtpPackets
 * @see PacketBuffer#fecPackets
 *
 * After first packet has been stored, it starts VideoPlayer and FillVideoPlayerQueueTimer.
 * @see PacketBuffer.FillVideoPlayerQueueTimer
 *
 * FillVideoPlayerQueueTimer is started at double of speed of VideoPlayer. It takes
 * Packets saved from PacketBuffer, parses it sequentially and saves Frames stored in RTP Packets
 * in the VideoPlayer. If one of the packets missing PacketBuffer try to find it in FEC Packets.
 * If it find one, it stores in VideoPlayer the right one packet, elsewise it stores the next one.
 *
 * This class also updates statistic data:
 * @see PacketBuffer#stats
 * and provides data for VideoPlayer:
 * @see PacketBuffer#player
 * which is passed as dependency in the constructor and could be controlled by client.
 *
 * @see Texts.BufferTerminalOutput is used to print output log messages.
 */
public class PacketBuffer {

    private Statistic stats;
    private VideoPlayer player;
    private int expectedPacketIndex = 0;        //Expected Sequence number of RTP messages within the session

    private List<FecPacket> fecPackets;
    private Queue<RtpPacket> rtpPackets;

    private Timer timer;
    private boolean firstPacket = true;

    /**
     * PacketBuffer constructor
     * @param stats dependency Statistic class
     * @param videoPlayer dependency VideoPlayer class
     */
    public PacketBuffer(@NotNull Statistic stats, @NotNull VideoPlayer videoPlayer) {
        this.stats = stats;
        player = videoPlayer;
        fecPackets = new ArrayList<>();
        rtpPackets = new ArrayDeque<>();
        timer = new Timer();
    }

    /**
     * Takes bytes of RTP or FEC packet.
     * @param data packet data
     * @param length it's length
     */
    public void addPacket(byte[] data, int length) {
        startPlayAtFirstPacket();

        RtpPacket rtpPacket = new RtpPacket(data, length);
        int pt = rtpPacket.getPayloadType();
        if (pt == RtpPacket.MJPEG_TYPE) {
            rtpPackets.add(rtpPacket);
        } else if (pt == FecPacket.PAYLOAD_TYPE_FEC) {
            FecPacket fecPacket = new FecPacket(data, length);
            fecPackets.add(fecPacket);
        }
        stats.increaseTotalBytes(length);
    }

    private void startPlayAtFirstPacket() {
        if (firstPacket) {
            firstPacket = false;
            int fps = 25;
            player.play(fps, 2000);
            timer.scheduleAtFixedRate(new FillVideoPlayerQueueTimer(), 2000, 1000 / (fps * 2));//40
        }
    }

    private class FillVideoPlayerQueueTimer extends TimerTask {
        @Override
        public void run() {
            RtpPacket rtpPacket = rtpPackets.peek();
            findNextImageForVideoPlayer(rtpPacket);
        }
    }

    private void findNextImageForVideoPlayer(RtpPacket rtpPacket) {
        if (rtpPacket == null) return;
        expectedPacketIndex++;
        int seqNr = rtpPacket.getsequencenumber();


        //STATISTIC
        stats.updateHighestNr(seqNr);
        stats.updateLostPacketsCounter(seqNr, expectedPacketIndex);

        byte[] payload;

        if (expectedPacketIndex == seqNr) {
            erasePacketFromQueue();
            payload = rtpPacket.getPayloadCopy();
            Texts.BufferTerminalOutput.RtpFound(seqNr);
        } else {
            FecPacket fecWithLostPacket = findFec(expectedPacketIndex);
            if (fecWithLostPacket == null) {
                Texts.BufferTerminalOutput.LostNotFound(expectedPacketIndex);
                return;
            }
            byte[] lostPayload = getPayloadFromFec(fecWithLostPacket, expectedPacketIndex);
            if (lostPayload == null) {
                Texts.BufferTerminalOutput.LostFoundCantRecover(expectedPacketIndex,fecWithLostPacket);
                return;
            }
            Texts.BufferTerminalOutput.LostFoundRecovered(expectedPacketIndex,fecWithLostPacket);
            payload = lostPayload;
        }
        player.insertImage(payload);
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
            if (lostSeqNr > p.lastSeqNr) {
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

}

