package com.example.tk_zadanie4;

import android.widget.SeekBar;
import android.widget.TextView;

public class SampleLevelListener implements SeekBar.OnSeekBarChangeListener {
    private TextView sampleLevel;
    private static final int[] sampleLevels = {
            8,
            16,
    };

    public SampleLevelListener(TextView sampleLevel) {
        this.sampleLevel = sampleLevel;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        sampleLevel.setText(getSampleLevelFormatted(seekBar.getProgress()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public int getSampleLevel(int index) {
        return sampleLevels[index];
    }

    public String getSampleLevelFormatted(int index) {
        return format(getSampleLevel(index));
    }

    private String format(int sampleLevel) {
        return sampleLevel + " bits";
    }
}
