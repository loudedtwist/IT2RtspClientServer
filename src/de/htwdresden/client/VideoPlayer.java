package de.htwdresden.client;

import de.htwdresden.packets.FecPacket;
import de.htwdresden.packets.RtpPacket;

import java.util.*;

public class VideoPlayer {
    private Queue<byte[]> images;
    private Timer timer;
    ClientView view;

    public VideoPlayer(ClientView view) {
        this.images = new ArrayDeque<>();
        this.timer = new Timer();
        this.view = view;
    }

    public void play(int fps, int msDelay){
        timer.scheduleAtFixedRate(new DrawImageTimer(), msDelay, 1000/fps);//40
    }

    public void insertImage(byte[] newImage){
        if(newImage == null || newImage.length == 0) return;
        images.add(newImage);
    }

    private class DrawImageTimer extends TimerTask {
        @Override
        public void run() {
            if(images.size() == 0) return;
            byte[] image = images.poll();
            view.setImage(image);
        }
    }
}
