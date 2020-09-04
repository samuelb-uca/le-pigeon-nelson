package com.jmtrivial.lepigeonnelson.broadcastplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.jmtrivial.lepigeonnelson.broadcastplayer.messages.BMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import static android.media.AudioManager.STREAM_MUSIC;

public class MessagePlayer extends Handler {

    public static final int playMessage = 0;
    public static final int stopMessage = 1;

    private TextToSpeech tts;
    private HashMap<String, String> map;
    private MediaPlayer mPlayer;

    private boolean isPlaying;

    private UtteranceProgressListener mProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
        } // Do nothing

        @Override
        public void onError(String utteranceId) {
        } // Do nothing.

        @Override
        public void onDone(String utteranceId) {
            isPlaying = false;
            Log.d("MessagePlayer", "end of TTS");
            messageQueue.sendEmptyMessage(messageQueue.nextMessage);
        }

    };

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            isPlaying = false;
            Log.d("MessagePlayer", "end of audio play");
            messageQueue.sendEmptyMessage(messageQueue.nextMessage);
        }

    };

    private MessageQueue messageQueue;

    public BMessage getCurrentMessage() {
        if (isPlaying)
            return currentMessage;
        else
            return null;
    }

    private BMessage currentMessage;


    public MessagePlayer(Context context) {

        this.messageQueue = null;
        this.isPlaying = false;

        // set text-to-speech method with the good language
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                        tts.setLanguage(Locale.FRANCE);
                }
                tts.setOnUtteranceProgressListener(mProgressListener);
            }
        });
        map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(STREAM_MUSIC);
        mPlayer.setOnCompletionListener(onCompletionListener);
    }

    @Override
    public final void handleMessage(Message msg) {
        if (msg.what == stopMessage) {
            Log.d("MessagePlayer", "stop message");
            stopRendering();
        } else if (msg.what == playMessage) {
            Log.d("MessagePlayer", "play message");
            stopRendering();
            currentMessage = (BMessage) msg.obj;
            renderMessage();
        }
    }

    private void renderMessage() {
        if (currentMessage.isText()) {
            tts.setLanguage(new Locale(currentMessage.getLang()));
            tts.speak(currentMessage.getTxt(), TextToSpeech.QUEUE_FLUSH, map);
            isPlaying = true;
        }
        else if (currentMessage.isAudio()) {
            // play audio file
            try {
                mPlayer.reset();
                mPlayer.setDataSource(currentMessage.getAudioURL());
                mPlayer.prepare();
                mPlayer.start();
                isPlaying = true;
            } catch (IOException e) {
            }
        }

    }

    private void stopRendering() {
        isPlaying = false;
        tts.stop();
        mPlayer.reset();
    }

    public void registerQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }
}
