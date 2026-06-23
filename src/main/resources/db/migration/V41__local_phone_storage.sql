UPDATE users
SET phone = substring(phone from 4)
WHERE phone ~ '^\+57[0-9]{10}$';

UPDATE technician_profiles
SET phone = substring(phone from 4)
WHERE phone ~ '^\+57[0-9]{10}$';
