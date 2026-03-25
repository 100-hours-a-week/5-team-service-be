#!/usr/bin/env python3
"""
부하테스트용 시드 데이터 SQL 생성 스크립트.

사용법: python3 seed/generate_seed_sql.py
결과물: seed/output/seed_data.sql

기존 seed/content/ JSON 파일을 읽어서 FK 정합성이 보장된 INSERT SQL을 생성한다.
"""

import json
import os
import random
from datetime import datetime, timedelta, date, time
from pathlib import Path
from typing import Union, List, Dict, Optional

# ─── 설정 ───────────────────────────────────────────────────────────────
SEED = 42
random.seed(SEED)

# 경로
BASE_DIR = Path(__file__).resolve().parent
CONTENT_DIR = BASE_DIR / "content"
OUTPUT_DIR = BASE_DIR / "output"

# 볼륨
NUM_BOOKS = 1000
NUM_MEETINGS = 5000
NUM_CHATTING_ROOMS = 2000
NUM_NOTIFICATIONS = 50000

# 유저 범위 (DevDataInitializer가 생성한 testuser_1 ~ testuser_500)
USER_ID_START = 1
USER_ID_END = 500
USER_IDS = list(range(USER_ID_START, USER_ID_END + 1))

# reading_genres (V1__init_schema.sql 기준)
READING_GENRES = {
    1: "NOVEL",
    2: "ECONOMY_BUSINESS",
    3: "ESSAY",
    4: "HUMANITIES_PHIL",
    5: "SOCIETY_POLITICS",
    6: "SELF_DEVELOPMENT",
    7: "SCIENCE_TECH",
    8: "HISTORY",
}

# genre code → content 파일 매핑
GENRE_TO_FILE = {
    "NOVEL": "novel",
    "ECONOMY_BUSINESS": "economy",
    "ESSAY": "essay",
    "HUMANITIES_PHIL": "humanities",
    "SOCIETY_POLITICS": "society",
    "SELF_DEVELOPMENT": "self_dev",
    "SCIENCE_TECH": "science",
    "HISTORY": "history",
}

# notification_types (V2 + V23 + V24 기준)
NOTIFICATION_TYPES = [
    {"id": 1, "code": "ROUND_START_10M_BEFORE", "title": "10분 후 토론이 시작돼요",
     "template": "{meetingTitle} 토론이 곧 시작됩니다", "link": "/users/me/meetings/{meetingId}"},
    {"id": 2, "code": "BOOK_REPORT_CHECKED", "title": "독후감 검사가 완료됐어요",
     "template": "{meetingTitle} 독후감 검사가 완료되었습니다", "link": "/users/me/meetings/{meetingId}"},
    {"id": 3, "code": "BOOK_REPORT_DEADLINE_24H_BEFORE", "title": "독후감 마감까지 하루 남았어요",
     "template": "{meetingTitle} 독후감 마감까지 하루 남았습니다", "link": "/users/me/meetings/{meetingId}"},
    {"id": 4, "code": "BOOK_REPORT_DEADLINE_30M_BEFORE", "title": "독후감 마감까지 30분 남았어요",
     "template": "{meetingTitle} 독후감 마감까지 30분 남았습니다", "link": "/users/me/meetings/{meetingId}"},
    {"id": 5, "code": "ROUND_COMPLETED_REVIEW_REQUEST", "title": "후기를 작성해주세요",
     "template": "라운드가 완료되었습니다. 후기를 작성해주세요", "link": "/my-meeting/{roundId}/review"},
    {"id": 6, "code": "BEST_MEMBER_SELECTED", "title": "베스트 모임원으로 선정되었어요",
     "template": "축하합니다! 베스트 모임원으로 선정되었습니다", "link": "/my-meeting/{meetingId}"},
    {"id": 7, "code": "BOOK_REPORT_POKE", "title": "독후감을 작성해주세요",
     "template": "독후감 작성 알림입니다", "link": "/my-meeting/{meetingId}"},
]

# 기준 시각 (NOW() 대신 고정값 사용, SQL에서 NOW() 상대값으로 변환)
NOW = datetime(2026, 3, 21, 12, 0, 0)
TODAY = NOW.date()

# ─── 한국 출판사 / 저자 풀 ─────────────────────────────────────────────
PUBLISHERS = [
    "창비", "민음사", "문학동네", "김영사", "위즈덤하우스", "다산북스", "을유문화사",
    "열린책들", "사계절", "한겨레출판", "문학과지성사", "예스24", "알에이치코리아",
    "쌤앤파커스", "더퀘스트", "한빛미디어", "중앙북스", "매일경제신문사", "21세기북스",
    "시공사", "웅진지식하우스", "비룡소", "황금부엉이", "북이십일", "수오서재",
]

