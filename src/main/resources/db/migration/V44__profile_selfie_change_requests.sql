CREATE TABLE IF NOT EXISTS profile_selfie_change_requests (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    current_photo_url varchar(500),
    requested_photo_url varchar(500) NOT NULL,
    face_detection_status varchar(40),
    status varchar(30) NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    reviewed_by_user_id uuid REFERENCES users(id),
    reviewed_at timestamptz,
    rejection_reason varchar(1000),
    CONSTRAINT profile_selfie_change_requests_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT profile_selfie_change_requests_face_status_check
        CHECK (face_detection_status IS NULL OR face_detection_status IN ('AUTO_VALIDATED', 'MANUAL_REVIEW_REQUIRED', 'FAILED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_profile_selfie_change_requests_pending_user
    ON profile_selfie_change_requests(user_id)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS ix_profile_selfie_change_requests_status_requested
    ON profile_selfie_change_requests(status, requested_at);
