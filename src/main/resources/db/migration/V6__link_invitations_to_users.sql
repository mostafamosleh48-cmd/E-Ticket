DELETE FROM invitations;

ALTER TABLE invitations DROP COLUMN email;

ALTER TABLE invitations ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE invitations
    ADD CONSTRAINT fk_invitations_user FOREIGN KEY (user_id) REFERENCES users (id);
