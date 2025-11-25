ALTER TABLE chat_message
ADD COLUMN phase INT NOT NULL DEFAULT 0;

CREATE INDEX idx_message_stack
ON chat_message(room_id, message_type, phase, created_at);

ALTER TABLE multiplayer_room
ADD COLUMN is_llm_processing BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN story_outline TEXT;

COMMENT ON COLUMN chat_message.phase IS 'Phase number for conversation stack management';
COMMENT ON COLUMN multiplayer_room.is_llm_processing IS 'Flag indicating LLM request in progress';
COMMENT ON COLUMN multiplayer_room.story_outline IS 'Story outline for context maintenance';
