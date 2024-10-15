CREATE TYPE VEHICLE_TYPE AS ENUM (
  'truck',
  'bus',
  'auto',
  'pickup',
  'special_vehicle',
  'trailer',
  'road_construction_vehicle'
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
  'electric',
  'kerosene'
);

CREATE TYPE GPS_TRACKING_TYPE AS ENUM (
  'not_installed',
  'installed',
  'enabled',
  'disabled'
);

CREATE TYPE WORKING_MODE_TYPE AS ENUM (
  'daily',
  'business_trip',
  'itinerary',
  'duty',
  'cleaning_day',
  'work_on_weekend',
  'work_on_holiday',
  'accidental'
);

CREATE TYPE VEHICLE_INDICATOR_ACTION_TYPE AS ENUM (
  'exit',
  'back'
);

CREATE TYPE HEALTH_TYPE AS ENUM (
  'healthy',
  'unhealthy'
);

CREATE TYPE DRIVING_LICENSE_CATEGORY AS ENUM (
  'A',
  'B',
  'C',
  'D',
  'BE',
  'CE',
  'DE'
);

CREATE TYPE MACHINE_OPERATOR_LICENSE_CATEGORY AS ENUM (
  'A',
  'B',
  'C',
  'D',
  'E',
  'F'
);

CREATE TYPE DELIVERY_STATUS AS ENUM (
  'sent',
  'delivered',
  'not_delivered',
  'failed',
  'transmitted',
  'undefined'
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

INSERT INTO
  regions (id, name)
VALUES
  (
    '4fcb3bc7-8459-45dc-a380-10f995e15ad8',
    'Андижон вилояти'
  ),
  (
    '122a0d83-fb8e-4dbf-a65d-3ee6a0688037',
    'Бухоро вилояти'
  ),
  (
    'd51b9830-7cb6-4420-a07e-c8df78d90447',
    'Фарғона вилояти'
  ),
  (
    'a4ec39b1-dfad-45e1-a12c-7986ffa4e4bf',
    'Жиззах вилояти'
  ),
  (
    '2d27b575-f952-4c93-8f9e-02c89758cbc7',
    'Наманган вилояти'
  ),
  (
    '51b00d57-1b99-47c5-b89c-8d1fab5825f6',
    'Навоий вилояти'
  ),
  (
    'f4bbb8aa-680f-4220-9079-b460e9f2e573',
    'Қашқадарё вилояти'
  ),
  (
    '425ff71e-57dd-459f-a831-cf57b30a7345',
    'Самарқанд вилояти'
  ),
  (
    '3acfc29c-3e14-4beb-96f6-20f025e431ab',
    'Сирдарё вилояти'
  ),
  (
    '54b834ee-0df9-465e-ad34-be1834b491d0',
    'Сурхондарё вилояти'
  ),
  (
    '3b316182-e55c-4e03-8811-052fcd888236',
    'Тошкент вилояти'
  ),
  (
    'ad514b71-3096-4be5-a455-d87abbb081b2',
    'Хоразм вилояти'
  ),
  (
    '8b88eb6c-24e1-4ecd-b944-8605d28da975',
    'Қорақалпоғистон Республикаси'
  ),
  (
    'dac35ec3-a904-42d7-af20-5d7e853fe1f6',
    'Тошкент шаҳри'
  );

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
  birthday DATE NULL,
  personal_number INT NOT NULL UNIQUE,
  phone VARCHAR NOT NULL UNIQUE,
  role_id UUID NOT NULL CONSTRAINT fk_user_role REFERENCES roles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  asset_id UUID NULL CONSTRAINT fk_user_asset REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  branch_code VARCHAR NULL,
  driving_license_number VARCHAR NULL UNIQUE,
  driving_license_categories _DRIVING_LICENSE_CATEGORY NULL,
  driving_license_given DATE NULL,
  driving_license_expire DATE NULL,
  machine_operator_license_number VARCHAR NULL UNIQUE,
  machine_operator_license_category _MACHINE_OPERATOR_LICENSE_CATEGORY NULL,
  machine_operator_license_given DATE NULL,
  machine_operator_license_expire DATE NULL,
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
  ),
  (
    'cdffe4e0-ac3b-474a-a4fd-144a5f832943',
    'main_mechanic'
  ),
  (
    '95fe6cba-7ea4-415e-8faf-500a3199dc14',
    'refueller'
  ),
  (
    '3e858380-ad3b-4fa3-bb98-dfe2104e5d5b',
    'machine_operator'
  );

