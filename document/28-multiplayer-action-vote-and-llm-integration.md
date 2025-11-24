# 멀티플레이어 행동하기 투표 시스템 및 LLM 연동 계획

**작성일**: 2025-01-24
**상태**: 투표 시스템 구현 완료, LLM 실질 연동 필요

## 1. 개요

멀티플레이어 텍스트 어드벤처 게임에서 "행동하기" 기능을 과반수 투표 시스템으로 구현했습니다.
사용자가 행동하기 버튼을 누르면 투표가 시작되고, 과반수가 찬성하면 LLM에 요청을 보내 다음 스토리를 생성합니다.

### 1.1 주요 변경사항
- KICK 투표(추방)와 ACTION 투표(행동하기)를 구분하여 처리
- ACTION 투표는 과반수 찬성 시 통과 (1명=1표, 2명=1표, 3명=2표 ...)
- KICK 투표는 기존대로 만장일치 필요 (대상자 제외)
- 투표 통과 시 자동으로 LLM 호출 로직 추가

### 1.2 아키텍처 원칙
**프론트엔드에서 LLM 직접 호출 금지**
- API 키 노출 위험
- 클라이언트 측 보안 취약
- 요청 흐름: Frontend → SpringBoot → FastAPI → LLM
- 응답 흐름: LLM → FastAPI → SpringBoot → WebSocket → Frontend

---

## 2. 구현 내용

### 2.1 백엔드 (Spring Boot)

#### 2.1.1 VoteType 열거형 추가
**파일**: `backend/entity/multiplayer/VoteType.java`
```java
public enum VoteType {
    KICK,       // 추방 투표
    ACTION      // 행동하기 투표
}
```

#### 2.1.2 RoomVote 엔티티 수정
**파일**: `backend/entity/multiplayer/RoomVote.java`
- `targetUser` 필드를 `nullable = true`로 변경
- ACTION 투표는 대상자가 없으므로 null 허용

#### 2.1.3 VoteService 리팩토링
**파일**: `backend/service/multiplayer/VoteService.java`

**SecurityContext 의존성 제거**
- WebSocket 핸들러에서는 SecurityContext를 사용할 수 없음
- `startKickVote(roomId, targetUserId, userId)` - userId 파라미터 추가
- `submitBallot(voteId, vote, userId)` - userId 파라미터 추가
- AuthService 대신 UserRepository 사용

**startActionVote() 메서드 추가**
```java
@Transactional
public Long startActionVote(Long roomId, Long userId) {
    // 모든 참가자가 시작 가능 (방장 권한 불필요)
    // targetUser 없이 투표 생성
    // VoteType.ACTION으로 설정
}
```

**과반수 계산 로직**
```java
long requiredVotes;
if (vote.getVoteType() == VoteType.KICK) {
    requiredVotes = activeParticipants - 1;  // 만장일치 (대상자 제외)
} else {
    requiredVotes = (long) Math.ceil(activeParticipants / 2.0);  // 과반수
}
```

**투표 통과 조건**
```java
boolean passed = false;
if (vote.getVoteType() == VoteType.KICK) {
    passed = (yesVotes == requiredVotes);  // 모두 찬성
} else {
    passed = (yesVotes >= requiredVotes);  // 과반수 찬성
}
```

#### 2.1.4 ChatWebSocketController 수정
**파일**: `backend/controller/multiplayer/ChatWebSocketController.java`

**handleAction() 메서드 변경**
- 기존: LLM 직접 호출
- 변경: ACTION 투표 시작
```java
@MessageMapping("/room/{roomId}/action")
public void handleAction(@DestinationVariable Long roomId,
                         SimpMessageHeaderAccessor headerAccessor) {
    Long userId = getUserIdFromSession(headerAccessor);
    Long voteId = voteService.startActionVote(roomId, userId);

    // 투표 시작 메시지 브로드캐스트
    // 투표 상태 메시지 브로드캐스트
}
```

**submitBallot() 메서드 확장**
- ACTION 투표 통과 시 자동으로 LLM 호출
```java
if (voteState.getVoteType().equals("ACTION") &&
    result.getStatus() == VoteStatus.PASSED) {

    // "AI가 생각 중..." 메시지 전송
    llmIntegrationService.generateNextPhase(roomId)
        .thenAccept(...)
        .exceptionally(...);
}
```

