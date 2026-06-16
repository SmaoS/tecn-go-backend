ALTER TABLE users
    ADD COLUMN IF NOT EXISTS face_detection_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS identity_document_capture_status VARCHAR(40);

UPDATE users
SET face_detection_status = CASE
    WHEN profile_photo_url IS NOT NULL AND profile_photo_face_validated = TRUE THEN 'AUTO_VALIDATED'
    WHEN profile_photo_url IS NOT NULL THEN 'MANUAL_REVIEW_REQUIRED'
    ELSE face_detection_status
END
WHERE face_detection_status IS NULL;

UPDATE users
SET identity_document_capture_status = 'MANUAL_REVIEW_REQUIRED'
WHERE identity_document_capture_status IS NULL
  AND (document_front_url IS NOT NULL OR document_single_url IS NOT NULL OR document_photo_url IS NOT NULL);
