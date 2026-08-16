CREATE INDEX idx_bonus_operation_card_created_id
ON bonus_operation (card_id, created_at DESC, id DESC);