#### 2.1.5 커밋 정보
- **Repository**: behindy-back
- **Commit**: 7a69f03
- **Message**: "feat: ACTION 투표 시스템 구현 및 과반수 투표 로직 추가"

---

### 2.2 프론트엔드 (Next.js)

#### 2.2.1 타입 정의 수정
**파일**: `frontend/shared/types/multiplayer/index.ts`

**VoteType 열거형 확장**
```typescript
export enum VoteType {
  KICK = 'KICK',
  ACTION = 'ACTION'
}
```

**RoomVote 인터페이스 수정**
```typescript
export interface RoomVote {
  voteId: number;
  roomId: number;
  voteType: VoteType | string;
  targetUserId?: number;        // optional
  targetUsername?: string;      // optional
  initiatedByUserId: number;
  initiatedByUsername: string;
  status: VoteStatus;
  createdAt: string;
  expiresAt: string;
  yesCount: number;
  noCount: number;
  requiredVotes?: number;
}
```

#### 2.2.2 VotePanel 컴포넌트 개선
**파일**: `frontend/features/multiplayer/components/VotePanel.tsx`

**투표 타입별 UI 분기**
```typescript
const isKickVote = activeVote.voteType === 'KICK';
const isTargetUser = isKickVote && activeVote.targetUserId === currentUserId;

// 제목 표시
{isKickVote ? '추방 투표' : '행동하기 투표'}

// 대상자 표시 (KICK만)
{isKickVote && activeVote.targetUsername && (
  <p>대상: {activeVote.targetUsername}</p>
)}
```

**색상 로직 변경**
```typescript
const getVoteStatusColor = () => {
  if (activeVote.status === VoteStatus.PASSED) {
    return isKickVote ? 'var(--error)' : 'var(--success)';
  }
  if (activeVote.status === VoteStatus.FAILED) {
    return isKickVote ? 'var(--success)' : 'var(--error)';
  }
  // ...
};
```
- KICK 투표: 가결=빨강(나쁨), 부결=초록(좋음)
- ACTION 투표: 가결=초록(좋음), 부결=빨강(나쁨)

#### 2.2.3 디버깅 로그 추가
- `ChatInput.tsx`: handleAction 호출 시 로그
- `useMultiplayerWebSocket.ts`: sendAction, handleMessage 로그

#### 2.2.4 커밋 정보
- **Repository**: behindy-front
- **Commit**: 7e3b34c
- **Message**: "feat: ACTION 투표 UI 지원 및 타입 정의 추가"

---

## 3. 동작 방식

### 3.1 행동하기 투표 플로우

```
1. 사용자가 "행동하기" 버튼 클릭
   ↓
2. Frontend: sendAction() 호출
   ↓ WebSocket: /app/room/{roomId}/action
   ↓
3. Backend: ChatWebSocketController.handleAction()
   ↓
4. VoteService.startActionVote() 호출
   - 투표 생성 (VoteType.ACTION, targetUser=null)
   - 만료 시간 설정 (1분)
   ↓
5. 투표 시작 메시지 브로드캐스트
   - "행동하기 투표가 시작되었습니다"
   - 투표 상태 (voteId, yesCount=0, noCount=0, requiredVotes)
   ↓
6. Frontend: VotePanel 표시
   - 모든 참가자에게 찬성/반대 버튼 표시
   ↓
7. 참가자들이 투표
   - submitBallot(voteId, true/false)
   ↓
8. 투표 수가 requiredVotes에 도달하면 결과 확정
   - 과반수 찬성: PASSED
   - 과반수 미달: FAILED
   ↓
9. [PASSED인 경우]
   - "행동하기 투표가 가결되었습니다" 메시지
   - "AI가 생각 중..." 메시지
   - llmIntegrationService.generateNextPhase() 호출 ⚠️
   ↓
10. [LLM 응답 처리] ⚠️ 미구현
    - 스토리 텍스트 수신
    - HP/Sanity 변경 사항 수신
    - 채팅창에 스토리 표시
    - 참가자 상태 업데이트
```

⚠️ **현재 구현 상태**: 9번까지 완료, 10번 미구현

---

## 4. 아직 남은 작업 (LLM 연동)

### 4.1 현재 상태
- `llmIntegrationService.generateNextPhase(roomId)` 메서드 호출은 구현됨
- 그러나 실제 LLM과의 통신 및 응답 처리 로직이 **미완성**

### 4.2 LlmIntegrationService 현황
**파일**: `backend/service/multiplayer/LlmIntegrationService.java`

