package de.htwdresden;

import de.htwdresden.packets.FecPacket;

/*
* Resource class with strings used in project
* */
public  class Texts {
    static public String receivedBytes = "Received Bytes : ";
    static public String receivedPacktes = "Received Packets : ";
    static public String lostPacketsFraction = "Verlorene Packete (Fraction): ";
    static public String lostPackets = "Verlorene Packete: ";
    static public String dateRate = "DatenRate (bytes/sec): " ;

    static public class BufferTerminalOutput{

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
}
