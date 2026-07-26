package com.example.doktoribackend.common.client.book;

/**
 * 도서 검색 결과가 어느 공급자에서 왔는지 나타낸다.
 * NONE 은 모든 공급자가 실패해 빈 결과로 degrade 된 상태를 의미한다.
 */
public enum BookSearchSource {
    KAKAO,
    ALADIN,
    NONE
}
