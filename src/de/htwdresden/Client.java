package de.htwdresden;
/* ------------------
   Client
   usage: java Client [Server hostname] [Server RTSP listening port] [Video file requested]
   ---------------------- */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

import static de.htwdresden.Texts.receivedBytes;

public class Client {

    //GUI
    //----
    JFrame f = new JFrame("Client");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();

    JLabel receivedBytesLabel = new JLabel();
    JLabel lostPacketsLabel = new JLabel();
    JLabel dateRateLabel = new JLabel();
    ImageIcon icon;

    //Helper classes
    Statistic stats;


    //RTP variables:
    //----------------
    private DatagramPacket rcvdp; //UDP packet received from the server
    private DatagramSocket rtpDatagramSocket; //socket to be used to send and receive UDP packets
    private static int RTP_RCV_PORT = 25000; //port where the client will receive the RTP packets

    private Timer timer; //timer used to receive data from the UDP socket
    private byte[] buf; //buffer used to store data received from the server

    //RTSP variables
    private final static String RTSP_VERSION = "RTSP/1.0";
    //----------------

    //rtsp states
    private final static int INIT = 0;
    private final static int READY = 1;
    private final static int PLAYING = 2;
    private static int state; //RTSP state == INIT or READY or PLAYING
    private Socket rtspSocket; //socket used to send/receive RTSP messages
    //input and output stream filters
    private static BufferedReader rtspBufferedReader;
    private static BufferedWriter rtspBufferedWriter;
    //private static PrintWriter rtspBufferedWriter;
    static String videoFileName; //video file to request to the server
    int rtspSeqNb = 0; //Sequence number of RTSP messages within the session
    int rtspId = 0; //ID of the RTSP session (given by the RTSP Server)

    final static String CRLF = "\r\n";



    //Video constants:
    //------------------
    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

    //--------------------------
    //Constructor
    //--------------------------
    public Client() {
        //build GUI
        //--------------------------

        //Frame
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //Buttons
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);
        setupButton.addActionListener(new setupButtonListener());
        playButton.addActionListener(new playButtonListener());
        pauseButton.addActionListener(new pauseButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        //Statistics
        receivedBytesLabel.setText(receivedBytes +"0");
        lostPacketsLabel.setText(Texts.lostPackets + "0");
        dateRateLabel.setText(Texts.dateRate + "0");
        mainPanel.add(receivedBytesLabel);
        mainPanel.add(lostPacketsLabel);
        mainPanel.add(dateRateLabel);

        //Image display label
        iconLabel.setIcon(null);

        //frame layout
        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);

        receivedBytesLabel.setBounds(8,340,380,20);
        lostPacketsLabel.setBounds(8,360,380,20);
        dateRateLabel.setBounds(8,380,380,20);