AUTHORS_BY_GENRE = {
    "NOVEL": ["한강", "김영하", "최은영", "정세랑", "김초엽", "백수린", "손원평", "조남주",
              "황정은", "편혜영", "김숨", "박민규", "은희경", "공지영", "신경숙"],
    "ECONOMY_BUSINESS": ["홍춘욱", "김승호", "유시민", "장하성", "김난도", "이지성",
                          "박경철", "사경인", "존리", "피터 린치"],
    "ESSAY": ["김영하", "박완서", "김훈", "정유정", "이병률", "혜민", "김연수", "백영옥",
              "장류진", "이슬아"],
    "HUMANITIES_PHIL": ["마이클 샌델", "유발 하라리", "한병철", "김상근", "정재승", "채사장",
                         "주디스 버틀러", "움베르토 에코"],
    "SOCIETY_POLITICS": ["장하준", "노엄 촘스키", "조국", "김어준", "유시민", "진중권",
                          "강준만", "김누리"],
    "SELF_DEVELOPMENT": ["김미경", "이지성", "류이치 사카모토", "데일 카네기", "스티븐 코비",
                          "마크 맨슨", "제임스 클리어", "원씽"],
    "SCIENCE_TECH": ["리처드 도킨스", "칼 세이건", "닐 디그래스 타이슨", "정재승", "김상욱",
                      "이정모", "맷 리들리", "제레드 다이아몬드"],
    "HISTORY": ["유발 하라리", "재레드 다이아몬드", "설민석", "이덕일", "한명기", "주경철",
                 "박시백", "김훈"],
}

BOOK_TITLE_TEMPLATES = {
    "NOVEL": [
        "{}의 정원", "바람이 불 때", "어느 날 {}에서", "{}을 걷는 사람", "마지막 {}",
        "봄날의 {}에게", "{}의 시간", "우리가 {} 곁에", "{}으로 가는 길", "{}의 기억",
        "{}의 세계", "떠나는 {}", "{}에서 온 편지", "작은 {}의 이야기", "{}과 나",
    ],
    "ECONOMY_BUSINESS": [
        "{}의 경제학", "부의 {}법칙", "{}에서 배우는 투자", "돈의 {}원리", "{}시대의 경영",
        "{}전략의 기술", "글로벌 {}트렌드", "{}의 미래", "성공하는 {}의 비밀", "{}경제 읽기",
    ],
    "ESSAY": [
        "{}의 산책", "오늘도 {}했다", "{}에 대하여", "나의 {}일기", "{}의 온도",
        "{}이 가르쳐준 것", "{}을 위한 에세이", "{}의 순간들", "조용한 {}", "{}의 기록",
    ],
    "HUMANITIES_PHIL": [
        "{}이란 무엇인가", "{}에 대한 성찰", "{}의 철학", "인간과 {}의 조건", "{}을 묻다",
        "{}에 관한 사유", "{}의 발견", "존재와 {}의 사이", "{}의 한계", "{}을 넘어서",
    ],
    "SOCIETY_POLITICS": [
        "{}사회의 이면", "{}시대의 정치", "불평등과 {}의 구조", "{}의 권력", "시민과 {}",
        "{}을 위한 나라", "{}의 민주주의", "{}속의 진실", "변화하는 {}", "{}의 미래",
    ],
    "SELF_DEVELOPMENT": [
        "{}하는 습관", "하루 {}분의 기적", "{}의 힘", "성장하는 {}의 법칙", "{}을 이기는 법",
        "나를 바꾸는 {}", "{}루틴", "{}마인드셋", "{}하는 아침", "{}을 시작하는 용기",
    ],
    "SCIENCE_TECH": [
        "{}의 과학", "우주와 {}의 비밀", "{}이 바꾸는 세상", "{}의 물리학", "생명의 {}원리",
        "{}기술의 미래", "데이터와 {}의 시대", "{}을 읽는 눈", "{}의 발전사", "인공지능과 {}",
    ],
    "HISTORY": [
        "{}의 역사", "{}시대를 읽다", "{}이 만든 세계", "전쟁과 {}의 기록", "{}의 흥망",
        "{}에서 배우는 교훈", "{}왕조실록", "{}의 탄생", "잊혀진 {}의 이야기", "{}을 기억하다",
    ],
}

TITLE_FILLERS = [
    "사랑", "희망", "시간", "기억", "빛", "그림자", "바다", "산", "도시", "숲",
    "꿈", "눈물", "미소", "하늘", "별", "노래", "길", "다리", "집", "창문",
    "아침", "저녁", "가을", "봄", "여름", "겨울", "강", "달", "구름", "비",
]


