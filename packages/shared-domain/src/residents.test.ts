import { describe, expect, it } from 'vitest';
import { filterResidents, maskIdentityNo, maskPhone, residents, toResidentView } from './residents';
import { hasPermission } from './roles';

describe('resident profile permissions', () => {
  it('masks sensitive fields for property roles', () => {
    const view = toResidentView(residents[0], 'property-supervisor');

    expect(view.maskedPhone).toBe('138****6721');
    expect(view.maskedIdentityNo).toBe('310101********6428');
    expect(view.primaryContact.maskedPhone).toBe('139****3455');
  });

  it('allows elderly-care supervisors to read sensitive fields', () => {
    const view = toResidentView(residents[0], 'social-worker-supervisor');

    expect(view.maskedPhone).toBe(residents[0].phone);
    expect(view.maskedIdentityNo).toBe(residents[0].identityNo);
  });

  it('keeps create/update permissions in elderly-care execution roles', () => {
    expect(hasPermission('social-worker', 'resident:create')).toBe(true);
    expect(hasPermission('social-worker', 'resident:update')).toBe(true);
    expect(hasPermission('property-manager', 'resident:update')).toBe(false);
  });

  it('filters residents by tenant-scoped view inputs without leaking hidden rows', () => {
    const result = filterResidents(residents, 'social-worker', {
      facilityId: 'facility-east',
      keyword: 'CY-2026',
    });

    expect(result).toHaveLength(2);
    expect(result.every((resident) => resident.facilityId === 'facility-east')).toBe(true);
  });

  it('flattens admission summary for cross-end list views', () => {
    const view = toResidentView(residents[0], 'social-worker');

    expect(view.room).toBe(residents[0].admission.room);
    expect(view.bed).toBe(residents[0].admission.bed);
    expect(view.careLevel).toBe(residents[0].admission.careLevel);
    expect(view.completenessScore).toBeGreaterThanOrEqual(90);
  });
});

describe('resident profile masking utilities', () => {
  it('masks common phone and identity formats', () => {
    expect(maskPhone('13812345678')).toBe('138****5678');
    expect(maskIdentityNo('110101194910013327')).toBe('110101********3327');
  });
});
