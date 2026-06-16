UPDATE users
SET profile_photo_face_validated = TRUE,
    profile_photo_verified_at = COALESCE(profile_photo_verified_at, verified_at, NOW()),
    profile_photo_verified_by_user_id = COALESCE(profile_photo_verified_by_user_id, verified_by_user_id)
WHERE verification_status = 'VERIFIED'
  AND profile_photo_url IS NOT NULL
  AND profile_photo_url <> '';

UPDATE content_assets asset
SET moderation_status = 'APPROVED',
    moderation_reason = COALESCE(asset.moderation_reason, 'Profile photo approved because user is verified'),
    moderated_at = COALESCE(asset.moderated_at, users.profile_photo_verified_at, users.verified_at, NOW()),
    moderated_by_user_id = COALESCE(asset.moderated_by_user_id, users.profile_photo_verified_by_user_id, users.verified_by_user_id)
FROM users
WHERE asset.file_url = users.profile_photo_url
  AND asset.uploaded_by_user_id = users.id
  AND asset.kind = 'PROFILE'
  AND users.verification_status = 'VERIFIED'
  AND users.profile_photo_url IS NOT NULL
  AND users.profile_photo_url <> ''
  AND asset.moderation_status <> 'REJECTED';
