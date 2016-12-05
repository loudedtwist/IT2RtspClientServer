package de.htwdresden.client;

import java.util.*;

/**
 * Plays images stored in
 * @see VideoPlayer#images at fixed rate (FPS).
 * If there no images stored, it just wait until next Timer call.
 * @see VideoPlayer#view is used to delegate drawing of the image.
 */
public class VideoPlayer {
    private Queue<byte[]> images;
    private Timer timer;
    ClientView view;

    /**
     * VideoPlayer constructor
     * @param view dependency - ClientView, interface which draws images on display
     */
    public VideoPlayer(ClientView view) {
        this.images = new ArrayDeque<>();
        this.timer = new Timer();
        this.view = view;
    }

    /**
     * Starts VideoPlayer at fixed rate
     * @param fps frame pro second for the VideoPlayer
     * @param msDelay starts video playback after that delay
     */
    public void play(int fps, int msDelay){
        timer.scheduleAtFixedRate(new DrawImageTimer(), msDelay, 1000/fps);
    }

    /**
     * Public interface, used to insert images to play buffer.
     * @param newImage
     */
    public void insertImage(byte[] newImage){
        if(newImage == null || newImage.length == 0) return;
        images.add(newImage);
    }

    /**
     * VideoPlayer timer
     */
    private class DrawImageTimer extends TimerTask {
        @Override
        public void run() {
            if(images.size() == 0) return;
            byte[] image = images.poll();
            view.setImage(image);
        }
    }
}
