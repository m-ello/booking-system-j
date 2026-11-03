-- Resources become restaurant tables
ALTER TABLE resources RENAME TO dining_tables;

ALTER TABLE dining_tables RENAME COLUMN name TO label;
ALTER TABLE dining_tables RENAME COLUMN capacity TO seats;
ALTER TABLE dining_tables ADD COLUMN zone VARCHAR(20) NOT NULL DEFAULT 'INDOOR';

ALTER TABLE dining_tables RENAME CONSTRAINT chk_resources_capacity TO chk_dining_tables_seats;
ALTER TABLE dining_tables
    ADD CONSTRAINT chk_dining_tables_zone CHECK (zone IN ('INDOOR', 'TERRACE', 'BAR', 'PRIVATE'));

-- Bookings now reference a table and carry a party size
ALTER TABLE bookings RENAME COLUMN resource_id TO table_id;
ALTER TABLE bookings ADD COLUMN party_size INTEGER NOT NULL DEFAULT 1;
ALTER TABLE bookings ALTER COLUMN party_size DROP DEFAULT;
ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_party_size CHECK (party_size > 0);

ALTER INDEX idx_bookings_resource_time RENAME TO idx_bookings_table_time;