# 모임 제목 & 설명 생성

> DB: `meetings.title` VARCHAR(50) NOT NULL, `meetings.description` VARCHAR(300)
> 저장: `seed/content/meetings_novel.json`, `meetings_economy.json`, ...
> 총 목표: 8장르 × 15개 = 120개 (8회, 장르별 1회)

한 번에 장르 1개, 15개씩 생성.

## 규칙
- **title**: 50자 이내 (NOT NULL)
- **description**: 300자 이내
- 장르별로 요청할 예정

## 8개 장르
1. `NOVEL` (소설)
2. `ECONOMY_BUSINESS` (경제/경영)
3. `ESSAY` (에세이)
4. `HUMANITIES_PHIL` (인문/철학)
5. `SOCIETY_POLITICS` (사회/정치)
6. `SELF_DEVELOPMENT` (자기계발)
7. `SCIENCE_TECH` (과학/기술)
8. `HISTORY` (역사)

## 제목 스타일 — 다양하게 섞어

- 타겟 제시: "직장인을 위한 경제 북클럽"
- 감정 기반: "마음이 지칠 때 읽는 에세이"
- 구체적 주제: "기후변화 과학책 함께 읽기"
- 질문형: "요즘 뭐 읽어? 소설 추천 모임"
- 캐주얼: "퇴근 후 한 챕터씩 같이 읽어요"

## 설명 — 이런 내용 포함

- 어떤 책을 주로 읽는지 (구체적 분야/키워드)
- 모임 분위기 (편한 수다 vs 깊은 토론 vs 가벼운 감상 공유)
- 참여 대상 ("초보 환영", "매주 1권 읽을 수 있는 분", "해당 분야 관심 있는 분")

## JSON 형식
```json
[
  {
    "id": 1,
    "genre": "NOVEL",
    "title": "20대의 마음을 읽는 현대소설 모임",
    "description": "직장 생활 속 고민, 연애, 성장에 관한 소설들을 함께 읽습니다..."
  }
]
```

## 주의사항
- title 50자, description 300자 **초과 시 DB 에러** — 반드시 체크
- genre 값은 위 목록의 정확한 문자열 사용
- 특정 실존 책 제목 언급 OK (자연스러우면)

---

**NOVEL(소설) 장르 15개 생성해줘.**

이후 요청: "ECONOMY_BUSINESS 15개", "ESSAY 15개", ... 순차적으로.