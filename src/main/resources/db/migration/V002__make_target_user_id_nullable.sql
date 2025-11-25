-- ACTION 투표에서 target_user_id가 NULL을 허용하도록 수정
ALTER TABLE room_vote
ALTER COLUMN target_user_id DROP NOT NULL;

COMMENT ON COLUMN room_vote.target_user_id IS 'Target user for KICK votes (NULL for ACTION votes)';
