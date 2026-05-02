create table audit_logs (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  module_name varchar(80) not null,
  action varchar(80) not null,
  resource_type varchar(80) not null,
  resource_id varchar(80) not null,
  operator_id varchar(80) not null,
  operator_role varchar(80) not null,
  trace_id varchar(120),
  detail varchar(1000),
  created_at timestamp not null default current_timestamp
);

create index idx_audit_logs_resource on audit_logs(tenant_id, facility_id, resource_type, resource_id);
create index idx_audit_logs_created_at on audit_logs(created_at);

create table idempotency_records (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  idempotency_key varchar(160) not null,
  action varchar(80) not null,
  request_hash varchar(128) not null,
  resource_type varchar(80),
  resource_id varchar(80),
  status varchar(30) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_idempotency_key unique (tenant_id, facility_id, action, idempotency_key)
);

create table resident_profiles (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  name varchar(120) not null,
  room_id varchar(80),
  bed_label varchar(40),
  living_location_label varchar(240),
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create index idx_resident_profiles_room on resident_profiles(tenant_id, facility_id, room_id);

create table bed_assignments (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  resident_id varchar(80) not null references resident_profiles(id),
  room_id varchar(80) not null references rooms(id),
  bed_label varchar(40) not null,
  status varchar(30) not null,
  start_at timestamp not null default current_timestamp,
  end_at timestamp,
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_bed_assignment_active_resident unique (tenant_id, facility_id, resident_id, status),
  constraint uk_bed_assignment_active_bed unique (tenant_id, facility_id, room_id, bed_label, status)
);

create index idx_bed_assignments_room on bed_assignments(tenant_id, facility_id, room_id, status);

insert into resident_profiles(id, tenant_id, facility_id, name)
values
  ('resident-001', 'tenant-yiyang', 'facility-hecheng', '张建国'),
  ('resident-002', 'tenant-yiyang', 'facility-hecheng', '李桂兰');
