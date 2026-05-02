-- System users table for authentication
CREATE TABLE sys_users (
    id VARCHAR(80) PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(80) NOT NULL,
    tenant_id VARCHAR(80) NOT NULL,
    facility_id VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_users_tenant ON sys_users (tenant_id, facility_id);

-- Seed accounts: one per role, password = Erp@2026 (bcrypt hash)
INSERT INTO
    sys_users (
        id,
        username,
        password_hash,
        display_name,
        role,
        tenant_id,
        facility_id
    )
VALUES (
        'user-social-worker',
        'social_worker',
        '$2a$12$F89dtH3po7hpzD1ZoQGAqekNSbg2uywGKIcmrAeAqVusTGCQ5FuR2',
        '社工',
        'social-worker',
        'tenant-yiyang',
        'facility-hecheng'
    ),
    (
        'user-sw-supervisor',
        'sw_supervisor',
        '$2a$12$F89dtH3po7hpzD1ZoQGAqekNSbg2uywGKIcmrAeAqVusTGCQ5FuR2',
        '社工主管',
        'social-worker-supervisor',
        'tenant-yiyang',
        'facility-hecheng'
    ),
    (
        'user-dept-manager',
        'dept_manager',
        '$2a$12$F89dtH3po7hpzD1ZoQGAqekNSbg2uywGKIcmrAeAqVusTGCQ5FuR2',
        '部门经理',
        'department-manager',
        'tenant-yiyang',
        'facility-hecheng'
    ),
    (
        'user-prop-manager',
        'prop_manager',
        '$2a$12$F89dtH3po7hpzD1ZoQGAqekNSbg2uywGKIcmrAeAqVusTGCQ5FuR2',
        '物业经理',
        'property-manager',
        'tenant-yiyang',
        'facility-hecheng'
    ),
    (
        'user-prop-supervisor',
        'prop_supervisor',
        '$2a$12$F89dtH3po7hpzD1ZoQGAqekNSbg2uywGKIcmrAeAqVusTGCQ5FuR2',
        '物业主管',
        'property-supervisor',
        'tenant-yiyang',
        'facility-hecheng'
    );