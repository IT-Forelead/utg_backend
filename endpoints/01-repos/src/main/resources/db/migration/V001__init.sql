CREATE TYPE VEHICLE_TYPE AS ENUM (
  'auto',
  'special_road_vehicles',
  'trailer',
  'welding_equipment',
  'other_mechanism'
);

CREATE TYPE CONDITION_TYPE AS ENUM (
  'valid',
  'invalid',
  'write_off'
);

CREATE TYPE FUEL_TYPE AS ENUM (
  'petrol',
  'diesel',
  'methane',
  'propane',
  'hybrid',
  'electric'
);

CREATE TYPE GPS_TRACKING_TYPE AS ENUM (
  'not_installed',
  'installed',
  'enabled',
  'disabled'
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

CREATE TABLE IF NOT EXISTS regions
(
    id      UUID PRIMARY KEY,
    name    VARCHAR NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT false
);

INSERT INTO "regions" ("id", "name")
VALUES ('4fcb3bc7-8459-45dc-a380-10f995e15ad8', 'Андижон вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('122a0d83-fb8e-4dbf-a65d-3ee6a0688037', 'Бухоро вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('d51b9830-7cb6-4420-a07e-c8df78d90447', 'Фарғона вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('a4ec39b1-dfad-45e1-a12c-7986ffa4e4bf', 'Жиззах вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('2d27b575-f952-4c93-8f9e-02c89758cbc7', 'Наманган вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('51b00d57-1b99-47c5-b89c-8d1fab5825f6', 'Навоий вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('f4bbb8aa-680f-4220-9079-b460e9f2e573', 'Қашқадарё вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('425ff71e-57dd-459f-a831-cf57b30a7345', 'Самарқанд вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('3acfc29c-3e14-4beb-96f6-20f025e431ab', 'Сирдарё вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('54b834ee-0df9-465e-ad34-be1834b491d0', 'Сурхондарё вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('3b316182-e55c-4e03-8811-052fcd888236', 'Тошкент вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('ad514b71-3096-4be5-a455-d87abbb081b2', 'Хоразм вилояти');
INSERT INTO "regions" ("id", "name")
VALUES ('8b88eb6c-24e1-4ecd-b944-8605d28da975', 'Қорақалпоғистон Республикаси');
INSERT INTO "regions" ("id", "name")
VALUES ('dac35ec3-a904-42d7-af20-5d7e853fe1f6', 'Тошкент шаҳри');

CREATE TABLE IF NOT EXISTS branches (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  code VARCHAR NOT NULL UNIQUE,
  region_id UUID NOT NULL CONSTRAINT fk_region_id REFERENCES regions (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  firstname VARCHAR NOT NULL,
  lastname VARCHAR NOT NULL,
  middle_name VARCHAR NULL,
  phone VARCHAR NOT NULL,
  role_id UUID NOT NULL CONSTRAINT fk_user_role REFERENCES roles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  asset_id UUID NULL CONSTRAINT fk_user_asset REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  branch_code VARCHAR NULL,
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
    "phone",
    "role_id",
    "branch_code",
    "password"
  )
VALUES
  (
    '72a911c8-ad24-4e2d-8930-9c3ba51741df',
    '2022-11-07T06:43:01.089Z',
    'Admin',
    'Super Manager',
    '+998901234567',
    '7aa5ba51-5f32-4123-b88c-aca7c8e7b033',
    null,
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

CREATE TABLE IF NOT EXISTS vehicle_categories (
  id UUID PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  vehicle_type VEHICLE_TYPE NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS vehicles (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  branch_id UUID NOT NULL
    CONSTRAINT fk_branch_id REFERENCES branches (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_category_id UUID NOT NULL
    CONSTRAINT fk_vehicle_category REFERENCES vehicle_categories (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_type VEHICLE_TYPE NOT NULL,
  brand VARCHAR NOT NULL,
  registered_number VARCHAR NULL UNIQUE,
  inventory_number VARCHAR NOT NULL UNIQUE,
  year_of_release INT NOT NULL,
  body_number VARCHAR NULL,
  chassis_number VARCHAR NULL,
  engine_number VARCHAR NULL,
  condition CONDITION_TYPE NOT NULL,
  fuel_type FUEL_TYPE NULL,
  description VARCHAR NULL,
  gps_tracking GPS_TRACKING_TYPE NULL,
  fuel_level_sensor DOUBLE PRECISION NULL,
  fuel_tank_volume DOUBLE PRECISION NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);
