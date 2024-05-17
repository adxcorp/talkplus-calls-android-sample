package events

import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import model.EndCallInfo
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.PeerConnectionState

interface DirectCallListener {
    fun inComing(talkPlusCall: TalkPlusCall)
    fun connected(talkPlusCall: TalkPlusCall)
    fun ended(endCallInfo: EndCallInfo)
    fun failed(talkPlusCall: TalkPlusCall)
    fun stateChanged(talkPlusCall: TalkPlusCall, state: PeerConnection.IceConnectionState)
    fun disConnect(talkPlusCall: TalkPlusCall)
    fun error()
}