        iconLabel.setBounds(0, 0, 380, 280);
        buttonPanel.setBounds(0, 280, 380, 50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390, 440));
        f.setVisible(true);

        //init timer
        //--------------------------
        timer = new Timer(20, new timerListener());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        //allocate enough memory for the buffer used to receive data from the server
        buf = new byte[15000];
    }

    //------------------------------------
    //main
    //------------------------------------
    public static void main(String argv[]) throws Exception {
        //Create a Client object
        Client theClient = new Client();

        //get server RTSP port and IP address from the command line
        //------------------
        int RTSP_server_port = Integer.parseInt(argv[1]);
        String ServerHost = argv[0];
        InetAddress ServerIPAddr = InetAddress.getByName(ServerHost);

        //get video filename to request:
        videoFileName = argv[2];

        //Establish a TCP connection with the server to exchange RTSP messages
        //------------------
        theClient.rtspSocket = new Socket(ServerIPAddr, RTSP_server_port);

        //Set input and output stream filters:
        rtspBufferedReader = new BufferedReader(new InputStreamReader(theClient.rtspSocket.getInputStream()));
        rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(theClient.rtspSocket.getOutputStream()));
        //rtspBufferedWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(theClient.rtspSocket.getOutputStream()) ));

        //init RTSP state:
        state = INIT;
    }


    //------------------------------------
    //Handler for buttons
    //------------------------------------

    //.............
    //TO COMPLETE
    //.............

    //Handler for Setup button
    //-----------------------
    private class setupButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            System.out.println("Setup Button pressed !");

            if (state == INIT) {
                //Init non-blocking rtpDatagramSocket that will be used to receive data
                try {
                    rtpDatagramSocket = new DatagramSocket(RTP_RCV_PORT);
                    rtpDatagramSocket.setSoTimeout(5);
                    //construct a new DatagramSocket to receive RTP packets from the server, on port RTP_RCV_PORT

                } catch (SocketException se) {
                    System.out.println("Socket exception: " + se);
                    System.exit(0);
                }

                //init RTSP sequence number
                rtspSeqNb = 1;

                //Send SETUP message to the server
                send_RTSP_request("SETUP");

                //Wait for the response
                if (parse_server_response() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    //change RTSP state and print new state
                    state = READY;
                    System.out.println("New RTSP state: READY");
                }
            }
        }
    }

    //Handler for Play button
    //-----------------------
    private class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("Play Button pressed !");
            ;

            stats = Statistic.start();
            if (state == READY) {

                inkSeqNr();

                //Send PLAY message to the server
                send_RTSP_request("PLAY");

                //Wait for the response
                if (parse_server_response() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    //change RTSP state and print out new state
                    state = PLAYING;
                    System.out.println("New RTSP state: PLAYING" );

                    //start the timer
                    timer.start();
                }
            }
        }
    }


    //Handler for Pause button
    //-----------------------
    private class pauseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //System.out.println("Pause Button pressed !");

            if (state == PLAYING) {
                inkSeqNr();

                //Send PAUSE message to the server
                send_RTSP_request("PAUSE");

                //Wait for the response
                if (parse_server_response() != 200)
                    System.out.println("Invalid Server Response");
                else {
                    //change RTSP state and print out new state

                    state = READY;
                    System.out.println("New RTSP state: PAUSED");

                    //stop the timer
                    timer.stop();
                }
            }
        }
    }

    //Handler for Teardown button
    //-----------------------
    private class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            System.out.println("Teardown Button pressed !");
            inkSeqNr();

            //Send TEARDOWN message to the server
            send_RTSP_request("TEARDOWN");

            //Wait for the response
            if (parse_server_response() != 200)
                System.out.println("Invalid Server Response");
            else {
                //change RTSP state and print out new state
                state = INIT;
                System.out.println("New RTSP state: INIT");

                //stop the timer
                timer.stop();

                //exit
                System.exit(0);
            }
        }
    }


    //------------------------------------
    //Handler for timer
    //------------------------------------

    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    private class timerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            //Construct a DatagramPacket to receive data from the UDP socket
            rcvdp = new DatagramPacket(buf, buf.length);

            try {
                //receive the DP from the socket:
                rtpDatagramSocket.receive(rcvdp);

                //create an RTPpacket object from the DP
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                int seqNr = rtp_packet.getsequencenumber();

                //print important header fields of the RTP packet received:
                System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getsequencenumber() + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type " + rtp_packet.getpayloadtype());

                //print header bitstream:
                rtp_packet.printheader();

                //get the payload bitstream from the RTPpacket object
                int payload_length = rtp_packet.getpayload_length();
                byte[] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                //STATISTIC
                statExpRtpNb++;

                if (seqNr > stats.getHighestSeqNr()) {
                    stats.setHighestSeqNr(seqNr);
                }
                if (statExpRtpNb != seqNr) {
                    stats.incrementPacketsLost();
                }

                stats.increaseTotalBytes(payload_length);
                updateStatsGui();
                //get an Image object from the payload bitstream
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                //display the image as an ImageIcon object
                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            } catch (InterruptedIOException iioe) {
                //System.out.println("Nothing to read");
            } catch (IOException ioe) {
                System.out.println("Exception caught: " + ioe);
            }
        }
    }

    //------------------------------------
    //Parse Server Response
    //------------------------------------
    private int parse_server_response() {
        int reply_code = 0;

        try {
            //parse status line and extract the reply_code:
            String StatusLine = rtspBufferedReader.readLine();
            System.out.println("Received from Server:");
            System.out.println(StatusLine);

            StringTokenizer tokens = new StringTokenizer(StatusLine);
            tokens.nextToken(); //skip over the RTSP version
            reply_code = Integer.parseInt(tokens.nextToken());

            //if reply code is OK get and print the 2 other lines
            if (reply_code == 200) {
                String SeqNumLine = rtspBufferedReader.readLine();
                System.out.println(SeqNumLine);

                String SessionLine = rtspBufferedReader.readLine();
                System.out.println(SessionLine);

                //if state == INIT gets the Session Id from the SessionLine
                tokens = new StringTokenizer(SessionLine);
                tokens.nextToken(); //skip over the Session:
                rtspId = Integer.parseInt(tokens.nextToken());
            }
            System.out.println("-------------------------------");
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }

        return (reply_code);
    }

    //------------------------------------
    //Send RTSP Request
    //------------------------------------

    //.............
    //TO COMPLETE
    //.............

    private void send_RTSP_request(String request_type) {
        try {
            rtspBufferedWriter.write(request_type + " " + videoFileName + " " + RTSP_VERSION + CRLF);
            rtspBufferedWriter.write(getSeqNrString() + CRLF);
            if ("SETUP".equalsIgnoreCase(request_type)) {
                rtspBufferedWriter.write("Transport: RTP/UDP; client_port= " + RTP_RCV_PORT + CRLF);
            } else {
                rtspBufferedWriter.write("Session: " + rtspId + CRLF);
            }
            rtspBufferedWriter.flush();
        } catch (Exception ex) {
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }

    private void inkSeqNr() {
        rtspSeqNb++;
    }

    private String getSeqNrString() {
        return "CSeq: " + rtspSeqNb;
    }

    private void updateStatsGui() {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        receivedBytesLabel.setText(Texts.receivedBytes + stats.getTotalBytes());
        lostPacketsLabel.setText(Texts.lostPackets + formatter.format(stats.getPacketsLostFraction()));
        dateRateLabel.setText(Texts.dateRate + formatter.format(stats.getDataRate()) );
    }

}//end of Class Client

