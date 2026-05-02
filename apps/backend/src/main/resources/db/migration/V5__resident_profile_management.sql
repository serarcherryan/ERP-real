alter table resident_profiles add column department_id varchar(80) not null default 'dept-care';
alter table resident_profiles add column resident_no varchar(40);
alter table resident_profiles add column preferred_name varchar(80);
alter table resident_profiles add column gender varchar(20) not null default 'unknown';
alter table resident_profiles add column birth_date date;
alter table resident_profiles add column identity_type varchar(40);
alter table resident_profiles add column identity_no_cipher text;
alter table resident_profiles add column identity_no_hash varchar(128);
alter table resident_profiles add column phone_cipher text;
alter table resident_profiles add column phone_hash varchar(128);
alter table resident_profiles add column household_address_cipher text;
alter table resident_profiles add column current_address_cipher text;
alter table resident_profiles add column status varchar(30) not null default 'Draft';
alter table resident_profiles add column admission_status varchar(30) not null default 'pending';
alter table resident_profiles add column admission_date date;
alter table resident_profiles add column contract_no varchar(80);
alter table resident_profiles add column zone_id varchar(80);
alter table resident_profiles add column building_id varchar(80);
alter table resident_profiles add column floor_id varchar(80);
alter table resident_profiles add column bed_id varchar(80);
alter table resident_profiles add column room_label varchar(120);
alter table resident_profiles add column nursing_zone varchar(80);
alter table resident_profiles add column care_level varchar(40);
alter table resident_profiles add column payment_type varchar(40);
alter table resident_profiles add column medical_insurance_type varchar(80);
alter table resident_profiles add column responsible_social_worker_id varchar(80);
alter table resident_profiles add column case_manager_id varchar(80);
alter table resident_profiles add column health_summary varchar(500);
alter table resident_profiles add column care_needs_text varchar(1000);
alter table resident_profiles add column completeness_score numeric(5,2) not null default 0;
alter table resident_profiles add column missing_fields_text varchar(1000);
alter table resident_profiles add column last_service_at timestamp;
alter table resident_profiles add column next_follow_up_date date;
alter table resident_profiles add column created_by varchar(80) not null default 'system';
alter table resident_profiles add column updated_by varchar(80) not null default 'system';
alter table resident_profiles add column archived_at timestamp;
alter table resident_profiles add column archived_reason varchar(500);

update resident_profiles
set resident_no = id,
    identity_type = 'unknown',
    identity_no_cipher = 'legacy-empty',
    identity_no_hash = id,
    birth_date = date '1900-01-01'
where resident_no is null;

alter table resident_profiles alter column resident_no set not null;
alter table resident_profiles alter column birth_date set not null;
alter table resident_profiles alter column identity_type set not null;
alter table resident_profiles alter column identity_no_cipher set not null;
alter table resident_profiles alter column identity_no_hash set not null;

create unique index uk_resident_profiles_tenant_no on resident_profiles(tenant_id, resident_no);
create unique index uk_resident_profiles_tenant_identity on resident_profiles(tenant_id, identity_no_hash);
create index idx_resident_profiles_facility_status on resident_profiles(tenant_id, facility_id, status);
create index idx_resident_profiles_updated_at on resident_profiles(tenant_id, facility_id, updated_at);

create table family_contacts (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  resident_id varchar(80) not null references resident_profiles(id),
  name varchar(80) not null,
  relation varchar(40) not null,
  phone_cipher text not null,
  phone_hash varchar(128) not null,
  address_cipher text,
  is_emergency boolean not null default false,
  is_guardian boolean not null default false,
  can_receive_notice boolean not null default false,
  priority int not null default 1,
  status varchar(30) not null default 'Active',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create index idx_family_contacts_resident on family_contacts(tenant_id, facility_id, resident_id, status);
create index idx_family_contacts_phone on family_contacts(tenant_id, phone_hash);

create table resident_health_summaries (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  resident_id varchar(80) not null references resident_profiles(id),
  blood_type varchar(20),
  allergy_summary_text varchar(1000),
  chronic_disease_summary_text varchar(1000),
  mobility_level varchar(80),
  cognitive_status varchar(80),
  diet_requirement varchar(120),
  fall_risk_level varchar(20),
  pressure_sore_risk_level varchar(20),
  emergency_plan varchar(500),
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_resident_health_summary unique (tenant_id, facility_id, resident_id)
);

create index idx_resident_health_fall_risk on resident_health_summaries(tenant_id, facility_id, fall_risk_level);

create table resident_tags (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  resident_id varchar(80) not null references resident_profiles(id),
  tag_code varchar(80) not null,
  tag_name varchar(80) not null,
  source varchar(40) not null default 'manual',
  status varchar(30) not null default 'Active',
  created_at timestamp not null default current_timestamp
);

create unique index uk_resident_tags_active on resident_tags(tenant_id, facility_id, resident_id, tag_code, status);
create index idx_resident_tags_resident on resident_tags(tenant_id, facility_id, resident_id, status);
