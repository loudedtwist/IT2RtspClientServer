package de.htwdresden.packets;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Send packets to client. Class used to send Fec packet over the UDP Socket
 */
public class FecPacketsSender implements IPacketsSender{
    private DatagramSocket updSocket; //updSocket to be used to send and receive UDP packets


    private InetAddress clientIpAddress; //Client IP address
    private int clientPort = 0; //destination port for RTP packets  (given by the RTSP Client)

    /**
     * Constructor of FecPacketsSender
     * @param clientIpAddress ip address of the client
     * @param port port of the client
     * @throws SocketException if something went wrong
     */
    public FecPacketsSender(InetAddress clientIpAddress, int port) throws SocketException {
        this.updSocket = new DatagramSocket();
        this.clientIpAddress = clientIpAddress;
        this.clientPort = port;
    }

    /**
     * Send packet to client.
     * @param packet packet to send.
     */
    @Override
    public void sendPacket(Packet packet) {

        //get to total length of the full rtp packet to send
        int packet_length = packet.getLength();

        //retrieve the packet bitstream and store it in an array of bytes
        byte[] packet_bits = new byte[packet_length];
        packet.copyPacketBytesTo(packet_bits);

        //send the packet as a DatagramPacket over the UDP updSocket
        DatagramPacket udpPacket = new DatagramPacket(packet_bits, packet_length, clientIpAddress, clientPort);
        try {
            updSocket.send(udpPacket);
        } catch (IOException e) {
            System.out.println("Couldn't send FEC Packet, because of some network issue");
            e.printStackTrace();
        }
    }
}
