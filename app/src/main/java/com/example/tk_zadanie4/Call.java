package com.example.tk_zadanie4;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Call {
    private MainActivity parentActivity;

    private InetAddress externalIpAddress;
    private int externalPortNumber;

    private DatagramSocket server;

    private int sampleRate;
    private int sampleInterval;
    private int sampleSize;

    private int encodingId;

    private boolean microphoneRunning;
    private boolean speakersRunning;

    private Thread microphoneThread;
    private Thread speakersThread;

    private Call(MainActivity parentActivity, String externalIpAddress, int externalPortNumber, DatagramSocket server,
                 int sampleRate, int sampleInterval, int sampleSize) throws UnknownHostException {
        this.parentActivity = parentActivity;
        this.externalIpAddress = InetAddress.getByName(externalIpAddress);
        this.externalPortNumber = externalPortNumber;
        this.server = server;
        this.sampleRate = sampleRate;
        this.sampleInterval = sampleInterval;
        this.sampleSize = sampleSize;

        switch(sampleSize) {
            case 1:
                encodingId = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case 2:
                encodingId = AudioFormat.ENCODING_PCM_16BIT;
                break;
            default:
                throw new RuntimeException("Nieprawidlowy bitrate: " + (sampleSize * 8));
        }
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    static class Builder {
        public MainActivity parentActivity;

        public String externalIpAddress;
        public int externalPortNumber;

        public DatagramSocket server;

        public int sampleRate;
        public int sampleInterval;
        public int sampleLevel;

        private Builder() {

        }

        public Call build() throws UnknownHostException {
            return new Call(parentActivity, externalIpAddress, externalPortNumber, server, sampleRate, sampleInterval, sampleLevel / 8);
        }
    }

    public void startCall() {
        turnOnMicrophone();
        turnOnSpeakers();
    }

    public void endCall() {
        try {
            stopMicrophone();
            stopSpeakers();
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void turnOnMicrophone() {
        startRecording();
    }

    public void turnOnSpeakers() {
        startReceiving();
    }

    public void turnOffMicrophone() {
        microphoneRunning = false;
    }

    public void turnOffSpeakers() {
        speakersRunning = false;
    }

    private void stopMicrophone() throws InterruptedException {
        if (microphoneRunning) {
            microphoneRunning = false;
            microphoneThread.join();
        }
    }

    private void stopSpeakers() throws InterruptedException {
        if (speakersRunning) {
            speakersRunning = false;
            speakersThread.join();
        }
    }

    private void startRecording() {
        if (!microphoneRunning) {
            if (ActivityCompat.checkSelfPermission(parentActivity.getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                microphoneThread = new Thread(() -> {
                    microphoneRunning = true;

                    AudioRecord microphone = new AudioRecord(
                            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                            sampleRate, AudioFormat.CHANNEL_IN_MONO,
                            encodingId, AudioRecord.getMinBufferSize(
                            sampleRate, AudioFormat.CHANNEL_IN_MONO, encodingId)*10);

                    final int bufferSize = sampleInterval * sampleInterval * sampleSize * 2;
                    byte[] buffer = new byte[bufferSize];

                    try {
                        DatagramSocket socket = new DatagramSocket();

                        microphone.startRecording();

                        while (microphoneRunning) {
                            int bytesRead = microphone.read(buffer, 0, bufferSize);
                            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, externalIpAddress, externalPortNumber);
                            socket.send(packet);
                            //Thread.sleep(sampleInterval/1000, sampleInterval%1000);
                        }

                        microphone.stop();
                        microphone.release();
                        socket.disconnect();
                        socket.close();
                    } catch(Exception e) {
                        microphoneRunning = false;
                        throw new RuntimeException(e);
                    }
                });
                microphoneThread.start();
            } else {
                ActivityCompat.requestPermissions(parentActivity, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
                startRecording();
            }
        }
    }

    private void startReceiving() {
        if (!speakersRunning) {
            speakersRunning = true;
            speakersThread = new Thread(() -> {
                final int bufferSize = sampleInterval * sampleInterval * sampleSize * 2;
                byte[] buffer = new byte[bufferSize];

                AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO, encodingId, bufferSize, AudioTrack.MODE_STREAM);
                audio.play();
                try {
                    server.setSoTimeout(500);
                    while (speakersRunning) {
                        DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
                        try {
                            server.receive(packet);
                            audio.write(packet.getData(), 0, bufferSize);
                        } catch (SocketTimeoutException e) {
                            System.out.println(e.getLocalizedMessage());
                        }
                    }

                    audio.stop();
                    audio.flush();
                    audio.release();
                } catch(Exception e) {
                    speakersRunning = false;
                    throw new RuntimeException(e);
                }
            });

            speakersThread.start();
        }
    }
}
