-- BUG-2 fix: username uniqueness must be scoped to tenant, not global.
-- In a multi-tenant system, different tenants may have users with the same username.
-- The global UNIQUE on username prevents this and leaks tenant boundaries.

-- Drop the global unique index on username.
-- Use DROP INDEX which is compatible across both PostgreSQL and H2.
DROP INDEX IF EXISTS sys_users_username_key;

-- For H2 compatibility: the unique constraint may be named differently.
-- H2 in PostgreSQL mode names the auto-generated constraint based on column.
-- Using ALTER TABLE with a more portable approach.
ALTER TABLE sys_users ADD CONSTRAINT uk_sys_users_tenant_username UNIQUE (tenant_id, username);
