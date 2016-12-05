package de.htwdresden;
/* ------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------- */


import de.htwdresden.packets.FecPacketNetworkManager;
import de.htwdresden.packets.FecPacketsSender;
import de.htwdresden.packets.Packet;
import de.htwdresden.packets.RtpPacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Random;
import java.util.StringTokenizer;

public class Server extends JFrame {

    //RTP variables:
    //----------------
    DatagramSocket RTPsocket; //socket to be used to send and receive UDP packets
    DatagramPacket senddp; //UDP packet containing the video frames
    FecPacketNetworkManager fecBuffer;

    InetAddress ClientIPAddr; //Client IP address
    int RTP_dest_port = 0; //destination port for RTP packets  (given by the RTSP Client)

    //GUI:
    //----------------
    JLabel label;

    //Video variables:
    //----------------
    int imageNr = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    static int FRAME_PERIOD = 40; //Frame period of the video to stream, in ms
    static int VIDEO_LENGTH = 500; //length of the video in frames

    Timer timer; //timer used to send the images at the video frame rate
    byte[] buf; //fecBuffer used to store the images to send to the client

    //RTSP variables
    //----------------
    //rtsp states
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    //rtsp message types
    final static int SETUP = 3;
    final static int PLAY = 4;
    final static int PAUSE = 5;
    final static int TEARDOWN = 6;
    final static int OPTIONS = 7;

    static int state; //RTSP Server state == INIT or READY or PLAY
    Socket RTSPsocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    static BufferedReader RTSPBufferedReader;
    static BufferedWriter RTSPBufferedWriter;
    static String VideoFileName; //video file requested from the client
    static int RTSP_ID = 123456; //ID of the RTSP session
    int RTSPSeqNb = 0; //Sequence number of RTSP messages within the session

    static int request_type = INIT;

    final static String CRLF = "\r\n";

    private SendPacketTimer sendPacketTimer;


    JPanel mainPanel = new JPanel();

    private JSlider dropRateSlider;
    private JSlider packetsForOneFecCodePacketSlider;

