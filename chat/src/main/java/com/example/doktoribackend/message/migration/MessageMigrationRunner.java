package com.example.doktoribackend.message.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class MessageMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final MongoOperations mongoOperations;

    @Override
    public void run(ApplicationArguments args) {
        long mongoCount = mongoOperations.getCollection("messages").countDocuments();
        if (mongoCount > 0) {
            log.info("MongoDB messages already exist ({}), skipping migration.", mongoCount);
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT room_id, round_id, sender_id, client_message_id, " +
                "message_type, text_message, file_path, created_at FROM messages ORDER BY id"
        );

        if (rows.isEmpty()) {
            log.info("No messages in MySQL, skipping migration.");
            return;
        }

        log.info("Migrating {} messages from MySQL to MongoDB...", rows.size());

        List<Document> docs = rows.stream()
                .map(this::toDocument)
                .toList();

        mongoOperations.getCollection("messages").insertMany(docs);

        log.info("Migration complete. {} messages inserted into MongoDB.", docs.size());
    }

    private Document toDocument(Map<String, Object> row) {
        Document doc = new Document();
        doc.put("roomId", toLong(row.get("room_id")));
        doc.put("roundId", toLong(row.get("round_id")));
        doc.put("senderId", toLong(row.get("sender_id")));
        doc.put("clientMessageId", row.get("client_message_id"));
        doc.put("messageType", row.get("message_type"));
        doc.put("textMessage", row.get("text_message"));
        doc.put("filePath", row.get("file_path"));
        doc.put("createdAt", row.get("created_at"));
        return doc;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        return ((Number) value).longValue();
    }
}
