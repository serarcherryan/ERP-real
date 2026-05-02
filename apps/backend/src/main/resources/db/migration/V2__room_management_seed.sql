insert into housing_zones(id, tenant_id, facility_id, name, status)
values ('zone-hecheng-elderly-care', 'tenant-yiyang', 'facility-hecheng', '和成养老', 'active');

insert into buildings(id, tenant_id, facility_id, zone_id, name, sort_order, status)
values
  ('building-1', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', '1栋', 1, 'active'),
  ('building-2', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', '2栋', 2, 'active');

insert into floors(id, tenant_id, facility_id, zone_id, building_id, name, floor_no, status)
values
  ('floor-1-3', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', 'building-1', '3楼', 3, 'active'),
  ('floor-1-5', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', 'building-1', '5楼', 5, 'active'),
  ('floor-2-2', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', 'building-2', '2楼', 2, 'active');

insert into rooms(id, tenant_id, facility_id, zone_id, building_id, floor_id, room_no, display_name, capacity, occupied_count, status)
values
  ('room-1-3-301', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', 'building-1', 'floor-1-3', '301', '301号房', 2, 1, 'available'),
  ('room-1-5-512', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', 'building-1', 'floor-1-5', '512', '512号房', 2, 2, 'occupied'),
  ('room-2-2-218', 'tenant-yiyang', 'facility-hecheng', 'zone-hecheng-elderly-care', 'building-2', 'floor-2-2', '218', '218号房', 1, 0, 'available');