# ─── 유틸리티 ──────────────────────────────────────────────────────────

def load_json(filename: str) -> Union[list, dict]:
    with open(CONTENT_DIR / filename, "r", encoding="utf-8") as f:
        return json.load(f)


def escape_sql(s: str) -> str:
    """SQL 문자열 이스케이프."""
    if s is None:
        return "NULL"
    return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")


def sql_str(s: Optional[str], max_len: int = 0) -> str:
    """SQL 문자열 리터럴 생성. max_len > 0이면 문자열을 잘라낸다 (escape 전)."""
    if s is None:
        return "NULL"
    if max_len > 0:
        s = s[:max_len]
    return f"'{escape_sql(s)}'"


def sql_datetime(dt: Optional[datetime]) -> str:
    if dt is None:
        return "NULL"
    return f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}'"


def sql_date(d: Optional[date]) -> str:
    if d is None:
        return "NULL"
    return f"'{d.strftime('%Y-%m-%d')}'"


def sql_time(t: Optional[time]) -> str:
    if t is None:
        return "NULL"
    return f"'{t.strftime('%H:%M:%S')}'"


def random_datetime_between(start: datetime, end: datetime) -> datetime:
    delta = end - start
    seconds = int(delta.total_seconds())
    if seconds <= 0:
        return start
    return start + timedelta(seconds=random.randint(0, seconds))


def random_date_between(start: date, end: date) -> date:
    delta = (end - start).days
    if delta <= 0:
        return start
    return start + timedelta(days=random.randint(0, delta))


# ─── 콘텐츠 로더 ──────────────────────────────────────────────────────

def load_all_content():
    """seed/content/ 에서 모든 JSON을 로드."""
    content = {}

    # meetings descriptions by genre
    content["meetings"] = {}
    for genre_code, file_key in GENRE_TO_FILE.items():
        content["meetings"][genre_code] = load_json(f"meetings_{file_key}.json")

    # book reports by genre
    content["book_reports"] = {}
    for genre_code, file_key in GENRE_TO_FILE.items():
        content["book_reports"][genre_code] = load_json(f"book_reports_{file_key}.json")

    # topics by genre
    content["topics"] = {}
    for genre_code, file_key in GENRE_TO_FILE.items():
        content["topics"][genre_code] = load_json(f"topics_{file_key}.json")

    # nicknames
    content["nicknames"] = load_json("nicknames.json")

    # user intros
    content["user_intros"] = load_json("user_intros.json")

    # messages
    content["messages"] = {}
    for cat in ["casual", "discussion", "farewell", "greeting", "impression", "opinion"]:
        content["messages"][cat] = load_json(f"messages_{cat}.json")

    return content


# ─── 데이터 생성 ───────────────────────────────────────────────────────

def generate_books() -> List[dict]:
    """books 1000건 생성."""
    books = []
    used_isbns = set()

    for i in range(1, NUM_BOOKS + 1):
        genre_id = ((i - 1) % 8) + 1
        genre_code = READING_GENRES[genre_id]
        authors_pool = AUTHORS_BY_GENRE[genre_code]
        templates = BOOK_TITLE_TEMPLATES[genre_code]

        # ISBN 생성 (978 + 10자리)
        while True:
            isbn = f"978{random.randint(1000000000, 9999999999)}"
            if isbn not in used_isbns:
                used_isbns.add(isbn)
                break

        filler = random.choice(TITLE_FILLERS)
        template = random.choice(templates)
        title = template.format(filler)
        # 중복 제목 방지: 인덱스 추가
        if i > len(templates) * len(TITLE_FILLERS):
            title = f"{title} {i}"

        author = random.choice(authors_pool)
        publisher = random.choice(PUBLISHERS)
        published_at = random_date_between(date(2010, 1, 1), date(2025, 12, 31))

        books.append({
            "id": i,
            "isbn": isbn,
            "title": title,
            "authors": author,
            "publisher": publisher,
            "thumbnail_url": f"https://via.placeholder.com/120x174.png?text=book{i}",
            "published_at": published_at,
            "summary": None,
            "created_at": NOW - timedelta(days=random.randint(30, 365)),
            "updated_at": NOW - timedelta(days=random.randint(1, 30)),
        })

    return books


