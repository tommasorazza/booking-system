-- ======================
-- Tenants
-- ======================
INSERT INTO tenant (id, name) VALUES
                                  ('11111111-1111-1111-1111-111111111111', 'Tenant A'),
                                  ('22222222-2222-2222-2222-222222222222', 'Tenant B');

-- ======================
-- Users
-- ======================
INSERT INTO app_user (id, email, password, role, tenant_id) VALUES
                                                                ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'alice@example.com', 'password', 'USER', '11111111-1111-1111-1111-111111111111'),
                                                                ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'bob@example.com', 'password', 'USER', '22222222-2222-2222-2222-222222222222'),
                                                                ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'admin@example.com', 'admin_pass', 'ADMIN', '11111111-1111-1111-1111-111111111111');

-- ======================
-- Events
-- ======================
INSERT INTO event (id, name, description, location, date, available_capacity, total_capacity, tenant_id, version) VALUES
                                                                                                                      ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Spring Boot Workshop', 'Learn Spring Boot basics', 'Room 101', NOW() + INTERVAL '1 day', 30, 30, '11111111-1111-1111-1111-111111111111', 0),
                                                                                                                      ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Docker Deep Dive', 'Hands-on Docker session', 'Room 202', NOW() + INTERVAL '2 days', 25, 25, '22222222-2222-2222-2222-222222222222', 0);

-- ======================
-- Bookings
-- ======================
INSERT INTO booking (id, user_id, event_id, tenant_id, quantity, status, created_at) VALUES
                                                                                         ('99999999-9999-9999-9999-999999999999', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '11111111-1111-1111-1111-111111111111', 2, 'CONFIRMED', NOW()),
                                                                                         ('88888888-8888-8888-8888-888888888888', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ffffffff-ffff-ffff-ffff-ffffffffffff', '22222222-2222-2222-2222-222222222222', 1, 'CONFIRMED', NOW());