INSERT INTO
  "users" (
    "id",
    "created_at",
    "firstname",
    "lastname",
    "personal_number",
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
    1,
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

INSERT INTO
  vehicle_categories (id, name, vehicle_type)
VALUES
  (
    '425dfbb8-f83b-4e84-87f9-97ffcc4e90be',
    'Бортли автомобил',
    'truck'
  ),
  (
    'df8b5b7e-9c46-4c9c-a12f-d465582ce93b',
    'Ўзи ағдарувчи автомобил',
    'truck'
  ),
  (
    '51903ae1-0df8-4f88-8166-3302e5bb5430',
    'Фургон',
    'truck'
  ),
  (
    '8986c735-4d7b-43d9-8818-330c00d3b8de',
    'Автоцистерна',
    'truck'
  ),
  (
    'f6676093-6a9b-4c33-8f97-2ba87ed8f757',
    'Эгарли шатакчи автомобил',
    'truck'
  ),
  (
    'd7c7a985-31f0-48df-a27b-03f6af382a18',
    'Қувур ташувчи автомобил',
    'truck'
  ),
  (
    'f10e0b28-31fa-4bb2-979f-08b80b30123d',
    'Тузилиши бўйича бошқа кузов',
    'truck'
  ),
  (
    '60647105-c2a8-45a2-92cd-336f7029aa38',
    'Автобус',
    'bus'
  ),
  (
    '8c1e02e5-af8b-41b4-82fc-5bad30625e4f',
    'Микроавтобус',
    'bus'
  ),
  (
    '462f7a98-a2aa-4c6c-ae9f-1092e059756e',
    'Енгил автомобиль',
    'auto'
  ),
  (
    '6b0aae22-4afe-4b1b-8ebf-6ade94f44a14',
    'Пикап',
    'pickup'
  ),
  (
    '67b00136-4642-431d-87c1-448f77e94195',
    'Енгил фургон',
    'pickup'
  ),
  (
    '74e60ba8-1bf7-4fb3-b067-20afba8ce3eb',
    'Махсус енгил автомобил',
    'special_vehicle'
  ),
  (
    'ed36f633-d3a9-4bd2-a14e-b10e4b27b2bc',
    'Ўт ўчирувчи автомобил',
    'special_vehicle'
  ),
  (
    '342ce703-2058-441f-95ad-b6a286dd5eca',
    'Автокран',
    'special_vehicle'
  ),
  (
    '0a24c6d8-2d62-4262-82e2-776aa66a4e56',
    'Бошқа турдаги махсус автомобил',
    'special_vehicle'
  ),
  (
    'bd7bc1f3-d2b1-4a90-bd9a-820810e96674',
    'Автомобил тиркамаси',
    'trailer'
  ),
  (
    '237c5322-114e-48b6-b784-0b5a96cb7ee6',
    'Автомобил ярим тиркамаси',
    'trailer'
  ),
  (
    '7ebf14ed-217e-44a9-8fb0-a27ccfa371cd',
    'Трактор тиркамаси',
    'trailer'
  ),
  (
    '9a220e31-cb2a-49a3-babd-3d06f53f50d0',
    'Трактор',
    'road_construction_vehicle'
  ),
  (
    'a27cb8c8-d9fb-4c1c-8145-b16d0be7a61e',
    'Экскаватор',
    'road_construction_vehicle'
  ),
  (
    '6471ce95-a459-4cd7-a072-4d39767928ad',
    'Бульдозер',
    'road_construction_vehicle'
  ),
  (
    'ca9bdce9-798f-4a04-a1ec-0e04489fdf76',
    'Қувурёткизгич',
    'road_construction_vehicle'
  ),
  (
    '8a1eaff8-097d-493b-830d-41edbfd93517',
    'Пайвандлаш агрегати',
    'road_construction_vehicle'
  ),
  (
    'e43e2f64-6e87-4bd5-9076-092a97423fd4',
    'Сув тўлдирувчи агрегат',
    'road_construction_vehicle'
  ),
  (
    '84f14f0c-c887-489d-b9ea-7cc31ae2fd4f',
    'Сиқувчи агрегат',
    'road_construction_vehicle'
  ),
  (
    'b24ccdc7-6400-463c-9882-bdd6b2349860',
    'Бошқа турдаги механизм',
    'road_construction_vehicle'
  );

CREATE TABLE IF NOT EXISTS vehicles (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  vehicle_type VEHICLE_TYPE NOT NULL,
  registered_number VARCHAR NULL UNIQUE,
  brand VARCHAR NOT NULL,
  color VARCHAR NULL,
  owner VARCHAR NULL,
  address VARCHAR NULL,
  date_of_issue DATE NULL,
  issuing_authority VARCHAR NULL,
  pin INT NULL,
  year_of_release INT NOT NULL,
  vehicle_category_id UUID NOT NULL
    CONSTRAINT fk_vehicle_category_id REFERENCES vehicle_categories (id) ON UPDATE CASCADE ON DELETE CASCADE,
  body_number VARCHAR NULL,
  chassis_number VARCHAR NULL,
  max_mass INT NOT NULL DEFAULT 0,
  unload_mass INT NOT NULL DEFAULT 0,
  engine_number VARCHAR NULL,
  engine_capacity INT NULL,
  number_of_seats INT NOT NULL DEFAULT 0,
  number_of_standing_places INT NOT NULL DEFAULT 0,
  special_marks VARCHAR NULL,
  license_number VARCHAR NULL,
  branch_id UUID NOT NULL
    CONSTRAINT fk_branch_id REFERENCES branches (id) ON UPDATE CASCADE ON DELETE CASCADE,
  inventory_number VARCHAR NOT NULL UNIQUE,
  condition CONDITION_TYPE NOT NULL,
  gps_tracking GPS_TRACKING_TYPE NULL,
  fuel_level_sensor DOUBLE PRECISION NULL,
  description VARCHAR NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS vehicle_fuel_items (
  id UUID PRIMARY KEY NOT NULL,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  fuel_type FUEL_TYPE NOT NULL,
  fuel_tank_volume DOUBLE PRECISION NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trips (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  serial_number VARCHAR NULL,
  first_tab VARCHAR NULL,
  second_tab VARCHAR NULL,
  third_tab VARCHAR NULL,
  work_order WORKING_MODE_TYPE NULL,
  summation VARCHAR NULL,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  notes VARCHAR NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_fuel_supplies (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL
    CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  dispatcher_id UUID NOT NULL
    CONSTRAINT fk_dispatcher_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  dispatcher_signature UUID NOT NULL
    CONSTRAINT fk_dispatcher_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_fuel_supply_items (
  id UUID PRIMARY KEY NOT NULL,
  trip_fuel_supply_id UUID NOT NULL
    CONSTRAINT fk_trip_fuel_supply_id REFERENCES trip_fuel_supplies (id) ON UPDATE CASCADE ON DELETE CASCADE,
  fuel_type FUEL_TYPE NOT NULL,
  fuel_supply DOUBLE PRECISION NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS medical_examinations (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_id UUID NOT NULL CONSTRAINT fk_driver_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_personal_number INT NOT NULL,
  complaint VARCHAR NULL,
  pulse INT NOT NULL,
  body_temperature DOUBLE PRECISION NOT NULL,
  blood_pressure VARCHAR NOT NULL,
  alcohol_concentration DOUBLE PRECISION NOT NULL,
  driver_health HEALTH_TYPE NOT NULL,
  doctor_id UUID NOT NULL
    CONSTRAINT fk_doctor_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  doctor_signature UUID NOT NULL
    CONSTRAINT fk_doctor_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_drivers (
  id UUID PRIMARY KEY NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_id UUID NOT NULL CONSTRAINT fk_driver_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driving_license_number VARCHAR NOT NULL,
  driver_health HEALTH_TYPE NULL,
  doctor_id UUID NULL
    CONSTRAINT fk_doctor_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  doctor_signature UUID NULL
    CONSTRAINT fk_doctor_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  medical_examination_id UUID NULL
    CONSTRAINT fk_medical_examination_id REFERENCES medical_examinations (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false,
  UNIQUE (trip_id, driver_id)
);

CREATE TABLE IF NOT EXISTS trip_trailers (
  id UUID PRIMARY KEY NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  trailer_id UUID NOT NULL CONSTRAINT fk_trailer_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false,
  UNIQUE (trip_id, trailer_id)
);

CREATE TABLE IF NOT EXISTS trip_semi_trailers (
  id UUID PRIMARY KEY NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  semi_trailer_id UUID NOT NULL CONSTRAINT fk_semi_trailer_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false,
  UNIQUE (trip_id, semi_trailer_id)
);

CREATE TABLE IF NOT EXISTS trip_accompanying_persons (
  id UUID PRIMARY KEY NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  user_id UUID NOT NULL CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false,
  UNIQUE (trip_id, user_id)
);

CREATE TABLE IF NOT EXISTS trip_vehicle_indicators (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_id UUID NOT NULL CONSTRAINT fk_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  action_type VEHICLE_INDICATOR_ACTION_TYPE NOT NULL,
  scheduled_time TIMESTAMP WITH TIME ZONE NOT NULL,
  current_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
  odometer_indicator DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  spent_hours DOUBLE PRECISION NULL,
  paid_distance DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_given_fuels (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL
    CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_trip_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  fuel_brand FUEL_TYPE NOT NULL,
  brand_code VARCHAR NULL,
  fuel_given DOUBLE PRECISION NOT NULL,
  refueler_id UUID NOT NULL
    CONSTRAINT fk_refueler_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  refueler_signature UUID NOT NULL
    CONSTRAINT fk_refueler_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_fuel_inspections (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL
    CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_trip_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  action_type VEHICLE_INDICATOR_ACTION_TYPE NOT NULL,
  mechanic_id UUID NOT NULL
    CONSTRAINT fk_mechanic_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  mechanic_signature UUID NOT NULL
    CONSTRAINT fk_mechanic_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_fuel_inspection_items (
  id UUID PRIMARY KEY NOT NULL,
  trip_fuel_inspection_id UUID NOT NULL
    CONSTRAINT fk_trip_fuel_inspection_id REFERENCES trip_fuel_inspections (id) ON UPDATE CASCADE ON DELETE CASCADE,
  fuel_type FUEL_TYPE NOT NULL,
  fuel_in_tank DOUBLE PRECISION NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_fuel_rates (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  fuel_type FUEL_TYPE NOT NULL,
  norm_change_coeff DOUBLE PRECISION NULL,
  equipment_working_time DOUBLE PRECISION NULL,
  engine_working_time DOUBLE PRECISION NULL,
  dispatcher_id UUID NULL
    CONSTRAINT fk_dispatcher_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  dispatcher_signature UUID NULL
    CONSTRAINT fk_dispatcher_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_driver_tasks (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  whose_discretion VARCHAR NOT NULL,
  arrival_time TIMESTAMP WITH TIME ZONE NULL,
  pickup_location VARCHAR NOT NULL,
  delivery_location VARCHAR NOT NULL,
  freight_name VARCHAR NOT NULL,
  number_of_interactions INT NULL,
  distance DOUBLE PRECISION NULL,
  freight_volume DOUBLE PRECISION NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_route_delays (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  name VARCHAR NOT NULL,
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
  end_time TIMESTAMP WITH TIME ZONE NOT NULL,
  user_id UUID NOT NULL
    CONSTRAINT fk_user_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  user_signature UUID NOT NULL
    CONSTRAINT fk_user_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_vehicle_acceptances (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL
    CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_trip_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  action_type VEHICLE_INDICATOR_ACTION_TYPE NOT NULL,
  condition CONDITION_TYPE NOT NULL,
  mechanic_id UUID NULL
    CONSTRAINT fk_mechanic_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  mechanic_signature UUID NULL
    CONSTRAINT fk_mechanic_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_id UUID NULL
    CONSTRAINT fk_driver_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_signature UUID NULL
    CONSTRAINT fk_driver_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_complete_tasks (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL
    CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  commute_number INT NOT NULL,
  load_numbers VARCHAR NOT NULL,
  arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
  consignor_full_name VARCHAR NOT NULL,
  consignor_signature UUID NOT NULL
    CONSTRAINT fk_consignor_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_id UUID NOT NULL
    CONSTRAINT fk_driver_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS trip_complete_task_acceptances (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  trip_id UUID NOT NULL
    CONSTRAINT fk_trip_id REFERENCES trips (id) ON UPDATE CASCADE ON DELETE CASCADE,
  commute_number_total INT NOT NULL,
  load_number_total INT NOT NULL,
  load_number_total_str VARCHAR NOT NULL,
  document_id UUID NULL
    CONSTRAINT fk_document_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_id UUID NULL
    CONSTRAINT fk_driver_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  driver_signature UUID NULL
    CONSTRAINT fk_driver_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  dispatcher_id UUID NULL
    CONSTRAINT fk_dispatcher_id REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE,
  dispatcher_signature UUID NULL
    CONSTRAINT fk_dispatcher_signature_id REFERENCES assets (id) ON UPDATE CASCADE ON DELETE CASCADE,
  deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS vehicle_histories (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  branch_id UUID NOT NULL
    CONSTRAINT fk_branch_id REFERENCES branches (id) ON UPDATE CASCADE ON DELETE CASCADE,
  registered_number VARCHAR NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS sms_messages (
  id UUID PRIMARY KEY NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  phone VARCHAR NOT NULL,
  text VARCHAR NOT NULL,
  status DELIVERY_STATUS NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);