def generate_meetings(content: dict) -> tuple[List[dict], List[dict], List[dict]]:
    """
    meetings, meeting_rounds, meeting_members 생성.
    반환: (meetings, rounds, members)
    """
    meetings = []
    all_rounds = []
    all_members = []

    round_id = 0
    member_id = 0

    # 상태 분포
    statuses = (
        ["RECRUITING"] * 2000
        + ["FINISHED"] * 2500
        + ["CANCELED"] * 500
    )
    random.shuffle(statuses)

    days_of_week = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
    start_times = [time(19, 0), time(20, 0), time(21, 0)]
    duration_options = [60, 90, 120]

    for i in range(1, NUM_MEETINGS + 1):
        status = statuses[i - 1]
        genre_id = ((i - 1) % 8) + 1
        genre_code = READING_GENRES[genre_id]

        # meeting content
        meeting_descs = content["meetings"].get(genre_code, [])
        desc_item = meeting_descs[(i - 1) % len(meeting_descs)] if meeting_descs else {}

        capacity = random.randint(3, 8)
        round_count = random.randint(1, 4)
        day_of_week = random.choice(days_of_week)
        start_t = random.choice(start_times)
        duration = random.choice(duration_options)

        leader_user_id = random.choice(USER_IDS)

        # leader intro
        leader_intros = content["user_intros"].get("leaderIntros", [])
        if isinstance(content["user_intros"], dict):
            leader_intros = content["user_intros"].get("leaderIntros", [])
        else:
            leader_intros = content["user_intros"]
        leader_intro = leader_intros[(i - 1) % len(leader_intros)].get("content", "") if leader_intros else ""

        title = desc_item.get("title", f"독서 모임 #{i}")
        description = desc_item.get("description", f"테스트용 독서 모임입니다. #{i}")

        # 시간 설정
        if status == "RECRUITING":
            created_at = random_datetime_between(NOW - timedelta(days=14), NOW - timedelta(days=1))
            recruitment_deadline = random_date_between(TODAY + timedelta(days=7), TODAY + timedelta(days=30))
            first_round_at = datetime.combine(
                recruitment_deadline + timedelta(days=random.randint(1, 7)),
                start_t
            )
            current_round = 1
        elif status == "FINISHED":
            created_at = random_datetime_between(NOW - timedelta(days=180), NOW - timedelta(days=60))
            recruitment_deadline = random_date_between(
                TODAY - timedelta(days=90), TODAY - timedelta(days=30)
            )
            first_round_at = datetime.combine(
                recruitment_deadline + timedelta(days=random.randint(1, 7)),
                start_t
            )
            current_round = round_count
        else:  # CANCELED
            created_at = random_datetime_between(NOW - timedelta(days=120), NOW - timedelta(days=30))
            recruitment_deadline = random_date_between(
                TODAY - timedelta(days=60), TODAY - timedelta(days=10)
            )
            first_round_at = datetime.combine(
                recruitment_deadline + timedelta(days=random.randint(1, 7)),
                start_t
            )
            current_round = 1

        # ─ members 먼저 생성 (currentCount 계산 필요) ─
        if status == "CANCELED":
            member_count = random.randint(1, min(3, capacity))
        else:
            member_count = random.randint(3, capacity)

        # 유저 선택: leader + members (중복 방지)
        available_users = [u for u in USER_IDS if u != leader_user_id]
        other_members = random.sample(available_users, min(member_count - 1, len(available_users)))
        meeting_user_ids = [leader_user_id] + other_members
        actual_count = len(meeting_user_ids)

        meeting = {
            "id": i,
            "leader_user_id": leader_user_id,
            "reading_genre_id": genre_id,
            "leader_intro": leader_intro[:300] if leader_intro else None,
            "meeting_image_path": "images/meetings/default.png",
            "title": title[:50],
            "description": description[:300],
            "capacity": capacity,
            "current_count": actual_count,
            "round_count": round_count,
            "current_round": current_round,
            "status": status,
            "day_of_week": day_of_week,
            "start_time": start_t,
            "duration_minutes": duration,
            "first_round_at": first_round_at,
            "recruitment_deadline": recruitment_deadline,
            "created_at": created_at,
            "updated_at": created_at + timedelta(hours=random.randint(1, 48)),
            "deleted_at": None,
        }
        meetings.append(meeting)

        # ─ members ─
        member_intros_pool = content["user_intros"]
        if isinstance(member_intros_pool, dict):
            mi_list = member_intros_pool.get("memberIntros", member_intros_pool.get("leaderIntros", []))
        else:
            mi_list = member_intros_pool

        for idx, uid in enumerate(meeting_user_ids):
            member_id += 1
            role = "LEADER" if uid == leader_user_id else "MEMBER"
            mi = mi_list[(member_id - 1) % len(mi_list)] if mi_list else {}
            intro_text = mi.get("content", "안녕하세요, 잘 부탁드립니다.")

            all_members.append({
                "id": member_id,
                "meeting_id": i,
                "user_id": uid,
                "role": role,
                "status": "APPROVED",
                "member_intro": intro_text[:300],
                "approved_at": created_at + timedelta(hours=random.randint(1, 72)),
                "rejected_at": None,
                "left_at": None,
                "created_at": created_at + timedelta(minutes=random.randint(0, 60)),
                "updated_at": created_at + timedelta(hours=random.randint(1, 72)),
            })

        # ─ rounds ─
        for r in range(1, round_count + 1):
            round_id += 1
            round_start = first_round_at + timedelta(days=7 * (r - 1))
            round_end = round_start + timedelta(minutes=duration)
            book_id = random.randint(1, NUM_BOOKS)

            if status == "FINISHED":
                round_status = "DONE"
            elif status == "CANCELED":
                round_status = "CANCELED"
            else:  # RECRUITING
                round_status = "SCHEDULED"

            all_rounds.append({
                "id": round_id,
                "meeting_id": i,
                "book_id": book_id,
                "round_no": r,
                "status": round_status,
                "meeting_link": None,
                "start_at": round_start,
                "end_at": round_end,
                "best_member_determined": round_status == "DONE",
                "created_at": created_at,
                "updated_at": round_start if round_status == "DONE" else created_at,
            })

    return meetings, all_rounds, all_members


