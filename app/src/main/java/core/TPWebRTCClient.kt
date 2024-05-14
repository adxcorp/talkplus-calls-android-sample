package core

import com.neptune.talkpluscallsandroid.webrtc.core.RtcClient
import com.neptune.talkpluscallsandroid.webrtc.model.RTCConnectionConfig
import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import io.talkplus.TalkPlus
import io.talkplus.entity.user.TPRtcConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.SurfaceViewRenderer
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TPWebRTCClient(val talkPlusCall: TalkPlusCall) {
    private lateinit var rtcClient: RtcClient

    init {
        setTurnServers()
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

    fun enableVideo(isEnabled: Boolean) {
        rtcClient.enableVideo(isEnabled)
    }

    fun enableAudio(isEnabled: Boolean) {
        rtcClient.enableAudio(isEnabled)
    }

    fun endCall() {
//        rtcClient.endCall()
    }

    private fun setRtcClient(rtcConnectionConfig: RTCConnectionConfig): RtcClient {
        return RtcClient(
            context = TalkPlus.getContext(),
            talkplusCall = this.talkPlusCall,
            rtcConnectionConfig = rtcConnectionConfig
        )
    }

    private fun setTurnServers() {
        TalkPlus.getWebRtcConfiguration(object : TalkPlus.CallbackListener<TPRtcConfiguration> {
            override fun onSuccess(tpRtcConfiguration: TPRtcConfiguration) {
                val rtcConnectionConfig = RTCConnectionConfig(
                    turnPassword = tpRtcConfiguration.turnPassword,
                    turnUsername = tpRtcConfiguration.turnUsername,
                    stunServerUris = tpRtcConfiguration.stunServerUris,
                    turnServerUris = tpRtcConfiguration.turnServerUris
                )
                rtcClient = setRtcClient(rtcConnectionConfig)
            }

            override fun onFailure(p0: Int, p1: Exception?) {

            }
        })
    }

    companion object {
        private const val TAG = "TPDirectCall"
    }
}