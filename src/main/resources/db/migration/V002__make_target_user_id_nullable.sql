-- ACTION 투표에서 target_user_id가 NULL을 허용하도록 수정
ALTER TABLE room_vote
ALTER COLUMN target_user_id DROP NOT NULL;

-- vote_type CHECK 제약 조건 수정: ACTION 추가
ALTER TABLE room_vote
DROP CONSTRAINT IF EXISTS room_vote_vote_type_check;

ALTER TABLE room_vote
ADD CONSTRAINT room_vote_vote_type_check
CHECK (vote_type IN ('KICK', 'ACTION'));

COMMENT ON COLUMN room_vote.target_user_id IS 'Target user for KICK votes (NULL for ACTION votes)';
