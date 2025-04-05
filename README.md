# 항해플러스 1주차 - TDD로 개발하기

<br>

## 심화과제 요구 사항
- 포인트 관련 기능 동시성 이슈 제어하기
- 이를 검증하는 통합 테스트 작성하기

<br>

---
## [1] 동시성 이슈는 왜 발생할까?
- 자바는 멀티스레드 환경이기 때문이다.
- 서버가 단일이라 하더라도
    - 여러 스레드가 동시에 자원을 수정하거나 접근할 때 Race Condition(경쟁 조건)이 발생할 수 있다.

<br>
** 멀티스레드 : 하나의 프로세스 내에서 여러 작업이 동시에 수행되는 구조<br>
** Race Condition : 둘 이상의 스레드가 동시에 같은 자원에 접근하면서 실행 순서에 따라 예기치 않은 결과가 발생하는 상황 <br>

<br><br>

---

## [2] 동시성 문제는 실제로 어떻게 발생할까?
예시: 포인트를 동시에 충전할 때 <br>
- 유저 ID 1번이 존재함 <br>
- 100P 충전 요청을 5개 스레드에서 동시에 보냄 <br>
- 기대값: 100P × 5 = 500P <br>

<br>

### 그런데 동시성 제어 없이 실행하면?
실제 포인트: 100P <br>
→ 일부 요청만 반영됨

![image](https://github.com/user-attachments/assets/ee6af5d3-28b3-48eb-8978-32ff9ad1918b)


<br>

### 왜 이런 문제가 생길까?
- 여러 스레드가 동시에 현재 포인트 100P를 읽음<br>
- 각각 100 + 100 → 200으로 계산<br>
- 동시에 저장하면 가장 마지막 요청만 반영됨 (덮어쓰기 현상)<br>
- 총 500P를 충전했지만 마지막 요청만 살아남아 100P만 반영<br>

<br>

### 결론
- 멀티스레드 환경에서는 자원 접근 순서를 보장할 수 없음
- 공유 자원에는 락(lock) 등 동기화 처리가 필수
- 동시성 미제어 시 데이터 손실 발생 가능 → 예상과 다른 결과

<br><br>

## [3] 동시성 이슈를 해결하기 위해 내가 선택한 방법

<br>
✅ 해결 전략 <br>
사용자별로 락을 분리하여 자원 접근을 순차적으로 제어함 <br>

<br>

✅ 사용 기술 조합 <br>
ConcurrentHashMap + ReentrantLock 조합

<br>

✅ 왜 이 방법을 택했나? <br>
같은 사용자에 대해 여러 요청이 동시에 들어왔을 때 충돌이 발생함 <br>
같은 사용자에 대한 요청을 순차적으로 처리하는 것이 필요 <br>

userId 기준으로 락을 분리하면 → 동일 사용자에 대한 요청만 순차 처리되도록 만들 수 있음 <br>

(ex) userId 1번에 대한 Lock은 하나이고, 그 Lock을 얻은 스레드만 접근 가능

<br>

✅ 구체적인 내용
- ConcurrentHashMap을 사용하여 사용자 ID별로 락을 분리해 관리함 <br>
<br>

- 동일한 사용자에게 동시에 여러 요청이 들어오더라도 하나씩 차례로 처리되도록 함 <br>
반면, 서로 다른 사용자들의 요청은 동시에 처리될 수 있도록 함 <br>
<br>

- ReentrantLock을 통해 락을 획득한 스레드만 해제 가능하도록 제어 <br>
  → 락 소유 스레드만 해제 가능하므로 예외나 버그 방지에 효과 <br>
<br>

- 전체 구조는 Java 기본 라이브러리로만 구현되어 <br>
  → 외부 라이브러리 없이 가볍고 효율적인 동시성 처리가 가능함

![image](https://github.com/user-attachments/assets/f6295b96-c911-41d6-ab92-18b2015b7c73)

<br><br><br>


## [4] 그 외 동시성 이슈를 해결하기 위해 생각한 방법

### (1) synchronized 
- synchronized는 간단한 동기화 방법이지만, <br>
임계 영역 전체를 하나의 락으로 감싸기 때문에 <br>
동시성이 필요 없는 상황까지 차단할 수 있음. <br>
<br>

- 예를 들어, 서로 다른 사용자에 대한 요청도 모두 직렬화되기 때문에 <br>
사용자 A의 요청이 사용자 B의 요청을 불필요하게 기다리는 병목이 발생할 수 있음 <br>
<br>

- 또한 객체 단위로 락이 걸리기 때문에 <br>
userId 단위로 락을 분리하는 세밀한 제어가 어려움. <br>
<br>

- 이런 이유로 사용자별로 락을 분리할 수 있는 <br>
 `ConcurrentHashMap + ReentrantLock 조합` 선택함. <br>

<br>

```java
// ❌ synchronized: 모든 요청을 직렬화 (userId 무시)
public class SynchronizedPointService {
    private long point = 0;

    public synchronized void charge(Long userId, long amount) {
        point += amount;
        // userId가 달라도 동시에 실행 불가
    }
}

// ✅ ReentrantLock: userId별로 락 분리 
public class ReentrantPointService {
    private final Map<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    private final Map<Long, Long> userPoints = new ConcurrentHashMap<>();

    public void charge(Long userId, long amount) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, id -> new ReentrantLock());
        lock.lock();
        try {
            long current = userPoints.getOrDefault(userId, 0L);
            userPoints.put(userId, current + amount);
        } finally {
            lock.unlock();
        }
    }
}

```

<br><br>

### (2) AtomicLong 
- AtomicLong은 CAS(Compare-And-Swap) 기반의 낙관적 락으로 <br> 
락 없이도 동시성 제어가 가능한 경량화된 방식. <br><br>

- 단순한 값 증가/감소에는 적합하지만 <br> 
이번 과제처럼 포인트 조회 → 계산 → 저장 → 히스토리 기록처럼 <br>
여러 작업이 묶여 있는 흐름에는 원자성이 보장되지 않음. <br><br>

- 전체 로직을 보호하기에는 범위가 좁기 떄문에
락 제어와 범위 지정이 가능한 `ConcurrentHashMap + ReentrantLock 조합` 선택함. <br>

<br>

```java
// ❌ AtomicLong: 값은 안전하지만 복합 로직 보호 어려움
public class AtomicPointService {
    private final AtomicLong point = new AtomicLong(1000);

    public void use(long amount) {
        long current;
        long updated;
        do {
            current = point.get();
            if (amount > current) throw new IllegalArgumentException("잔액 부족");
            updated = current - amount;
        } while (!point.compareAndSet(current, updated));

        // 이 아래 로직은 원자적으로 보호되지 않음
        // ex) 히스토리 저장, 알림 전송 등
    }
}

// ✅ ReentrantLock: 복합 로직 전체를 명확하게 보호 가능
public class LockPointService {
    private final ReentrantLock lock = new ReentrantLock();
    private long point = 1000;

    public void use(long amount) {
        lock.lock();
        try {
            if (amount > point) throw new IllegalArgumentException("잔액 부족");
            point -= amount;

            // 이 안에 있는 모든 로직은 동시성  보호됨
            // ex) 히스토리 저장, 로그 기록 등
        } finally {
            lock.unlock();
        }
    }
}

```

