# OpenWeatherMap API 설정 가이드

## WeatherAgent2가 실제 날씨 데이터를 사용합니다

`WeatherAgent2`는 OpenWeatherMap API를 사용하여 실제 날씨 정보를 제공합니다.

## 1. API 키 발급 (무료)

1. https://openweathermap.org/ 접속
2. 우측 상단 "Sign In" → "Create an Account" 클릭
3. 이메일, 비밀번호 입력하여 회원가입
4. 이메일 인증 완료
5. 로그인 후 "API keys" 메뉴 클릭
6. 기본 API 키가 자동 생성되어 있음 (또는 "Create Key" 클릭)

## 2. 환경 변수 설정

### Windows (PowerShell)
```powershell
$env:OPENWEATHER_API_KEY="your-api-key-here"
```

### Windows (명령 프롬프트)
```cmd
set OPENWEATHER_API_KEY=your-api-key-here
```

### Linux/Mac
```bash
export OPENWEATHER_API_KEY="your-api-key-here"
```

## 3. application.properties 직접 설정 (대안)

환경 변수 대신 `src/main/resources/application.properties` 파일에 직접 입력:

```properties
openweather.api.key=your-api-key-here
```

## 4. 무료 티어 제한

- **1,000 calls/day** (하루 1,000번 호출 가능)
- **60 calls/minute** (분당 60번 호출 가능)
- 5일 예보 제공 (3시간 간격)
- 현재 날씨 정보

## 5. 사용 가능한 기능

### Current Weather (현재 날씨)
```java
getWeatherInfo(String city, String date)
```
- 현재 기온, 습도, 체감온도
- 날씨 상태 (맑음, 흐림, 비 등)
- 최저/최고 기온

### 5 Day Forecast (5일 예보)
```java
getWeeklyForecast(String city, String startDate, int days)
```
- 3시간 간격 예보
- 일별 최고/최저 기온
- 시간대별 날씨 상태

## 6. 도시 이름 입력 방법

영문 도시 이름을 사용하세요:
- Seoul (서울)
- Busan (부산)
- Jeju (제주)
- Paris (파리)
- London (런던)
- New York (뉴욕)

## 7. 테스트 방법

애플리케이션 실행 후:
1. http://localhost:8080 접속
2. "날씨 에이전트 테스트" 클릭
3. 예시: "Seoul의 날씨를 알려주세요"
4. 예시: "Paris의 5일 날씨 예보를 보여주세요"

## 8. 문제 해결

### API 키 오류
```
401 Unauthorized
```
→ API 키가 잘못되었거나 환경 변수가 설정되지 않음

### 도시를 찾을 수 없음
```
404 Not Found
```
→ 영문 도시 이름으로 다시 시도 (예: "서울" → "Seoul")

### 호출 제한 초과
```
429 Too Many Requests
```
→ 무료 티어 호출 제한 초과 (1,000 calls/day)

## 9. API 키 활성화 대기

새로 발급받은 API 키는 **활성화까지 2시간 정도 소요**될 수 있습니다.
처음에는 401 오류가 발생할 수 있으니 조금 기다린 후 다시 시도하세요.
