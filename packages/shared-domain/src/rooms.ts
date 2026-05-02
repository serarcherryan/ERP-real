export type RoomStatus = 'available' | 'occupied' | 'maintenance' | 'inactive';

export interface HousingZone {
  id: string;
  tenantId: string;
  facilityId: string;
  name: string;
  status: 'active' | 'inactive';
}

export interface Building {
  id: string;
  tenantId: string;
  facilityId: string;
  zoneId: string;
  name: string;
  sortOrder: number;
}

export interface Floor {
  id: string;
  tenantId: string;
  facilityId: string;
  zoneId: string;
  buildingId: string;
  name: string;
  floorNo: number;
}

export interface Room {
  id: string;
  tenantId: string;
  facilityId: string;
  zoneId: string;
  buildingId: string;
  floorId: string;
  roomNo: string;
  displayName: string;
  capacity: number;
  occupiedCount: number;
  status: RoomStatus;
}

export interface RoomLocation {
  zone: HousingZone;
  building: Building;
  floor: Floor;
  room: Room;
}

export interface RoomCatalog {
  zones: HousingZone[];
  buildings: Building[];
  floors: Floor[];
  rooms: Room[];
}

export const HECHENG_ZONE_ID = 'zone-hecheng-elderly-care';
export const HECHENG_ZONE_NAME = '和成养老';

export const roomCatalog: RoomCatalog = {
  zones: [
    {
      id: HECHENG_ZONE_ID,
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      name: HECHENG_ZONE_NAME,
      status: 'active',
    },
  ],
  buildings: [
    {
      id: 'building-1',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      name: '1栋',
      sortOrder: 1,
    },
    {
      id: 'building-2',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      name: '2栋',
      sortOrder: 2,
    },
  ],
  floors: [
    {
      id: 'floor-1-3',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-1',
      name: '3楼',
      floorNo: 3,
    },
    {
      id: 'floor-1-5',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-1',
      name: '5楼',
      floorNo: 5,
    },
    {
      id: 'floor-2-2',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-2',
      name: '2楼',
      floorNo: 2,
    },
  ],
  rooms: [
    {
      id: 'room-1-3-301',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-1',
      floorId: 'floor-1-3',
      roomNo: '301',
      displayName: '301号房',
      capacity: 2,
      occupiedCount: 1,
      status: 'available',
    },
    {
      id: 'room-1-5-512',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-1',
      floorId: 'floor-1-5',
      roomNo: '512',
      displayName: '512号房',
      capacity: 2,
      occupiedCount: 2,
      status: 'occupied',
    },
    {
      id: 'room-2-2-218',
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-2',
      floorId: 'floor-2-2',
      roomNo: '218',
      displayName: '218号房',
      capacity: 1,
      occupiedCount: 0,
      status: 'available',
    },
  ],
};

export function getRoomLocation(catalog: RoomCatalog, roomId: string): RoomLocation {
  const room = catalog.rooms.find((item) => item.id === roomId);
  if (!room) {
    throw new Error(`ROOM_NOT_FOUND:${roomId}`);
  }

  const zone = catalog.zones.find((item) => item.id === room.zoneId);
  const building = catalog.buildings.find((item) => item.id === room.buildingId);
  const floor = catalog.floors.find((item) => item.id === room.floorId);

  if (!zone || !building || !floor) {
    throw new Error(`ROOM_HIERARCHY_BROKEN:${roomId}`);
  }

  if (
    room.tenantId !== zone.tenantId ||
    room.facilityId !== zone.facilityId ||
    room.zoneId !== building.zoneId ||
    room.buildingId !== floor.buildingId ||
    room.zoneId !== floor.zoneId
  ) {
    throw new Error(`ROOM_HIERARCHY_MISMATCH:${roomId}`);
  }

  return { zone, building, floor, room };
}

export function formatRoomLocation(location: RoomLocation) {
  return `${location.zone.name} - ${location.building.name} - ${location.floor.name} - ${location.room.displayName}`;
}

export function getRoomLocationLabel(catalog: RoomCatalog, roomId: string) {
  return formatRoomLocation(getRoomLocation(catalog, roomId));
}

export function listRoomsByHierarchy(
  catalog: RoomCatalog,
  options: {
    tenantId: string;
    facilityId: string;
    zoneId?: string;
    buildingId?: string;
    floorId?: string;
    status?: RoomStatus;
  },
) {
  return catalog.rooms.filter((room) => {
    if (room.tenantId !== options.tenantId) return false;
    if (room.facilityId !== options.facilityId) return false;
    if (options.zoneId && room.zoneId !== options.zoneId) return false;
    if (options.buildingId && room.buildingId !== options.buildingId) return false;
    if (options.floorId && room.floorId !== options.floorId) return false;
    if (options.status && room.status !== options.status) return false;
    return true;
  });
}

export function assertHechengOnlyZone(catalog: RoomCatalog) {
  const activeZones = catalog.zones.filter((zone) => zone.status === 'active');
  if (activeZones.length !== 1 || activeZones[0].name !== HECHENG_ZONE_NAME) {
    throw new Error('ROOM_ZONE_SCOPE_INVALID');
  }
}

export function canAssignResidentToRoom(room: Room) {
  return room.status === 'available' && room.occupiedCount < room.capacity;
}
