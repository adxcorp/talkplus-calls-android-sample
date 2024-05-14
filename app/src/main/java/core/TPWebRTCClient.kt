package core

import com.neptune.talkpluscallsandroid.webrtc.core.RtcClient
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import io.talkplus.TalkPlus
import org.webrtc.SurfaceViewRenderer

class TPWebRTCClient(val talkPlusCall: TalkPlusCall) {
    private val rtcClient: RtcClient = setRtcClient()

    fun setLocalVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcClient.setLocalVideo(surfaceViewRenderer)
    }

    fun setRemoteVideo(surfaceViewRenderer: SurfaceViewRenderer) {
        rtcClient.setRemoteVideo(surfaceViewRenderer)
    }

    fun makeCall(talkplusCall: TalkPlusCall) {
        rtcClient.makeCall(talkplusCall)
    }

    fun acceptCall() {
        rtcClient.acceptCall()
    }

    fun enableVideo(isEnabled: Boolean) {
        rtcClient.enableVideo(isEnabled)
    }

    fun enableAudio(isEnabled: Boolean) {
        rtcClient.enableAudio(isEnabled)
    }

    fun endCall() {
//        rtcClient.endCall()
    }

    private fun setRtcClient(): RtcClient {
        return RtcClient(
            context = TalkPlus.getContext(),
            talkplusCall = talkPlusCall,
//            rtcConnectionConfig = rtcConnectionConfig
        )
    }

    companion object {
        private const val TAG = "TPDirectCall"
    }
}