def generate_book_reports(
    content: dict,
    meetings: List[dict],
    rounds: List[dict],
    members: List[dict],
) -> List[dict]:
    """DONE 라운드의 참여자에 대해 독후감 생성."""
    reports = []
    report_id = 0

    # meeting_id → member user_ids
    meeting_members_map: Dict[int, List[int]] = {}
    for m in members:
        mid = m["meeting_id"]
        if mid not in meeting_members_map:
            meeting_members_map[mid] = []
        meeting_members_map[mid].append(m["user_id"])

    # meeting_id → genre
    meeting_genre_map = {m["id"]: READING_GENRES[m["reading_genre_id"]] for m in meetings}

    # 모든 장르 독후감 콘텐츠 통합
    all_report_contents = []
    for genre_code in GENRE_TO_FILE:
        genre_reports = content["book_reports"].get(genre_code, [])
        for r in genre_reports:
            all_report_contents.append(r.get("content", "좋은 책이었습니다."))

    for rnd in rounds:
        if rnd["status"] != "DONE":
            continue

        mid = rnd["meeting_id"]
        user_ids = meeting_members_map.get(mid, [])
        genre = meeting_genre_map.get(mid, "NOVEL")

        # 장르별 콘텐츠 우선, 없으면 전체에서
        genre_contents = [r.get("content", "") for r in content["book_reports"].get(genre, [])]
        if not genre_contents:
            genre_contents = all_report_contents

        for uid in user_ids:
            # 80% 확률로 독후감 작성 (현실적 비율)
            if random.random() > 0.85:
                continue

            report_id += 1
            report_content = genre_contents[(report_id - 1) % len(genre_contents)]

            is_approved = random.random() < 0.8
            status = "APPROVED" if is_approved else "PENDING_REVIEW"
            report_created = rnd["end_at"] + timedelta(hours=random.randint(1, 48))
            ai_validated = report_created + timedelta(minutes=random.randint(5, 30)) if is_approved else None

            reports.append({
                "id": report_id,
                "user_id": uid,
                "meeting_round_id": rnd["id"],
                "content": report_content,
                "status": status,
                "rejection_reason": None,
                "ai_validated_at": ai_validated,
                "deleted_at": None,
                "created_at": report_created,
                "updated_at": ai_validated if ai_validated else report_created,
            })

    return reports


