INSERT INTO sys_user(username, password)
VALUES ('admin', '123456')
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_user(username, password)
VALUES ('editor', '123456')
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_user(username, password)
VALUES ('viewer', '123456')
ON CONFLICT (username) DO NOTHING;
