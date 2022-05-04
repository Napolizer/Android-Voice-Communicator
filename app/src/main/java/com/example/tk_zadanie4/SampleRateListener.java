package com.example.tk_zadanie4;

import android.widget.SeekBar;
import android.widget.TextView;

public class SampleRateListener implements SeekBar.OnSeekBarChangeListener {
    private TextView sampleRate;
    private static final int[] sampleRates = {
            5000,
            11000,
            22000,
            44100,
            48000,
            96000,
            192000,
    };

    public SampleRateListener(TextView sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        sampleRate.setText(format(sampleRates[seekBar.getProgress()]));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public int getSampleRate(int index) {
        return sampleRates[index];
    }

    public String getSampleRateFormatted(int index) {
        return format(getSampleRate(index));
    }

    private String format(int sampleRate) {
        if (sampleRate >= 1000_000) {
            return sampleRate/1_000_000 + "MHz";
        } if (sampleRate >= 1000) {
            return sampleRate / 1000 + "kHz";
        }
        return sampleRate + "Hz";
    }
}