def generate_chatting_rooms(content: dict) -> tuple[List[dict], List[dict]]:
    """chatting_rooms + chatting_room_members 생성."""
    rooms = []
    all_room_members = []
    room_member_id = 0

    # 상태 분포
    statuses = ["WAITING"] * 1000 + ["ENDED"] * 1000
    random.shuffle(statuses)

    nicknames_pool = [n["nickname"] for n in content["nicknames"]]

    # 장르별 topics
    all_topics = []
    for genre_code in GENRE_TO_FILE:
        genre_topics = content["topics"].get(genre_code, [])
        for t in genre_topics:
            all_topics.append(t.get("topic", "자유 토론"))

    for i in range(1, NUM_CHATTING_ROOMS + 1):
        status = statuses[i - 1]
        capacity = random.randint(2, 6)
        book_id = random.randint(1, NUM_BOOKS)
        topic = all_topics[(i - 1) % len(all_topics)]

        if status == "WAITING":
            created_at = random_datetime_between(NOW - timedelta(days=7), NOW - timedelta(hours=1))
            member_count = random.randint(1, capacity)
        else:  # ENDED
            created_at = random_datetime_between(NOW - timedelta(days=30), NOW - timedelta(days=3))
            member_count = random.randint(2, capacity)

        # owner + participants
        room_users = random.sample(USER_IDS, member_count)
        owner_uid = room_users[0]

        rooms.append({
            "id": i,
            "topic": topic[:50],
            "description": f"이 책에 대해 자유롭게 이야기해요"[:50],
            "capacity": capacity,
            "current_member_count": member_count,
            "book_id": book_id,
            "duration": 30,
            "status": status,
            "created_at": created_at,
        })

        for idx, uid in enumerate(room_users):
            room_member_id += 1
            role = "HOST" if uid == owner_uid else "PARTICIPANT"
            position = "AGREE" if random.random() < 0.5 else "DISAGREE"
            nickname = nicknames_pool[(uid - 1) % len(nicknames_pool)]

            if status == "WAITING":
                member_status = "WAITING"
            else:
                member_status = "DISCONNECTED"

            all_room_members.append({
                "id": room_member_id,
                "chatting_room_id": i,
                "user_id": uid,
                "nickname": nickname[:20],
                "profile_image_url": "images/profiles/273ddbd8-028c-4521-87b3-338f2e7de116.jpg",
                "status": member_status,
                "role": role,
                "position": position,
                "created_at": created_at + timedelta(seconds=random.randint(0, 300)),
                "updated_at": created_at + timedelta(seconds=random.randint(300, 600)),
            })

    return rooms, all_room_members


def generate_notifications(meetings: List[dict]) -> List[dict]:
    """notifications 50,000건."""
    notifications = []

    meeting_titles = {m["id"]: m["title"] for m in meetings}

    for i in range(1, NUM_NOTIFICATIONS + 1):
        uid = random.choice(USER_IDS)
        ntype = random.choice(NOTIFICATION_TYPES)
        mid = random.randint(1, NUM_MEETINGS)
        mtitle = meeting_titles.get(mid, "독서 모임")

        title = ntype["title"]
        message = ntype["template"].format(
            meetingTitle=mtitle, meetingId=mid, roundId=random.randint(1, 15000)
        )
        link = ntype["link"].format(meetingId=mid, roundId=random.randint(1, 15000))

        is_read = random.random() < 0.7
        created_at = random_datetime_between(NOW - timedelta(days=3), NOW)
        read_at = created_at + timedelta(minutes=random.randint(5, 1440)) if is_read else None

        notifications.append({
            "id": i,
            "user_id": uid,
            "notification_type_id": ntype["id"],
            "title": title[:80],
            "message": message[:300],
            "link_path": link,
            "is_read": is_read,
            "read_at": read_at,
            "created_at": created_at,
            "deleted_at": None,
        })

    return notifications


# ─── SQL 출력 ──────────────────────────────────────────────────────────

