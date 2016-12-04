package de.htwdresden.client;

import de.htwdresden.packets.FecPacket;

/**
 * Created by warik on 04.12.16.
 */
class BufferTerminalOutput{
    public static void RtpFound(int seqNr){
        System.out.println("Playing Seq: " + seqNr + "(O) OK");
    }

    public static void LostNotFound(int seqNr) {
        System.out.println("");
        System.out.println("Playing Seq: " + seqNr + "(X) LOST AND NOT FOUND");
        System.out.println("Fec packet for the lost packet has not been found.");
        System.out.println("");
    }
    public static void LostFoundCantRecover(int seqNr, FecPacket foundFec) {
        System.out.println("");
        System.out.println("Playing Seq: " + seqNr + "(X) LOST AND NOT FOUND");
        System.out.println("There are not enough packets to restore lost packet from FEC");
        System.out.println(foundFec);
    }
    public static void LostFoundRecovered(int seqNr, FecPacket foundFec) {
        System.out.println("");
        System.out.println("Playing Seq: " + seqNr + "(R) LOST AND HAS BEEN FOUND IN FEC AND RECOVERED");
        System.out.println(foundFec);
    }
}
