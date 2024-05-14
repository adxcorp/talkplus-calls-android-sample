package events

import com.neptune.talkpluscallsandroid.webrtc.model.TalkPlusCall
import model.EndCallInfo

interface DirectCallListener {
    fun inComing(talkPlusCall: TalkPlusCall)
    fun connected(talkPlusCall: TalkPlusCall)
    fun ended(endCallInfo: EndCallInfo)
    fun failed()
    fun stateChanged()
    fun error()
}