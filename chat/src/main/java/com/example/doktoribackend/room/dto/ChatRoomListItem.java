package com.example.doktoribackend.room.dto;

import com.example.doktoribackend.book.domain.Book;
import com.example.doktoribackend.room.domain.ChattingRoom;

public record ChatRoomListItem(
        Long roomId,
        String topic,
        String description,
        Integer capacity,
        Integer currentMemberCount,
        String bookTitle,
        String bookAuthors,
        String bookThumbnailUrl
) {

    public static ChatRoomListItem from(ChattingRoom room) {
        Book book = room.getBook();
        return new ChatRoomListItem(
                room.getId(),
                room.getTopic(),
                room.getDescription(),
                room.getCapacity(),
                room.getCurrentMemberCount(),
                book != null ? book.getTitle() : null,
                book != null ? book.getAuthors() : null,
                book != null ? book.getThumbnailUrl() : null
        );
    }
}
