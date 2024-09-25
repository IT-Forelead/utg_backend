INSERT INTO
  roles (id, name)
VALUES
  (
    '3e858380-ad3b-4fa3-bb98-dfe2104e5d5b',
    'machine_operator'
  );

CREATE TYPE MACHINE_OPERATOR_LICENSE_CATEGORY AS ENUM (
  'A',
  'B',
  'C',
  'D',
  'E',
  'F'
);

ALTER TABLE users
ADD COLUMN machine_operator_license_number VARCHAR NULL UNIQUE;
ALTER TABLE users
ADD COLUMN machine_operator_license_category _MACHINE_OPERATOR_LICENSE_CATEGORY NULL;