**추정되는 현재 구현**
- FastAPI LLM 서버로 HTTP 요청 전송
- 방 정보, 현재 페이즈, 최근 메시지 등을 컨텍스트로 전달
- LLM 응답을 ChatMessage로 저장
- WebSocket을 통해 클라이언트에 브로드캐스트

**필요한 작업**
1. FastAPI 서버와의 실제 통신 구현 확인
2. LLM 프롬프트 설계 및 최적화
3. LLM 응답 파싱 로직 구현
4. HP/Sanity 변경 사항 파싱 및 적용

---

## 5. 다음 단계: MVP 완성

### 5.1 LLM 통신 구현

#### 5.1.1 FastAPI 서버 (behindy-story)
**엔드포인트 설계**
```
POST /api/multiplayer/generate-story
Request:
{
  "roomId": 123,
  "phase": 5,
  "stationName": "강남역",
  "participants": [
    {"characterName": "탐정", "hp": 80, "sanity": 60},
    {"characterName": "기자", "hp": 100, "sanity": 40}
  ],
  "recentMessages": [
    {"username": "user1", "content": "문을 열어보자"},
    {"username": "user2", "content": "조심해야 할 것 같아"}
  ]
}

Response:
{
  "storyText": "문을 여는 순간, 어둠 속에서 무언가가 움직였다...",
  "effects": [
    {"characterName": "탐정", "hpChange": -10, "sanityChange": -5},
    {"characterName": "기자", "hpChange": 0, "sanityChange": -10}
  ],
  "phase": 6
}
```

**구현 파일**
- `llmserver/routers/multiplayer.py`
- OpenAI API 또는 Claude API 호출
- 프롬프트 엔지니어링

#### 5.1.2 SpringBoot 연동
**파일**: `backend/service/multiplayer/LlmIntegrationService.java`

```java
@Service
public class LlmIntegrationService {

    private final RestTemplate restTemplate;
    private final String llmServerUrl; // FastAPI URL

    @Async
    public CompletableFuture<Void> generateNextPhase(Long roomId) {
        // 1. 방 정보 조회
        MultiplayerRoom room = roomRepository.findById(roomId)...;
        List<RoomParticipant> participants = participantRepository.findActiveByRoomId(roomId);
        List<ChatMessage> recentMessages = messageRepository.findRecentUserMessages(roomId, 10);

        // 2. FastAPI 요청 DTO 생성
        LlmStoryRequest request = LlmStoryRequest.builder()
            .roomId(roomId)
            .phase(room.getCurrentPhase())
            .stationName(room.getStation().getStationName())
            .participants(participants.stream()
                .map(p -> new ParticipantContext(...))
                .collect(Collectors.toList()))
            .recentMessages(recentMessages.stream()
                .map(m -> new MessageContext(...))
                .collect(Collectors.toList()))
            .build();

        // 3. FastAPI 호출
        LlmStoryResponse response = restTemplate.postForObject(
            llmServerUrl + "/api/multiplayer/generate-story",
            request,
            LlmStoryResponse.class
        );

        // 4. 스토리 메시지 저장 및 브로드캐스트
        ChatMessageResponse storyMessage = chatMessageService.sendLlmMessage(
            roomId,
            response.getStoryText(),
            response.getPhase()
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, storyMessage);

        // 5. HP/Sanity 변경 적용
        applyEffectsToParticipants(roomId, response.getEffects());

        // 6. Phase 업데이트
        room.setCurrentPhase(response.getPhase());
        roomRepository.save(room);

        return CompletableFuture.completedFuture(null);
    }

    private void applyEffectsToParticipants(Long roomId, List<CharacterEffect> effects) {
        for (CharacterEffect effect : effects) {
            RoomParticipant participant = participantRepository
                .findByRoomIdAndCharacterName(roomId, effect.getCharacterName())
                .orElseThrow();

            int newHp = Math.max(0, participant.getHp() + effect.getHpChange());
            int newSanity = Math.max(0, participant.getSanity() + effect.getSanityChange());

            participant.setHp(newHp);
            participant.setSanity(newSanity);

            // HP가 0이 되면 사망 처리
            if (newHp <= 0) {
                participant.leave();
                // 사망 메시지 브로드캐스트
            }

            participantRepository.save(participant);
        }

        // 변경된 참가자 상태 브로드캐스트
        List<RoomParticipant> updatedParticipants =
            participantRepository.findActiveByRoomId(roomId);
        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId + "/participants",
            updatedParticipants
        );
    }
}
```

