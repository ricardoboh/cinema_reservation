-- Loaded only by application-dev.yaml, after Hibernate creates the disposable schema.
INSERT INTO cinema_users (id) VALUES (1);

INSERT INTO screening (id, movie, hall, starts_at) VALUES
    (1, 'CP1 Demo Movie', 'Hall A', DATEADD('DAY', 1, CURRENT_TIMESTAMP)),
    (2, 'CP1 Cutoff Demo', 'Hall A', DATEADD('MINUTE', 10, CURRENT_TIMESTAMP));

INSERT INTO seat (id, hall, row_number, seat_number) VALUES
    (1, 'Hall A', 1, 1),
    (2, 'Hall A', 1, 2),
    (3, 'Hall A', 1, 3),
    (4, 'Hall A', 1, 4);

-- Explicit fixture IDs must not collide with subsequent generated IDs.
ALTER TABLE cinema_users ALTER COLUMN id RESTART WITH 2;
ALTER TABLE screening ALTER COLUMN id RESTART WITH 3;
ALTER TABLE seat ALTER COLUMN id RESTART WITH 5;
