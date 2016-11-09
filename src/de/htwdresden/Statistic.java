package de.htwdresden;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Statistic {
    private double dataRate;        //Rate of video data received in bytes/s
    private int totalBytes;         //Total number of bytes received in a session
    private double packetLostRate;        //Rate of video data received in bytes/s
    private float packetLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent

    private double startTime;       //time play button has been pressed
    private double lastTimePoint;
    private double playTime;   //Time in milliseconds of video playing since beginningp
    private Timer timer;

    public double getStartTime(){
        return startTime;
    }
    public double getPlayTime(){
        return playTime;
    }

    public Statistic(){
    }

    public void start(){
        lastTimePoint = startTime = System.currentTimeMillis();
        timer = new Timer(1000, new TimeUpdate());
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
    }

    public void updadateTime(){
        double curTime = System.currentTimeMillis();
        playTime += curTime - lastTimePoint;
        lastTimePoint = curTime;
    }

    private class TimeUpdate implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updadateTime();
        }
    }
}
