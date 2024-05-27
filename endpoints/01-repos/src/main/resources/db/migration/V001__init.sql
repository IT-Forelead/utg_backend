CREATE TYPE PRIVILEGE AS ENUM (
  'create_user',
  'update_user',
  'update_any_user',
  'view_users'
);

CREATE TABLE IF NOT EXISTS assets(
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  s3_key VARCHAR NOT NULL,
  filename VARCHAR NULL,
  media_type VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS privileges (
  name VARCHAR NOT NULL PRIMARY KEY,
  group_name VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
  id UUID NOT NULL PRIMARY KEY,
  name VARCHAR NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS role_privileges (
  role_id UUID NOT NULL CONSTRAINT fk_role_id REFERENCES roles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  privilege VARCHAR NOT NULL CONSTRAINT fk_role_privilege REFERENCES privileges (name) ON UPDATE CASCADE ON DELETE CASCADE,
  UNIQUE (role_id, privilege)
);


CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  firstname VARCHAR NOT NULL,
  lastname VARCHAR NOT NULL,
  login VARCHAR NOT NULL UNIQUE,
  role_id UUID NOT NULL CONSTRAINT fk_user_role REFERENCES roles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  asset_id UUID NULL CONSTRAINT fk_user_asset REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  password VARCHAR NOT NULL
);

INSERT INTO
  roles (id, name)
VALUES
  (
    '280ab51b-efc8-4e69-9f6d-ef500e1e5d42',
    'admin'
  ),
  (
    'b3d83cf5-aab4-4cb1-a1ce-f5754d96ec2c',
    'user'
  );

INSERT INTO
  "users" (
    "id",
    "created_at",
    "firstname",
    "lastname",
    "login",
    "role_id",
    "password"
  )
VALUES
  (
    '72a911c8-ad24-4e2d-8930-9c3ba51741df',
    '2022-11-07T06:43:01.089Z',
    'Admin',
    'Super Manager',
    'admin',
    '280ab51b-efc8-4e69-9f6d-ef500e1e5d42',
    '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY='
  );

  -- Create the trigger function
CREATE OR REPLACE FUNCTION fill_admin_role_trigger()
RETURNS TRIGGER AS $$
BEGIN
  -- Insert a row into role_privileges for the admin role and the new privilege
  INSERT INTO role_privileges (role_id, privilege)
  SELECT '280ab51b-efc8-4e69-9f6d-ef500e1e5d42'::UUID, NEW.name
  WHERE NOT EXISTS (
    SELECT 1 FROM role_privileges
    WHERE role_id = '280ab51b-efc8-4e69-9f6d-ef500e1e5d42'::UUID AND privilege = NEW.name
  );

  RETURN NULL; -- Since this is an AFTER trigger, we don't need to return anything
END;
$$ LANGUAGE plpgsql;

-- Create the trigger
CREATE TRIGGER fill_admin_role_trigger
AFTER INSERT ON privileges
FOR EACH ROW
EXECUTE FUNCTION fill_admin_role_trigger();

INSERT INTO privileges (name, group_name)
VALUES
    ('create_user', 'USER'),
    ('update_user', 'USER'),
    ('update_any_user', 'USER'),
    ('view_users', 'USER');

UPDATE
  users
SET
  role_id = '280ab51b-efc8-4e69-9f6d-ef500e1e5d42'
WHERE id = '72a911c8-ad24-4e2d-8930-9c3ba51741df';

UPDATE
  users
SET
  role_id = 'b3d83cf5-aab4-4cb1-a1ce-f5754d96ec2c'
WHERE id != '72a911c8-ad24-4e2d-8930-9c3ba51741df';
