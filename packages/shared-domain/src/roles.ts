export type Department = 'elderly-care' | 'property';

export type RoleKey =
  | 'social-worker'
  | 'social-worker-supervisor'
  | 'department-manager'
  | 'property-manager'
  | 'property-supervisor';

export type Permission =
  | 'resident:read'
  | 'resident:create'
  | 'resident:update'
  | 'resident:void'
  | 'resident:export'
  | 'resident:sensitive:read';

export interface RoleDefinition {
  key: RoleKey;
  name: string;
  department: Department;
  permissions: Permission[];
  description: string;
}

export const roleDefinitions: RoleDefinition[] = [
  {
    key: 'social-worker',
    name: '社工',
    department: 'elderly-care',
    permissions: ['resident:read', 'resident:create', 'resident:update'],
    description: '负责长者日常档案维护、联系人更新和服务跟进。',
  },
  {
    key: 'social-worker-supervisor',
    name: '社工主管',
    department: 'elderly-care',
    permissions: [
      'resident:read',
      'resident:create',
      'resident:update',
      'resident:void',
      'resident:sensitive:read',
    ],
    description: '负责档案审核、作废审批和敏感信息复核。',
  },
  {
    key: 'department-manager',
    name: '部门经理',
    department: 'elderly-care',
    permissions: [
      'resident:read',
      'resident:void',
      'resident:export',
      'resident:sensitive:read',
    ],
    description: '查看养老部门经营视图，审批作废和导出。',
  },
  {
    key: 'property-manager',
    name: '物业经理',
    department: 'property',
    permissions: ['resident:read', 'resident:export'],
    description: '从物业视角查看入住、房间和服务协同信息。',
  },
  {
    key: 'property-supervisor',
    name: '物业主管',
    department: 'property',
    permissions: ['resident:read'],
    description: '查看授权长者的房间、标签和服务协同信息。',
  },
];

export const roleByKey = Object.fromEntries(
  roleDefinitions.map((role) => [role.key, role]),
) as Record<RoleKey, RoleDefinition>;

export function hasPermission(roleKey: RoleKey, permission: Permission) {
  return roleByKey[roleKey].permissions.includes(permission);
}

export function getRoleDepartmentLabel(department: Department) {
  return department === 'elderly-care' ? '养老部门' : '物业部门';
}
