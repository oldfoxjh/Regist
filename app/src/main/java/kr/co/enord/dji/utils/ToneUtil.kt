package kr.co.enord.dji.utils

import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kr.co.enord.dji.DroneApplication

object ToneUtil {
    private var tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    fun beep(durationMs:Int){
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tone.startTone(ToneGenerator.TONE_DTMF_S, durationMs)
        Handler(Looper.getMainLooper()).postDelayed({
            tone.release()
        }, (durationMs+50).toLong())
    }

    fun beep(){
        tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tone.startTone(ToneGenerator.TONE_DTMF_S)
    }

    fun stopBeep(){
        tone.stopTone()
        tone.release()
    }
}