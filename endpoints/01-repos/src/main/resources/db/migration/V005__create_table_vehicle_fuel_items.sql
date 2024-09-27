ALTER TABLE vehicles DROP COLUMN fuel_types;
ALTER TABLE vehicles DROP COLUMN fuel_tank_volume;

CREATE TABLE IF NOT EXISTS vehicle_fuel_items (
  id UUID PRIMARY KEY NOT NULL,
  vehicle_id UUID NOT NULL
    CONSTRAINT fk_vehicle_id REFERENCES vehicles (id) ON UPDATE CASCADE ON DELETE CASCADE,
  fuel_type FUEL_TYPE NOT NULL,
  fuel_tank_volume DOUBLE PRECISION NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false
);