ALTER TABLE profiles
ADD COLUMN address VARCHAR(255);

COMMENT ON COLUMN profiles.address IS 'The primary physical or shipping address of the user';