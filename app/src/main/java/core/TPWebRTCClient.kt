package core

import android.util.Log
import com.neptune.talkpluscallsandroid.webrtc.core.RtcClient
import com.neptune.talkpluscallsandroid.webrtc.model.RTCConnectionConfig
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import events.DirectCallListener
import io.talkplus.TalkPlus
import io.talkplus.entity.user.TPRtcConfiguration
import org.webrtc.SurfaceViewRenderer
import java.lang.Exception

class TPWebRTCClient(
    var talkPlusCall: TalkPlusCall,
    private val directCallListener: DirectCallListener,
    private val rtcConnectionConfig: RTCConnectionConfig
) {
    private lateinit var rtcClient: RtcClient

    init {
        setRtcClient()
    }

    fun setLocalVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcClient.setLocalVideo(surfaceViewRenderer)
    }

    fun setRemoteVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcClient.setRemoteVideo(surfaceViewRenderer)
    }

    fun makeCall(talkplusCall: TalkPlusCall) {
        rtcClient.setTalkPlusCall(talkplusCall)
        rtcClient.makeCall(talkplusCall)
    }

    fun acceptCall() {
        rtcClient.setTalkPlusCall(talkPlusCall)
        rtcClient.acceptCall()
    }

    fun endCall() {
        // TODO enum화
        rtcClient.setTalkPlusCall(talkPlusCall)
        rtcClient.endCall(
            talkplusCall = talkPlusCall,
            endReasonCode = 1,
            endReasonMessage = "completed"
        )
    }

    fun decline() {
        // TODO enum화
        rtcClient.setTalkPlusCall(talkPlusCall)
        rtcClient.endCall(
            talkplusCall = talkPlusCall,
            endReasonCode = 2,
            endReasonMessage = "decline"
        )
    }

    fun cancel() {
        rtcClient.setTalkPlusCall(talkPlusCall)
        rtcClient.endCall(
            talkplusCall = talkPlusCall,
            endReasonCode = 3,
            endReasonMessage = "cancel"
        )
    }

    fun enableVideo(isEnabled: Boolean) {
        rtcClient.enableVideo(isEnabled)
    }

    fun enableAudio(isEnabled: Boolean) {
        rtcClient.enableAudio(isEnabled)
    }

    private fun getRtcClient(rtcConnectionConfig: RTCConnectionConfig): RtcClient {
        return RtcClient(
            context = TalkPlus.getContext(),
            talkplusCall = this.talkPlusCall,
            rtcConnectionConfig = rtcConnectionConfig,
            directCallListener
        )
    }

    private fun setRtcClient() {
        rtcClient = getRtcClient(rtcConnectionConfig)
    }

    companion object {
        private const val TAG = "TPDirectCall"
    }
}