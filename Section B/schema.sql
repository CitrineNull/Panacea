CREATE TABLE IF NOT EXISTS discounts (nonce TEXT PRIMARY KEY, expiration INTEGER, key_id TEXT)

-- CREATE TABLE IF NOT EXISTS products (barcode TEXT PRIMARY KEY, long_name TEXT, short_name TEXT, gross_weight INTEGER, price INTEGER, age_restricted INTEGER)
-- SQLite3 doesn't have boolean data type, so it would be stored as 0 or 1

-- Other tables for products, employees, etc. can be initialised here too