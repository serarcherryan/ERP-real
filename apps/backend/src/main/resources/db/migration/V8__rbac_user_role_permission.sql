alter table sys_users add column is_super_admin boolean not null default false;
alter table sys_users add column permission_version bigint not null default 1;

create table permissions (
  id varchar(80) primary key,
  code varchar(120) not null unique,
  name varchar(120) not null,
  module_name varchar(80) not null,
  action varchar(80) not null,
  risk_level varchar(20) not null,
  system_builtin boolean not null default true,
  created_at timestamp not null default current_timestamp
);

create table roles (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  code varchar(80) not null,
  name varchar(120) not null,
  description varchar(500),
  system_builtin boolean not null default true,
  enabled boolean not null default true,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_roles_tenant_code unique (tenant_id, code)
);

create table user_roles (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  user_id varchar(80) not null references sys_users(id),
  role_id varchar(80) not null references roles(id),
  created_at timestamp not null default current_timestamp,
  constraint uk_user_roles unique (tenant_id, user_id, role_id)
);

create table role_permissions (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  role_id varchar(80) not null references roles(id),
  permission_id varchar(80) not null references permissions(id),
  created_by varchar(80) not null,
  created_at timestamp not null default current_timestamp,
  constraint uk_role_permissions unique (tenant_id, role_id, permission_id)
);

create index idx_user_roles_user on user_roles(tenant_id, user_id);
create index idx_role_permissions_role on role_permissions(tenant_id, role_id);

insert into permissions(id, code, name, module_name, action, risk_level)
values
  ('perm-resident-profile-create', 'resident.profile:create', '新增长者档案', 'resident-profile', 'create', 'P0'),
  ('perm-resident-profile-read', 'resident.profile:read', '查看长者档案', 'resident-profile', 'read', 'P0'),
  ('perm-resident-profile-update', 'resident.profile:update', '修改长者档案', 'resident-profile', 'update', 'P0'),
  ('perm-resident-profile-delete', 'resident.profile:delete', '删除长者档案', 'resident-profile', 'delete', 'P0'),
  ('perm-identity-user-create', 'identity.user:create', '新增用户', 'identity', 'create-user', 'P0'),
  ('perm-identity-user-read', 'identity.user:read', '查看用户', 'identity', 'read-user', 'P0'),
  ('perm-identity-user-update', 'identity.user:update', '修改用户', 'identity', 'update-user', 'P0'),
  ('perm-identity-user-disable', 'identity.user:disable', '禁用用户', 'identity', 'disable-user', 'P0'),
  ('perm-identity-role-read', 'identity.role:read', '查看角色权限', 'identity', 'read-role', 'P0'),
  ('perm-identity-role-update-permissions', 'identity.role:update_permissions', '修改角色权限', 'identity', 'update-role-permissions', 'P0');

insert into roles(id, tenant_id, code, name, description)
values
  ('role-social-worker', 'tenant-yiyang', 'social-worker', '社工', '养老部门一线社工'),
  ('role-social-worker-supervisor', 'tenant-yiyang', 'social-worker-supervisor', '社工经理', '养老部门社工经理/社工主管'),
  ('role-department-manager', 'tenant-yiyang', 'department-manager', '部门经理', '养老部门经理'),
  ('role-property-supervisor', 'tenant-yiyang', 'property-supervisor', '物业主管', '物业部门主管'),
  ('role-property-manager', 'tenant-yiyang', 'property-manager', '物业经理', '物业部门经理'),
  ('role-admin', 'tenant-yiyang', 'admin', '管理员', '平台管理员');

insert into user_roles(id, tenant_id, user_id, role_id)
values
  ('user-role-social-worker', 'tenant-yiyang', 'user-social-worker', 'role-social-worker'),
  ('user-role-sw-supervisor', 'tenant-yiyang', 'user-sw-supervisor', 'role-social-worker-supervisor'),
  ('user-role-dept-manager', 'tenant-yiyang', 'user-dept-manager', 'role-department-manager'),
  ('user-role-prop-supervisor', 'tenant-yiyang', 'user-prop-supervisor', 'role-property-supervisor'),
  ('user-role-prop-manager', 'tenant-yiyang', 'user-prop-manager', 'role-property-manager');

insert into role_permissions(id, tenant_id, role_id, permission_id, created_by)
select 'rp-social-worker-' || p.id, 'tenant-yiyang', 'role-social-worker', p.id, 'system'
from permissions p
where p.code in ('resident.profile:create', 'resident.profile:read');

insert into role_permissions(id, tenant_id, role_id, permission_id, created_by)
select 'rp-sw-supervisor-' || p.id, 'tenant-yiyang', 'role-social-worker-supervisor', p.id, 'system'
from permissions p
where p.code in ('resident.profile:create', 'resident.profile:read', 'resident.profile:update', 'resident.profile:delete');

insert into role_permissions(id, tenant_id, role_id, permission_id, created_by)
select 'rp-dept-manager-' || p.id, 'tenant-yiyang', 'role-department-manager', p.id, 'system'
from permissions p
where p.code in (
  'resident.profile:create', 'resident.profile:read', 'resident.profile:update', 'resident.profile:delete',
  'identity.user:create', 'identity.user:read', 'identity.user:update', 'identity.user:disable',
  'identity.role:read', 'identity.role:update_permissions'
);

insert into role_permissions(id, tenant_id, role_id, permission_id, created_by)
select 'rp-prop-supervisor-' || p.id, 'tenant-yiyang', 'role-property-supervisor', p.id, 'system'
from permissions p
where p.code in ('resident.profile:create', 'resident.profile:read', 'resident.profile:update', 'resident.profile:delete');

insert into role_permissions(id, tenant_id, role_id, permission_id, created_by)
select 'rp-prop-manager-' || p.id, 'tenant-yiyang', 'role-property-manager', p.id, 'system'
from permissions p
where p.code in (
  'resident.profile:create', 'resident.profile:read', 'resident.profile:update', 'resident.profile:delete',
  'identity.user:create', 'identity.user:read', 'identity.user:update', 'identity.user:disable',
  'identity.role:read', 'identity.role:update_permissions'
);

insert into role_permissions(id, tenant_id, role_id, permission_id, created_by)
select 'rp-admin-' || p.id, 'tenant-yiyang', 'role-admin', p.id, 'system'
from permissions p;

insert into
    sys_users (
        id,
        username,
        password_hash,
        display_name,
        role,
        tenant_id,
        facility_id,
        enabled,
        is_super_admin,
        permission_version
    )
values (
        'user-admin',
        'admin',
        '$2a$12$F89dtH3po7hpzD1ZoQGAqekNSbg2uywGKIcmrAeAqVusTGCQ5FuR2',
        '管理员',
        'admin',
        'tenant-yiyang',
        'facility-hecheng',
        true,
        true,
        1
    );

insert into user_roles(id, tenant_id, user_id, role_id)
values ('user-role-admin', 'tenant-yiyang', 'user-admin', 'role-admin');
