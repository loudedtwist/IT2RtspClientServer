package de.htwdresden.packets;

import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;


public class FecPacketNetworkManagerTest {
    @Test
    public void toStringTest() throws Exception {
        FecPacket f = new FecPacket(new byte[1000],1000);
        f.k=4;
        f.lastSeqNr=8;
        System.out.println(f);
    }

    FecPacketNetworkManager buffer;
    IPacketsSender packetSender;

    @Before
    public void setUp() throws Exception {
        //Get Client IP address
        InetAddress ip = InetAddress.getLocalHost();
        packetSender = new FecPacketsSender(ip,25000);
        int k = 2;
        buffer =  new FecPacketNetworkManager(packetSender, k);
        //buffer.addPacket(new RTPpacket(...));
        //buffer.addPacket(new RTPpacket(...));
        //weil k = 2 , soll buffer jetzt ein packet an den pcketsender senden, un der schickt es an den Klient.
    }

    @Test
    public void addPacket() throws Exception {
    }

}