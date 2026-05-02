create table housing_zones (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  name varchar(120) not null,
  status varchar(30) not null,
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_housing_zones_name unique (tenant_id, facility_id, name)
);

create table buildings (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  zone_id varchar(80) not null references housing_zones(id),
  name varchar(120) not null,
  sort_order integer not null,
  status varchar(30) not null,
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_buildings_name unique (tenant_id, facility_id, zone_id, name)
);

create table floors (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  zone_id varchar(80) not null references housing_zones(id),
  building_id varchar(80) not null references buildings(id),
  name varchar(120) not null,
  floor_no integer not null,
  status varchar(30) not null,
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_floors_no unique (tenant_id, facility_id, building_id, floor_no)
);

create table rooms (
  id varchar(80) primary key,
  tenant_id varchar(80) not null,
  facility_id varchar(80) not null,
  zone_id varchar(80) not null references housing_zones(id),
  building_id varchar(80) not null references buildings(id),
  floor_id varchar(80) not null references floors(id),
  room_no varchar(40) not null,
  display_name varchar(120) not null,
  capacity integer not null,
  occupied_count integer not null default 0,
  status varchar(30) not null,
  version bigint not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint uk_rooms_no unique (tenant_id, facility_id, floor_id, room_no),
  constraint chk_rooms_capacity check (capacity > 0),
  constraint chk_rooms_occupied check (occupied_count >= 0 and occupied_count <= capacity)
);

create index idx_rooms_facility_status on rooms(tenant_id, facility_id, status);
create index idx_rooms_hierarchy on rooms(tenant_id, facility_id, zone_id, building_id, floor_id);