def write_sql(
    books, meetings, rounds, members, reports, rooms, room_members, notifications
):
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    filepath = OUTPUT_DIR / "seed_data.sql"

    with open(filepath, "w", encoding="utf-8") as f:
        f.write("-- ========================================\n")
        f.write("-- 부하테스트용 시드 데이터\n")
        f.write(f"-- 생성일: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"-- 기준시각: {NOW.strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("-- ========================================\n\n")
        f.write("SET NAMES utf8mb4;\n")
        f.write("SET CHARACTER SET utf8mb4;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n")
        f.write("SET UNIQUE_CHECKS = 0;\n")
        f.write("SET autocommit = 0;\n\n")

        # ─ books ─
        f.write(f"-- books: {len(books)}건\n")
        batch_size = 100
        for batch_start in range(0, len(books), batch_size):
            batch = books[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO books (id, isbn13, title, authors, publisher, thumbnail_url, published_at, summary, deleted_at, created_at, updated_at) VALUES\n"
            )
            rows = []
            for b in batch:
                rows.append(
                    f"  ({b['id']}, {sql_str(b['isbn'], 13)}, {sql_str(b['title'], 255)}, "
                    f"{sql_str(b['authors'], 255)}, {sql_str(b['publisher'], 255)}, "
                    f"{sql_str(b['thumbnail_url'], 512)}, {sql_date(b['published_at'])}, "
                    f"{sql_str(b['summary'], 2000)}, NULL, "
                    f"{sql_datetime(b['created_at'])}, {sql_datetime(b['updated_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ meetings ─
        f.write(f"-- meetings: {len(meetings)}건\n")
        for batch_start in range(0, len(meetings), batch_size):
            batch = meetings[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO meetings (id, leader_user_id, reading_genre_id, leader_intro, "
                "meeting_image_path, title, description, capacity, current_count, round_count, "
                "current_round, status, day_of_week, start_time, duration_minutes, "
                "first_round_at, recruitment_deadline, deleted_at, created_at, updated_at) VALUES\n"
            )
            rows = []
            for m in batch:
                rows.append(
                    f"  ({m['id']}, {m['leader_user_id']}, {m['reading_genre_id']}, "
                    f"{sql_str(m['leader_intro'], 300)}, {sql_str(m['meeting_image_path'], 512)}, "
                    f"{sql_str(m['title'], 50)}, {sql_str(m['description'], 300)}, "
                    f"{m['capacity']}, {m['current_count']}, {m['round_count']}, "
                    f"{m['current_round']}, {sql_str(m['status'], 20)}, {sql_str(m['day_of_week'], 3)}, "
                    f"{sql_time(m['start_time'])}, {m['duration_minutes']}, "
                    f"{sql_datetime(m['first_round_at'])}, {sql_date(m['recruitment_deadline'])}, "
                    f"NULL, {sql_datetime(m['created_at'])}, {sql_datetime(m['updated_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ meeting_rounds ─
        f.write(f"-- meeting_rounds: {len(rounds)}건\n")
        for batch_start in range(0, len(rounds), batch_size):
            batch = rounds[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO meeting_rounds (id, meeting_id, book_id, round_no, status, "
                "meeting_link, start_at, end_at, best_member_determined, created_at, updated_at) VALUES\n"
            )
            rows = []
            for r in batch:
                rows.append(
                    f"  ({r['id']}, {r['meeting_id']}, {r['book_id']}, {r['round_no']}, "
                    f"{sql_str(r['status'], 20)}, {sql_str(r['meeting_link'], 1024)}, "
                    f"{sql_datetime(r['start_at'])}, {sql_datetime(r['end_at'])}, "
                    f"{1 if r['best_member_determined'] else 0}, "
                    f"{sql_datetime(r['created_at'])}, {sql_datetime(r['updated_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ meeting_members ─
        f.write(f"-- meeting_members: {len(members)}건\n")
        for batch_start in range(0, len(members), batch_size):
            batch = members[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO meeting_members (id, meeting_id, user_id, role, status, "
                "member_intro, approved_at, rejected_at, left_at, created_at, updated_at) VALUES\n"
            )
            rows = []
            for m in batch:
                rows.append(
                    f"  ({m['id']}, {m['meeting_id']}, {m['user_id']}, "
                    f"{sql_str(m['role'], 20)}, {sql_str(m['status'], 20)}, "
                    f"{sql_str(m['member_intro'], 300)}, {sql_datetime(m['approved_at'])}, "
                    f"{sql_datetime(m['rejected_at'])}, {sql_datetime(m['left_at'])}, "
                    f"{sql_datetime(m['created_at'])}, {sql_datetime(m['updated_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ book_reports ─
        f.write(f"-- book_reports: {len(reports)}건\n")
        for batch_start in range(0, len(reports), batch_size):
            batch = reports[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO book_reports (id, user_id, meeting_round_id, content, status, "
                "rejection_reason, ai_validated_at, deleted_at, created_at, updated_at) VALUES\n"
            )
            rows = []
            for r in batch:
                rows.append(
                    f"  ({r['id']}, {r['user_id']}, {r['meeting_round_id']}, "
                    f"{sql_str(r['content'])}, {sql_str(r['status'], 20)}, "
                    f"{sql_str(r['rejection_reason'], 200)}, {sql_datetime(r['ai_validated_at'])}, "
                    f"{sql_datetime(r['deleted_at'])}, "
                    f"{sql_datetime(r['created_at'])}, {sql_datetime(r['updated_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ chatting_rooms ─
        f.write(f"-- chatting_rooms: {len(rooms)}건\n")
        for batch_start in range(0, len(rooms), batch_size):
            batch = rooms[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO chatting_rooms (id, topic, description, capacity, "
                "current_member_count, book_id, duration, status, created_at) VALUES\n"
            )
            rows = []
            for r in batch:
                rows.append(
                    f"  ({r['id']}, {sql_str(r['topic'], 50)}, {sql_str(r['description'], 50)}, "
                    f"{r['capacity']}, {r['current_member_count']}, {r['book_id']}, "
                    f"{r['duration']}, {sql_str(r['status'], 20)}, {sql_datetime(r['created_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ chatting_room_members ─
        f.write(f"-- chatting_room_members: {len(room_members)}건\n")
        for batch_start in range(0, len(room_members), batch_size):
            batch = room_members[batch_start:batch_start + batch_size]
            f.write(
                "INSERT INTO chatting_room_members (id, room_id, user_id, nickname, "
                "profile_image_url, status, role, position, created_at, updated_at) VALUES\n"
            )
            rows = []
            for m in batch:
                rows.append(
                    f"  ({m['id']}, {m['chatting_room_id']}, {m['user_id']}, "
                    f"{sql_str(m['nickname'], 20)}, {sql_str(m['profile_image_url'], 512)}, "
                    f"{sql_str(m['status'], 20)}, {sql_str(m['role'], 20)}, {sql_str(m['position'], 20)}, "
                    f"{sql_datetime(m['created_at'])}, {sql_datetime(m['updated_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ notifications ─
        f.write(f"-- notifications: {len(notifications)}건\n")
        batch_size_notif = 500  # 대량이므로 배치 크게
        for batch_start in range(0, len(notifications), batch_size_notif):
            batch = notifications[batch_start:batch_start + batch_size_notif]
            f.write(
                "INSERT INTO notifications (id, user_id, type_id, title, message, "
                "link_path, is_read, read_at, created_at, deleted_at) VALUES\n"
            )
            rows = []
            for n in batch:
                rows.append(
                    f"  ({n['id']}, {n['user_id']}, {n['notification_type_id']}, "
                    f"{sql_str(n['title'], 80)}, {sql_str(n['message'], 300)}, "
                    f"{sql_str(n['link_path'], 255)}, {1 if n['is_read'] else 0}, "
                    f"{sql_datetime(n['read_at'])}, {sql_datetime(n['created_at'])}, "
                    f"{sql_datetime(n['deleted_at'])})"
                )
            f.write(",\n".join(rows) + ";\n\n")

        # ─ auto_increment 보정 ─
        f.write("-- auto_increment 보정\n")
        f.write(f"ALTER TABLE books AUTO_INCREMENT = {len(books) + 1};\n")
        f.write(f"ALTER TABLE meetings AUTO_INCREMENT = {len(meetings) + 1};\n")
        f.write(f"ALTER TABLE meeting_rounds AUTO_INCREMENT = {len(rounds) + 1};\n")
        f.write(f"ALTER TABLE meeting_members AUTO_INCREMENT = {len(members) + 1};\n")
        f.write(f"ALTER TABLE book_reports AUTO_INCREMENT = {len(reports) + 1};\n")
        f.write(f"ALTER TABLE chatting_rooms AUTO_INCREMENT = {len(rooms) + 1};\n")
        f.write(f"ALTER TABLE chatting_room_members AUTO_INCREMENT = {len(room_members) + 1};\n")
        f.write(f"ALTER TABLE notifications AUTO_INCREMENT = {len(notifications) + 1};\n\n")

        f.write("COMMIT;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 1;\n")
        f.write("SET UNIQUE_CHECKS = 1;\n")

    return filepath


# ─── 메인 ──────────────────────────────────────────────────────────────

def main():
    print("📚 시드 데이터 생성 시작...")

    print("  콘텐츠 로드 중...")
    content = load_all_content()

    print(f"  books {NUM_BOOKS}건 생성 중...")
    books = generate_books()

    print(f"  meetings {NUM_MEETINGS}건 + rounds + members 생성 중...")
    meetings, rounds, members = generate_meetings(content)

    print(f"  book_reports 생성 중 (DONE 라운드 기준)...")
    reports = generate_book_reports(content, meetings, rounds, members)

    print(f"  chatting_rooms {NUM_CHATTING_ROOMS}건 + members 생성 중...")
    rooms, room_members = generate_chatting_rooms(content)

    print(f"  notifications {NUM_NOTIFICATIONS}건 생성 중...")
    notifications = generate_notifications(meetings)

    print("  SQL 파일 작성 중...")
    filepath = write_sql(books, meetings, rounds, members, reports, rooms, room_members, notifications)

    # 통계
    print("\n✅ 생성 완료!")
    print(f"  📄 {filepath}")
    print(f"  ├─ books:                  {len(books):,}")
    print(f"  ├─ meetings:               {len(meetings):,}")
    print(f"  ├─ meeting_rounds:         {len(rounds):,}")
    print(f"  ├─ meeting_members:        {len(members):,}")
    print(f"  ├─ book_reports:           {len(reports):,}")
    print(f"  ├─ chatting_rooms:         {len(rooms):,}")
    print(f"  ├─ chatting_room_members:  {len(room_members):,}")
    print(f"  └─ notifications:          {len(notifications):,}")
    print(f"  총 레코드:                  {len(books) + len(meetings) + len(rounds) + len(members) + len(reports) + len(rooms) + len(room_members) + len(notifications):,}")


if __name__ == "__main__":
    main()