#### 5.1.3 프론트엔드 상태 반영
**파일**: `frontend/store/multiplayerStore.ts`

WebSocket 구독 추가:
```typescript
// 참가자 상태 업데이트 구독
client.subscribe('/topic/room/' + roomId + '/participants', (message) => {
  const participants = JSON.parse(message.body);
  setParticipants(participants);

  // HP/Sanity 변경 애니메이션 트리거
  showStatusChangeAnimation(participants);
});
```

### 5.2 HP/Sanity 실시간 반영

#### 5.2.1 데이터베이스 스키마 확인
- `room_participant` 테이블에 `hp`, `sanity` 컬럼 존재 여부 확인
- 없다면 마이그레이션 필요

#### 5.2.2 UI 컴포넌트
**파일**: `frontend/features/multiplayer/components/ParticipantList.tsx`

```typescript
export const ParticipantList: React.FC = () => {
  const { participants } = useMultiplayerStore();

  return (
    <div>
      {participants.map(p => (
        <ParticipantCard key={p.participantId}>
          <CharacterName>{p.characterName}</CharacterName>
          <StatusBar>
            <HPBar value={p.hp} max={100} />
            <SanityBar value={p.sanity} max={100} />
          </StatusBar>
        </ParticipantCard>
      ))}
    </div>
  );
};
```

### 5.3 에러 처리

#### 5.3.1 LLM 응답 실패 처리
```java
.exceptionally(ex -> {
    log.error("LLM 응답 처리 실패: Room {}", roomId, ex);

    // 사용자에게 에러 메시지 전송
    ChatMessageResponse errorMessage = chatMessageService.sendSystemMessage(
        roomId,
        "AI 응답 생성에 실패했습니다. 다시 시도해주세요.",
        Map.of("type", "error")
    );
    messagingTemplate.convertAndSend("/topic/room/" + roomId, errorMessage);

    return null;
});
```

#### 5.3.2 타임아웃 설정
```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(30))  // LLM 응답 대기
        .build();
}
```

---

## 6. 테스트 계획

### 6.1 단위 테스트
- VoteService.startActionVote() 테스트
- 과반수 계산 로직 테스트
- LlmIntegrationService.applyEffectsToParticipants() 테스트

### 6.2 통합 테스트
- 투표 시작 → 투표 → LLM 호출 → 상태 반영 전체 플로우
- 여러 참가자 시뮬레이션 (1명, 2명, 3명, 4명)
- 동시 투표 처리

### 6.3 E2E 테스트
- 실제 브라우저 환경에서 멀티플레이 시뮬레이션
- 여러 탭/기기에서 동시 접속
- WebSocket 재연결 테스트

---

## 7. MVP 체크리스트

- [x] ACTION 투표 시스템 구현
- [x] 과반수 투표 로직 구현
- [x] 프론트엔드 투표 UI 구현
- [ ] FastAPI LLM 엔드포인트 구현
- [ ] SpringBoot ↔ FastAPI 통신 구현
- [ ] LLM 응답 파싱 및 메시지 저장
- [ ] HP/Sanity 변경 적용 로직 구현
- [ ] 참가자 상태 실시간 업데이트
- [ ] 에러 처리 및 재시도 로직
- [ ] 통합 테스트
- [ ] 멀티플레이 E2E 테스트

---

## 8. 참고 문서
- `26-multiplayer-websocket-fix-2025-11-23.md`: WebSocket 인증 구현
- `09-multiplayer-backend-progress.md`: 멀티플레이어 백엔드 구조
- `08-multiplayer-text-adventure-design.md`: 멀티플레이어 설계 문서

---

## 9. 다음 작업 우선순위

1. **LLM 통신 구현** (최우선)
   - FastAPI 엔드포인트 작성
   - SpringBoot 연동 완성
   - 프롬프트 설계

2. **상태 반영 구현**
   - HP/Sanity 변경 적용
   - 참가자 상태 브로드캐스트
   - 프론트엔드 UI 업데이트

3. **테스트 및 디버깅**
   - 로컬 환경에서 멀티플레이 테스트
   - 에러 케이스 처리
   - 성능 최적화

**MVP 완성 예상**: LLM 통신 + 상태 반영 구현 후 즉시 플레이 가능
