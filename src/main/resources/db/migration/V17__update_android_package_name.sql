UPDATE app_versions
SET update_url = 'https://play.google.com/store/apps/details?id=com.tecngo',
    updated_at = NOW()
WHERE platform = 'ANDROID';

UPDATE system_parameters
SET parameter_value = 'com.tecngo',
    updated_at = NOW()
WHERE parameter_key = 'ANDROID_PACKAGE_NAME';
