package com.example.doktoribackend.common.client.book;

/**
 * 도서 검색 공급자가 HTTP 상태로는 성공을 알리면서 본문으로 실패를 알린 경우.
 *
 * <p>알라딘은 잘못된 TTB 키나 일일 호출 한도 초과 시 HTTP 200 에 errorCode 를 담아 응답한다.
 * 이를 정상 응답으로 취급하면 서킷 브레이커가 열리지 않고 사용자에게는 빈 결과만 나가므로
 * 별도 예외로 승격해 실패로 집계한다.
 */
public class BookSearchProviderException extends RuntimeException {

    public BookSearchProviderException(String message) {
        super(message);
    }

    public BookSearchProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
