
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
  phone VARCHAR NOT NULL,
  role_id UUID NOT NULL CONSTRAINT fk_user_role REFERENCES roles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  asset_id UUID NULL CONSTRAINT fk_user_asset REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  password VARCHAR NOT NULL
);

INSERT INTO
  roles (id, name)
VALUES
  (
    '280ab51b-efc8-4e69-9f6d-ef500e1e5d42',
    'dispatcher'
  ),
  (
    'b3d83cf5-aab4-4cb1-a1ce-f5754d96ec2c',
    'mechanic'
  ),
  (
    '076ab1e9-a420-434e-9f8d-7d811e70f73e',
    'medical_stuff'
  ),
  (
    '4d069c2d-b741-43a3-8bc1-5bcd720240c9',
    'driver'
  ),
  (
    '18ca8ef7-7464-4270-8ce2-0c9908b3bc72',
    'manager'
  ),
  (
    '7aa5ba51-5f32-4123-b88c-aca7c8e7b033',
    'super_manager'
  ),
  (
    '847f4f8f-0856-4e2a-8838-caab6816f69c',
    'viewer'
  );

INSERT INTO
  "users" (
    "id",
    "created_at",
    "firstname",
    "lastname",
    "login",
    "phone",
    "role_id",
    "password"
  )
VALUES
  (
    '72a911c8-ad24-4e2d-8930-9c3ba51741df',
    '2022-11-07T06:43:01.089Z',
    'Admin',
    'Super Manager',
    'super_manager',
    '+998901234567',
    '7aa5ba51-5f32-4123-b88c-aca7c8e7b033',
    '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY='
  );

  -- Create the trigger function
CREATE OR REPLACE FUNCTION fill_admin_role_trigger()
RETURNS TRIGGER AS $$
BEGIN
  -- Insert a row into role_privileges for the admin role and the new privilege
  INSERT INTO role_privileges (role_id, privilege)
  SELECT '7aa5ba51-5f32-4123-b88c-aca7c8e7b033'::UUID, NEW.name
  WHERE NOT EXISTS (
    SELECT 1 FROM role_privileges
    WHERE role_id = '7aa5ba51-5f32-4123-b88c-aca7c8e7b033'::UUID AND privilege = NEW.name
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
