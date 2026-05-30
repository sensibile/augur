# Augur

Augur는 Kotlin 기반 feature flag 및 rule branching engine 실험 프로젝트입니다.

현재 초점은 Spring 없는 순수 rule core를 먼저 안정화하는 것입니다.

- functional core, imperative shell
- 도메인 경계에서 Value Object 적극 사용
- UUID v7 기반 식별자
- Kotlin 기본 `Result` 대신 가벼운 `Outcome<E, A>` 결과 처리
- mocking framework 대신 실제 도메인 객체, fake, stub 중심 테스트
- 누락된 테스트를 찾기 위한 Kover 기반 coverage 리포트
- Tidy First 방식의 작은 정리와 타입/함수 합성 중심 코어 로직

canonical rule 저장 포맷은 [docs/rule-json-format.md](docs/rule-json-format.md)에 정리되어 있습니다.

## 현재 모듈

- `augur-rule-core`: Spring, HTTP, DB, serialization 의존성이 없는 순수 평가 코어
- `augur-rule-json`: canonical JSON AST와 core domain 사이의 serialization adapter
- `augur-rule-sdk`: Kotlin 사용자를 위한 request builder와 SDK 편의 계층

## End-To-End 사용 예시

```kotlin
val ruleSet =
    when (val result = RuleJson.decodeValidRuleSet(json)) {
        is Outcome.Err -> error("Invalid rule set: ${result.error}")
        is Outcome.Ok -> result.value
    }

val request =
    when (
        val result =
            evaluationRequest(
                flagKey = "new_checkout",
                targetKey = "user-1",
            ) {
                string("country", "KR")
                number("age", 19.0)
            }
    ) {
        is Outcome.Err -> error("Invalid request: ${result.error}")
        is Outcome.Ok -> result.value
    }

val enabled =
    RuleEngine.evaluateBoolean(
        ruleSet = ruleSet,
        request = request,
    )
```

일반 `RuleValue` decision이 필요할 때는 `RuleEngine.evaluate`를 사용합니다.
수정 중인 draft rule을 다룰 때는 `decodeRuleSet`을 사용하고, 평가 가능한 snapshot이 필요할 때는 `decodeValidRuleSet`을 사용합니다.

## 개발 명령

```sh
./gradlew test
mise run coverage
mise run coverage:check
mise run lint
mise run docs:check
mise run docs:facts
```

## 문서

- `AGENTS.md`: 에이전트와 자동화가 따라야 하는 작업 규칙
- `docs/generated/project-facts.md`: 코드와 Gradle 설정에서 생성한 사실 기반 문서
- `docs/adr`: 아키텍처 결정 기록

사람이 읽는 문서는 배경과 의도를 설명하고, 에이전트용 문서는 규칙과 사실을 더 엄격하게 유지합니다.
