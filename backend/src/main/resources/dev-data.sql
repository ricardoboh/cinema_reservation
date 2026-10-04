-- Loaded only by application-dev.yaml, after Hibernate creates the disposable schema.
INSERT INTO cinema_users (id) VALUES (1), (2), (3);

INSERT INTO screening (id, movie, hall, starts_at) VALUES
    (1, 'CP1 Demo Movie', 'Hall A', DATEADD('DAY', 1, CURRENT_TIMESTAMP)),
    (2, 'C02 Cutoff Demo', 'Hall A', DATEADD('MINUTE', 10, CURRENT_TIMESTAMP)),
    (3, 'C02 Started Demo', 'Hall A', DATEADD('HOUR', -1, CURRENT_TIMESTAMP));

INSERT INTO seat (id, hall, row_number, seat_number) VALUES
    (1, 'Hall A', 1, 1),
    (2, 'Hall A', 1, 2),
    (3, 'Hall A', 1, 3),
    (4, 'Hall A', 1, 4);

-- Explicit fixture IDs must not collide with subsequent generated IDs.
INSERT INTO reservation (id, user_id, screening_id, status, created_at) VALUES
    (100, 1, 3, 'DRAFT', DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
    (101, 1, 3, 'CONFIRMED', DATEADD('DAY', -1, CURRENT_TIMESTAMP));
INSERT INTO reservation_seats (reservation_id, seat_id) VALUES (100, 1), (101, 2);
ALTER TABLE reservation ALTER COLUMN id RESTART WITH 1;
ALTER TABLE cinema_users ALTER COLUMN id RESTART WITH 4;
ALTER TABLE screening ALTER COLUMN id RESTART WITH 4;
ALTER TABLE seat ALTER COLUMN id RESTART WITH 5;
