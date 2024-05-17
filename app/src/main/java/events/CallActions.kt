package events

interface CallActions {
    fun onSuccess()
    fun onFailure(reason: String)
}