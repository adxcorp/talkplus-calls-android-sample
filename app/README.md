# talkplus-webrtc-android
// TODO 뱃지 플랫폼, 언어, 라이센스
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
    ... TODO 
    implementation("org.webrtc:google-webrtc:1.0.32006")
    implementation("com.github.adxcorp:talkplus-android:test-rtc-08")
}
```

## 시스템 권한 부여
```kotlin
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
```

## 의존성 라이브러리리
- WebRTC
- TalkPlus Chat SDK for Android

## 샘플 앱 빌드 및 실행하기

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
3. IntroActivity의 EditText의 순서대로 caller, callee 아이디 입력
> 제공되는 샘플 앱에서는 test1와 test2 유저 식별자 (User ID)를 이용하여 로그인하는 것으로 가정

> 만약 두 대의 디바이스가 있다면, 첫 번째 디바이스에는 test1라는 유저 식별자를 사용하여 앱을 실행하고, 두 번째 디바이스에서는 유저 식별자 값을 test2로 변경하여 앱을 실행.

## Functions, Interface

### Functions
```kotlin
// 영상통화 요청 

```

### Interface (Event, Listener)