    //--------------------------------
    //Constructor
    //--------------------------------
    public Server() {

        //init Frame
        super("Server");

        //init Timer
        sendPacketTimer = new SendPacketTimer();
        timer = new Timer(FRAME_PERIOD, sendPacketTimer);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //allocate memory for the sending fecBuffer
        buf = new byte[15000];

        //Handler to close the main window
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //stop the timer and exit
                timer.stop();
                System.exit(0);
            }
        });

        //GUI:
        label = new JLabel("Send frame #        ", JLabel.CENTER);
        dropRateSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, 25);
        packetsForOneFecCodePacketSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, 25);
        packetsForOneFecCodePacketSlider.addChangeListener(e -> {
            if (fecBuffer != null) fecBuffer.changeK(dropRateSlider.getValue());
        });
        dropRateSlider.addChangeListener(e -> sendPacketTimer.setSkipRate(dropRateSlider.getValue() / 100.0f));
        initSlider(dropRateSlider, 0, 100, 10, 0);
        initSlider(packetsForOneFecCodePacketSlider, 2, 20, 2, 2);
        getContentPane().add(label, BorderLayout.NORTH);
        getContentPane().add(dropRateSlider, BorderLayout.CENTER);
        getContentPane().add(packetsForOneFecCodePacketSlider, BorderLayout.SOUTH);
    }

    private void initSlider(JSlider slider, int min, int max, int step, int value) {
        slider.setLabelTable(dropRateSlider.createStandardLabels(step));
        slider.setMinorTickSpacing(step);
        slider.setMinimum(min);
        slider.setMaximum(max);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.setValue(value);
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception {
        //create a Server object
        Server theServer = new Server();
        //show GUI:
        theServer.pack();
        theServer.setVisible(true);

        //get RTSP socket port from the command line
        int RTSPport = Integer.parseInt(argv[0]);

        //Initiate TCP connection with the client for the RTSP session
        ServerSocket listenSocket = new ServerSocket(RTSPport);
        theServer.RTSPsocket = listenSocket.accept();
        listenSocket.close();

        //Get Client IP address
        theServer.ClientIPAddr = theServer.RTSPsocket.getInetAddress();


        //Initiate RTSPstate
        state = INIT;

        //Set input and output stream filters:
        RTSPBufferedReader = new BufferedReader(new InputStreamReader(theServer.RTSPsocket.getInputStream()));
        RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(theServer.RTSPsocket.getOutputStream()));

        //Wait for the SETUP message from the client
        boolean done = false;
        while (!done) {
            request_type = theServer.parse_RTSP_request(); //blocking

            if (request_type == SETUP) {
                done = true;

                //update RTSP state
                state = READY;
                System.out.println("New RTSP state: READY");

                //Send response
                theServer.send_RTSP_response();

                //init the VideoStream object:
                theServer.video = new VideoStream(VideoFileName);

                //init RTP socket
                theServer.RTPsocket = new DatagramSocket();
            } else if (request_type == OPTIONS) {
                theServer.send_RTSP_response();
            }
        }

        //loop to handle RTSP requests
        while (true) {
            //parse the request
            request_type = theServer.parse_RTSP_request(); //blocking

            if ((request_type == PLAY) && (state == READY)) {
                //send back response
                theServer.send_RTSP_response();
                //start timer
                theServer.timer.start();
                //update state
                state = PLAYING;
                System.out.println("New RTSP state: PLAYING");
            } else if ((request_type == PAUSE) && (state == PLAYING)) {
                //send back response
                theServer.send_RTSP_response();
                //stop timer
                theServer.timer.stop();
                //update state
                state = READY;
                System.out.println("New RTSP state: READY");
            } else if (request_type == TEARDOWN) {
                //send back response
                theServer.send_RTSP_response();
                //stop timer
                theServer.timer.stop();
                //close sockets
                theServer.RTSPsocket.close();
                theServer.RTPsocket.close();

                System.exit(0);
            }
        }
    }


    //------------------------
    //Handler for timer
    //------------------------

    //------------------------------------
    //Parse RTSP Request
    //------------------------------------
    private int parse_RTSP_request() {
        int request_type = -1;
        try {
            //parse request line and extract the request_type:
            String RequestLine = RTSPBufferedReader.readLine();
            //System.out.println("RTSP Server - Received from Client:");
            System.out.println(RequestLine);

            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();

            //convert to request_type structure:
            if ((new String(request_type_string)).compareTo("SETUP") == 0)
                request_type = SETUP;
            else if ((new String(request_type_string)).compareTo("PLAY") == 0)
                request_type = PLAY;
            else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
                request_type = PAUSE;
            else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
                request_type = TEARDOWN;
            else if ((new String(request_type_string)).compareTo("OPTIONS") == 0)
                request_type = OPTIONS;

            if (request_type == SETUP || request_type == OPTIONS) {
                //extract videoFileName from RequestLine
                VideoFileName = tokens.nextToken();
            }

            //parse the SeqNumLine and extract CSeq field
            String SeqNumLine = RTSPBufferedReader.readLine();
            System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            RTSPSeqNb = Integer.parseInt(tokens.nextToken());

            if (request_type != OPTIONS) {
                //get LastLine
                String LastLine = RTSPBufferedReader.readLine();
                System.out.println(LastLine);
                if (request_type == SETUP) {
                    //extract RTP_dest_port from LastLine
                    tokens = new StringTokenizer(LastLine);
                    for (int i = 0; i < 3; i++)
                        tokens.nextToken(); //skip unused stuff
                    RTP_dest_port = Integer.parseInt(tokens.nextToken());
                    //TODO FEC INIT
                    FecPacketsSender fecPacketSender = new FecPacketsSender(ClientIPAddr, RTP_dest_port);
                    fecBuffer = new FecPacketNetworkManager(fecPacketSender, packetsForOneFecCodePacketSlider.getValue());
                }

            }
            //else LastLine will be the SessionId line ... do not check for now.
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
        return (request_type);
    }

    //------------------------------------
    //Send RTSP Response
    //------------------------------------
    private void send_RTSP_response() {
        try {
            RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
            RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
            if (request_type == OPTIONS) {
                RTSPBufferedWriter.write("Public: SETUP, TEARDOWN, PLAY, PAUSE" + CRLF);
            } else {
                RTSPBufferedWriter.write("Session: " + RTSP_ID + CRLF);
            }
            RTSPBufferedWriter.flush();
            System.out.println("RTSP Server - Sent response to Client.");
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }


    class SendPacketTimer implements ActionListener {

        Random random = new Random();
        private float skipRatePercent = 0;

        public void setSkipRate(float percent) {
            System.out.println("New Percentege " + percent);
            this.skipRatePercent = percent;
        }

        public void actionPerformed(ActionEvent e) {

            //if the current image nb is less than the length of the video
            if (imageNr < VIDEO_LENGTH) {
                //update current imageNr
                imageNr++;

                try {
                    //get next frame to send from the video, as well as its size
                    int image_length = video.getnextframe(buf);

                    //Builds an RTPpacket object containing the frame
                    RtpPacket rtp_packet = new RtpPacket(Packet.MJPEG_TYPE, imageNr, imageNr * FRAME_PERIOD, buf, image_length);


                    //get to total length of the full rtp packet to send
                    int packet_length = rtp_packet.getLength();

                    //retrieve the packet bitstream and store it in an array of bytes
                    byte[] packet_bits = new byte[packet_length];
                    rtp_packet.copyPacketBytesTo(packet_bits);

                    //send the packet as a DatagramPacket over the UDP socket
                    senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);

                    //TODO ADD PACKET TO FEC BUFFER
                    fecBuffer.addPacket(rtp_packet);

                    if (random.nextFloat() >= skipRatePercent)
                        RTPsocket.send(senddp);

                    System.out.println("Send frame #" + imageNr);
                    //print the header bitstream
                    rtp_packet.printheader();

                    //update GUI
                    label.setText("Send frame #" + imageNr);
                } catch (Exception ex) {
                    System.out.println("Exception caught: " + ex);
                    System.exit(0);
                }
            } else {
                //if we have reached the end of the video file, stop the timer
                timer.stop();
            }
        }
    }
}
