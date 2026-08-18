ALTER TABLE tickets ADD COLUMN organization_id BIGINT;

INSERT INTO organizations (name, created_at) VALUES ('default', CURRENT_TIMESTAMP);
UPDATE tickets SET organization_id = (SELECT id FROM organizations WHERE name = 'default');

ALTER TABLE tickets ALTER COLUMN organization_id BIGINT NOT NULL;
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_organization FOREIGN KEY (organization_id) REFERENCES organizations (id);