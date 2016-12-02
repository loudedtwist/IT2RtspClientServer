package de.htwdresden.client;

/**
 * Created by warik on 30.11.16.
 */
public interface ClientView {
    void setImage(byte[] payload);
    void updateStatsGui(int totalBytes, float packetsLostFraction, int lostPackets, double dataRate, int highestSeqNr);
}
