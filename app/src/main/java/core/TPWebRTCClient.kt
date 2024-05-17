package core

import com.neptune.talkpluscallsandroid.webrtc.core.RtcClient
import com.neptune.talkpluscallsandroid.webrtc.model.RTCConnectionConfig
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import events.DirectCallListener
import io.talkplus.TalkPlus
import io.talkplus.entity.user.TPRtcConfiguration
import org.webrtc.SurfaceViewRenderer
import java.lang.Exception

class TPWebRTCClient(
    private val talkPlusCall: TalkPlusCall,
    private val directCallListener: DirectCallListener
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
        rtcClient.makeCall(talkplusCall)
    }

    fun acceptCall() {
        rtcClient.acceptCall()
    }

    fun endCall() {
        // TODO enum화
        rtcClient.endCall(
            talkplusCall = talkPlusCall,
            endReasonCode = 1,
            endReasonMessage = "completed"
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
        TalkPlus.getWebRtcConfiguration(object : TalkPlus.CallbackListener<TPRtcConfiguration> {
            override fun onSuccess(tpRtcConfiguration: TPRtcConfiguration) {
                val rtcConnectionConfig = RTCConnectionConfig(
                    turnPassword = tpRtcConfiguration.turnPassword,
                    turnUsername = tpRtcConfiguration.turnUsername,
                    stunServerUris = tpRtcConfiguration.stunServerUris,
                    turnServerUris = tpRtcConfiguration.turnServerUris
                )
                rtcClient = getRtcClient(rtcConnectionConfig)
            }

            override fun onFailure(p0: Int, p1: Exception?) {

            }
        })
    }

    companion object {
        private const val TAG = "TPDirectCall"
    }
}