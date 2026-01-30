package com.example.demo.chatmemory;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;

public class CustomChatMemoryRepositoryDialect implements JdbcChatMemoryRepositoryDialect {
  @Override
  public String getSelectMessagesSql() {
    return """
        SELECT content, type FROM TABLE_NAME 
        WHERE conversation_id = ? ORDER BY \"timestamp\"
    """;
  }
  
  @Override
  public String getInsertMessageSql() {
    return """
        INSERT INTO TABLE_NAME 
        (conversation_id, content, type, \"timestamp\") 
        VALUES (?, ?, ?, ?)
    """;
  }

  @Override
  public String getSelectConversationIdsSql() {
    return """
        SELECT DISTINCT conversation_id FROM TABLE_NAME
    """;
  }

  @Override
  public String getDeleteMessagesSql() {
    return """
        DELETE FROM TABLE_NAME WHERE conversation_id = ?
    """;
  }
}

