-- sample-data.sql — optional development data for the rental tracker.
--
-- What this is: sample data (owner, renters, items, rentals) to load while
-- developing, so you don't hand-enter data to test features. Not required.
--
-- Prerequisite: your tables must already exist. Check the table and column
-- names below against your own schema and correct any that differ.
--
-- Loading it, two ways:
--   - From your app: since it already creates the tables on start-up, add a
--     small dev command or flag that reads this file and runs its statements,
--     so one run sets up the tables and loads this data together.
--   - From the terminal, once the tables exist:  sqlite3 app.db < sample-data.sql
-- Reset: delete the database file, recreate the tables, then load this again.
--
-- Assumptions:
--   - Integer ids from 1, so the owner is user id 1 and the items belong to it.
--   - created_at is left to the database default, so it isn't inserted.

-- Users: id 1 is the owner, ids 2-4 are renters. OR IGNORE means that if you
-- already created your account through the app, your row is kept and this one
-- is skipped.
INSERT OR IGNORE INTO users (id, username, password) VALUES
  (1, 'liisa',  NULL),
  (2, 'meelis', NULL),
  (3, 'marili', NULL),
  (4, 'siim',   NULL);

-- 14 items owned by user 1: 6 available, 6 rented, 2 unlisted, so every list
-- (inventory, record-a-rental, confirm-a-return) clears 5 rows and paginates.
INSERT INTO listed_items (item_id, owner_id, item_name, description, cost_per_day, status) VALUES
  (1,  1, 'Ladder',          '6-step aluminium ladder',   5, 'available'),
  (2,  1, 'Drill',           'Cordless hammer drill',     4, 'rented'),
  (3,  1, 'Tent',            '4-person dome tent',        8, 'unlisted'),
  (4,  1, 'Pressure washer', 'Electric, 110 bar',         7, 'available'),
  (5,  1, 'Sander',          'Random orbital sander',     3, 'available'),
  (6,  1, 'Chainsaw',        'Petrol, 40cm bar',          9, 'rented'),
  (7,  1, 'Gazebo',          '3x3m pop-up gazebo',        6, 'available'),
  (8,  1, 'Wheelbarrow',     '90L builder''s barrow',     2, 'available'),
  (9,  1, 'Hedge trimmer',   'Electric, 60cm blade',      4, 'available'),
  (10, 1, 'Tile cutter',     'Manual, 600mm',             5, 'rented'),
  (11, 1, 'Cement mixer',    '120L drum',                12, 'rented'),
  (12, 1, 'Scaffold tower',  '4m working height',        15, 'rented'),
  (13, 1, 'Generator',       'Petrol, 2.2kW',            10, 'rented'),
  (14, 1, 'Jigsaw',          'Corded, variable speed',    3, 'unlisted');

-- 6 active rentals with different due dates (so confirm-a-return paginates and
-- its sort-by-due-date shows), plus one closed rental for history. Renters are
-- reused across rentals (meelis, marili, and siim each rent more than once).
INSERT INTO rentals (rental_id, item_id, renter_id, start_time, end_time, returned_at, status) VALUES
  (1, 2,  2, '2026-06-16 14:30', '2026-06-19 14:30', NULL,               'active'),
  (2, 6,  3, '2026-06-17 09:00', '2026-06-20 09:00', NULL,               'active'),
  (3, 10, 4, '2026-06-15 12:00', '2026-06-18 12:00', NULL,               'active'),
  (4, 11, 2, '2026-06-18 10:00', '2026-06-22 10:00', NULL,               'active'),
  (5, 12, 3, '2026-06-14 08:00', '2026-06-17 08:00', NULL,               'active'),
  (6, 13, 4, '2026-06-17 16:00', '2026-06-21 16:00', NULL,               'active'),
  (7, 1,  4, '2026-06-01 10:00', '2026-06-05 10:00', '2026-06-05 12:10', 'closed');
