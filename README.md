# talkplus-webrtc-android
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Languages](https://img.shields.io/badge/language-Kotlin-violet.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/sendbird/quickstart-calls-ios/blob/develop/LICENSE.md)
## talkplus-webrtc-android SDK소개
Android용 TalkPlus WebRTC SDK는 Kotlin 언어로 작성되었으며, Android 클라이언트 앱에 음성 및 영상 통화 기능을 구축하는데 사용할 수 있습니다. 이 저장소에서는 TalkPlus WebRTC SDK를 프로젝트에 구현하기 전에 필요한 몇 가지 절차와 Kotlin 언어를 활용하여 작성된 샘플 앱을 찾을 수 있습니다.

> 다자간 통화(그룹 통화)는 지원되지 않으며, 일대일 (1:1) 통화만 가능합니다.

> 통화를 하려면 채널 식별자 (Channel ID) + 유저 식별자 (User ID) 정보가 필요합니다.

## 요구사항
talkplus-webrtc-android SDK 사용을 위한 최소 요구사항

- Gradle : 6.1.1이상
- Android Gradle Plugin : 3.6.1이상
- minSDK : 21이상 (Api Level 5.0이상)
- Java 11 이상

## SDK 설치
```kotlin
// setting.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}
```
```kotlin
// build.gradle
dependencies {
    implementation("com.github.adxcorp:talkplus-calls-android:0.1.0")
}
```

## 시스템 권한 부여
```kotlin
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> (optional)
```

## 의존성 라이브러리리
- WebRTC
- TalkPlus Chat SDK for Android

## 샘플 앱 빌드 및 실행하기
- `talkplus-calls-android-sample` 샘플 앱은 FCM + Notifiaction을 통하여 통화 송, 수신할 수 있도록 구성되어 있습니다.
- 샘플 앱을 테스트하기 위해서 아래의 절차를 따라주십시오.

### TalkPlus 애플리케이션 생성
1. [TalkPlus 대시보드](https://www.talkplus.io/dashboard) 로그인 또는 회원 가입.
2. Apps > 새로운 앱 만들기' 버튼을 클릭하여 톡플러스 애플리케이션 생성
3. Apps > [생성된 앱 이름] > Settings > `App ID` 확인
4. Apps > [생성된 앱 이름] > Settings > `익명 로그인 (Anonymous user)` 활성화
5. Apps > [생성된 앱 이름] > Channel > `채널 생성` 클릭,
6. 채널 타입은 `PUBLIC`으로 선택, `채널명`을 입력 후 생성 버튼 클릭.
7. 위의 절차를 통해서 생성된 `App ID`와 `Channel ID`에 대한 문자열 정보 확인

### 애플리케이션 식별자 (App ID), 채널 식별자 (Channel ID), 유저 식별자 (User ID) 입력
1. 샘플 앱 (안드로이드 이름) 파일 열기
2. Constant.kt에서 `AppId`, `ChanneId`를 이전 단계에서 생성한 값으로 교체
3. MainActivity의 EditText의 순서대로 caller, callee 아이디 입력
> 제공되는 샘플 앱에서는 test4와 test5 유저 식별자 (User ID)를 이용하여 로그인하는 것으로 가정합니다.

> 만약 두 대의 디바이스가 있다면, 첫 번째 디바이스에는 test4라는 유저 식별자를 사용하여 앱을 실행하고, 두 번째 디바이스에서는 유저 식별자 값을 test5로 변경하여 앱을 실행합니다.

## Functions, Interface

### Functions
```kotlin
// 영상통화 요청 
directCall.makeCall(talkPlusCallParams: TalkPlusCallParams, object : OnCallResult {
    override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) {  }
    override fun onFailure(reason: String) {  }
})
```

```kotlin
// 영상통화 요청에 대한 수락
directCall.acceptCall(object : OnCallResult {
    override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) { }
    override fun onFailure(reason: String) { }
```

```kotlin
// 영상통화 종료
directCall.endCall(object : OnCallResult {
    override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) {  }
    override fun onFailure(reason: String) {  }
})

// 영상통화 거절
directCall.decline(object : OnCallResult {
    override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) {  }
    override fun onFailure(reason: String) {  }
})

// 영상통화 취소
directCall.cancel(object : OnCallResult {
    override fun onSuccess(talkPlusCallParams: TalkPlusCallParams) {  }
    override fun onFailure(reason: String) {  }
})
```

```kotlin
// 영상통화 관련 이벤트를 수신하기 위한 리스너 등록
directCall.setDirectCallListener(directCallListener: DirectCallListener)
```

```kotlin
// 영상통화 비디오, 오디오 on, off
directCall.enableVideo(enabled: Boolean)
directCall.enableAudio(enabled: Boolean)
```

```kotlin
// Push Noticiation Payload 데이터 처리 (FirebaseMessageService)
TalkPlus.getNotificationPayload(notificationLink, object : TalkPlus.CallbackListener<TPNotificationPayload> {
    override fun onSuccess(tpNotificationPayload: TPNotificationPayload) {  }
    override fun onFailure(errorCode: Int, e: Exception) {  }
})
```

### Interface (Event, Listener)
```kotlin
interface DirectCallListener {
    fun inComing(talkPlusCallParams: TalkPlusCallParams) // 통화 요청 수신
    fun ended(endCallInfo: EndCallInfo) // 통화 종료, 취소, 거절
    fun connected(talkPlusCallParams: TalkPlusCallParams) // 통화 연결 성공
    fun disConnect(talkPlusCallParams: TalkPlusCallParams) // 연결이 일시적으로 끊긴 경우
    fun failed(talkPlusCallParams: TalkPlusCallParams) // 연결 실패 또는 재연결 불가
    fun error(talkPlusCallParams: TalkPlusCallParams, message: String) // 통화 연결 에러 발생
    fun stateChanged(talkPlusCallParams: TalkPlusCallParams, state: PeerConnection.IceConnectionState) // 통화 연결 상태 확인
}
```

### 작성자
Neptune Company

### 라이선스
talkplus-calls-android SDK는 MIT 라이선스에 따라 사용할 수 있습니다.
