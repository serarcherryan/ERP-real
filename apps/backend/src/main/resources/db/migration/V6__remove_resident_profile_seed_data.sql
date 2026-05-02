delete from resident_tags
where resident_id in ('resident-001', 'resident-002');

delete from resident_health_summaries
where resident_id in ('resident-001', 'resident-002');

delete from family_contacts
where resident_id in ('resident-001', 'resident-002');

delete from resident_profiles
where id in ('resident-001', 'resident-002')
  and not exists (
    select 1
    from bed_assignments
    where bed_assignments.resident_id = resident_profiles.id
  );
