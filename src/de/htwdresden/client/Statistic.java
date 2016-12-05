package de.htwdresden.client;

import java.util.Timer;
import java.util.TimerTask;

public class Statistic {
    private double dataRate;         //Rate of video data received in bytes/s
    private int totalBytes;          //Total number of bytes received in a session
    private int packetsLost;        //Number of Packets lost
    private int highestSeqNr;

    private double startTime;       //time play button has been pressed
    private double lastTimePoint;
    private double playTime;   //Time in milliseconds of video playing since beginningp
    private Timer timer;

    public double getStartTime() {
        return startTime;
    }

    public double getPlayTime() {
        return playTime;
    }

    public int getHighestSeqNr() {
        return highestSeqNr;
    }

    public int getTotalBytes(){
        return totalBytes;
    }

    public double getDataRate() {
        return playTime == 0 ? 0 : (totalBytes / (playTime / 1000.0));
    }

    public float getPacketsLostFraction() {
        return highestSeqNr == 0 ? 0 : (float) packetsLost / highestSeqNr;
    }

    public int getLostPackets() {
        return packetsLost;
    }

    public void setHighestSeqNr(int newHighestNr) {
        highestSeqNr = newHighestNr;
    }

    public int increaseTotalBytes(int amount) {
        totalBytes += amount;
        return totalBytes;
    }

    public void incrementPacketsLost() {
        packetsLost++;
    }

    private Statistic() {
    }

    public static Statistic start() {
        Statistic stats = new Statistic();
        stats.dataRate = 0;
        stats.totalBytes = 0;
        stats.packetsLost = 0;
        stats.highestSeqNr = 0;
        stats.startTime = 0;
        stats.playTime = 0;
        stats.lastTimePoint = stats.startTime = System.currentTimeMillis();
        stats.timer = new Timer();
        stats.timer.scheduleAtFixedRate(stats.new TimerUpdateTask(), 0, 1000);
        return stats;
    }

    public void updadateTime() {
        double curTime = System.currentTimeMillis();
        playTime += curTime - lastTimePoint;
        lastTimePoint = curTime;
    }

    public void updateHighestNr(int seqNr) {
        if (seqNr > getHighestSeqNr()) {
            setHighestSeqNr(seqNr);
        }
    }

    public void updateLostPacketsCounter(int seqNr, int expectedPacketIndex) {
        if (expectedPacketIndex != seqNr) {
            incrementPacketsLost();
        }
    }

    private class TimerUpdateTask extends TimerTask {
        @Override
        public void run() {
            updadateTime();
        }
    }
}
