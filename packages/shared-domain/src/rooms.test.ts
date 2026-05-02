import { describe, expect, it } from 'vitest';
import {
  assertHechengOnlyZone,
  canAssignResidentToRoom,
  getRoomLocationLabel,
  listRoomsByHierarchy,
  roomCatalog,
} from './rooms';

describe('room hierarchy', () => {
  it('keeps the current active zone limited to hecheng elderly care', () => {
    expect(() => assertHechengOnlyZone(roomCatalog)).not.toThrow();
    expect(roomCatalog.zones).toHaveLength(1);
    expect(roomCatalog.zones[0].name).toBe('和成养老');
  });

  it('formats room location as zone-building-floor-room', () => {
    expect(getRoomLocationLabel(roomCatalog, 'room-1-3-301')).toBe('和成养老 - 1栋 - 3楼 - 301号房');
  });

  it('lists rooms by hierarchy and tenant scope', () => {
    const rooms = listRoomsByHierarchy(roomCatalog, {
      tenantId: 'tenant-yiyang',
      facilityId: 'facility-hecheng',
      buildingId: 'building-1',
    });

    expect(rooms.map((room) => room.roomNo)).toEqual(['301', '512']);
  });

  it('only allows assignment to available rooms with remaining capacity', () => {
    const availableRoom = roomCatalog.rooms.find((room) => room.id === 'room-1-3-301');
    const occupiedRoom = roomCatalog.rooms.find((room) => room.id === 'room-1-5-512');

    expect(availableRoom && canAssignResidentToRoom(availableRoom)).toBe(true);
    expect(occupiedRoom && canAssignResidentToRoom(occupiedRoom)).toBe(false);
  });
});
