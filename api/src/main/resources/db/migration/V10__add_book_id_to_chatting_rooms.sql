ALTER TABLE chatting_rooms
    ADD COLUMN book_id BIGINT NULL AFTER capacity;

ALTER TABLE chatting_rooms
    ADD CONSTRAINT fk_chatting_rooms_book FOREIGN KEY (book_id) REFERENCES books (id);
