import { hasPermission, RoleKey } from './roles';
import { getRoomLocationLabel, HECHENG_ZONE_ID, roomCatalog } from './rooms';

export type ResidentStatus = 'Draft' | 'Active' | 'Archived';
export type Gender = 'male' | 'female';
export type AdmissionStatus = 'pre_admission' | 'admitted' | 'temporarily_away' | 'discharged';
export type RiskLevel = 'low' | 'medium' | 'high';

export interface FamilyContact {
  id: string;
  name: string;
  relation: string;
  phone: string;
  address: string;
  priority: number;
  isEmergency: boolean;
  isGuardian: boolean;
  canReceiveNotice: boolean;
}

export interface ResidentHealthProfile {
  bloodType: string;
  allergyHistory: string[];
  chronicDiseases: string[];
  mobilityLevel: string;
  cognitiveStatus: string;
  dietRequirement: string;
  fallRiskLevel: RiskLevel;
  emergencyPlan: string;
  lastAssessmentAt: string;
}

export interface ResidentAdmissionProfile {
  admissionStatus: AdmissionStatus;
  admissionDate: string;
  contractNo: string;
  zoneId: string;
  buildingId: string;
  floorId: string;
  roomId: string;
  room: string;
  bed: string;
  nursingZone: string;
  careLevel: string;
  paymentType: string;
  medicalInsuranceType: string;
  responsibleSocialWorker: string;
}

export interface Resident {
  id: string;
  tenantId: string;
  facilityId: string;
  departmentId: string;
  residentNo: string;
  name: string;
  preferredName: string;
  gender: Gender;
  birthDate: string;
  identityType: string;
  identityNo: string;
  identityNoHash: string;
  nationality: string;
  ethnicity: string;
  maritalStatus: string;
  formerOccupation: string;
  phone: string;
  householdAddress: string;
  currentAddress: string;
  status: ResidentStatus;
  admission: ResidentAdmissionProfile;
  health: ResidentHealthProfile;
  healthSummary: string;
  careNeeds: string[];
  tags: string[];
  familyContacts: FamilyContact[];
  lastServiceAt: string;
  nextFollowUpDate: string;
  completenessScore: number;
  missingFields: string[];
  lastServiceOperatorName?: string;
  nextFollowUpOperatorName?: string;
  createdByName?: string;
  updatedByName?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ResidentView extends Resident {
  maskedIdentityNo: string;
  maskedPhone: string;
  primaryContact: FamilyContact & { maskedPhone: string };
  room: string;
  bed: string;
  careLevel: string;
  responsibleSocialWorker: string;
  admissionDate: string;
}

export const residents: Resident[] = [
  {
    id: 'res-001',
    tenantId: 'tenant-yiyang',
    facilityId: 'facility-hecheng',
    departmentId: 'dept-care-a',
    residentNo: 'CY-2026-0001',
    name: '陈兰英',
    preferredName: '陈阿姨',
    gender: 'female',
    birthDate: '1944-05-12',
    identityType: '居民身份证',
    identityNo: '310101194405126428',
    identityNoHash: 'hash-id-001',
    nationality: '中国',
    ethnicity: '汉族',
    maritalStatus: '丧偶',
    formerOccupation: '退休教师',
    phone: '13821886721',
    householdAddress: '上海市黄浦区外滩街道',
    currentAddress: '和成养老 1栋 3楼',
    status: 'Active',
    admission: {
      admissionStatus: 'admitted',
      admissionDate: '2024-09-12',
      contractNo: 'HT-2024-0912-001',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-1',
      floorId: 'floor-1-3',
      roomId: 'room-1-3-301',
      room: getRoomLocationLabel(roomCatalog, 'room-1-3-301'),
      bed: 'A床',
      nursingZone: '护理一区',
      careLevel: '二级护理',
      paymentType: '月付',
      medicalInsuranceType: '城镇职工医保',
      responsibleSocialWorker: '林亦辰',
    },
    health: {
      bloodType: 'A型',
      allergyHistory: ['青霉素'],
      chronicDiseases: ['高血压', '骨质疏松'],
      mobilityLevel: '扶手杖辅助行走',
      cognitiveStatus: '轻度记忆下降',
      dietRequirement: '低盐软食',
      fallRiskLevel: 'high',
      emergencyPlan: '夜间离床触发巡查，跌倒后联系家属并同步护理主管。',
      lastAssessmentAt: '2026-04-18',
    },
    healthSummary: '高血压稳定，需关注夜间睡眠和跌倒风险。',
    careNeeds: ['夜间巡查', '跌倒预防', '慢病随访'],
    tags: ['重点关注', '慢病', '独居家属少'],
    familyContacts: [
      {
        id: 'fc-001',
        name: '陈思远',
        relation: '儿子',
        phone: '13917223455',
        address: '上海市浦东新区陆家嘴街道',
        priority: 1,
        isEmergency: true,
        isGuardian: true,
        canReceiveNotice: true,
      },
    ],
    lastServiceAt: '2026-04-25 15:40',
    nextFollowUpDate: '2026-04-30',
    completenessScore: 96,
    missingFields: [],
    createdAt: '2024-09-12 10:12',
    updatedAt: '2026-04-25 15:40',
    version: 7,
  },
  {
    id: 'res-002',
    tenantId: 'tenant-yiyang',
    facilityId: 'facility-hecheng',
    departmentId: 'dept-care-b',
    residentNo: 'CY-2026-0018',
    name: '周建国',
    preferredName: '周叔',
    gender: 'male',
    birthDate: '1949-12-08',
    identityType: '居民身份证',
    identityNo: '330102194912085213',
    identityNoHash: 'hash-id-002',
    nationality: '中国',
    ethnicity: '汉族',
    maritalStatus: '已婚',
    formerOccupation: '工程师',
    phone: '13611990243',
    householdAddress: '浙江省杭州市上城区',
    currentAddress: '和成养老 1栋 5楼',
    status: 'Active',
    admission: {
      admissionStatus: 'admitted',
      admissionDate: '2025-03-04',
      contractNo: 'HT-2025-0304-018',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-1',
      floorId: 'floor-1-5',
      roomId: 'room-1-5-512',
      room: getRoomLocationLabel(roomCatalog, 'room-1-5-512'),
      bed: 'B床',
      nursingZone: '自理区',
      careLevel: '三级护理',
      paymentType: '季付',
      medicalInsuranceType: '异地医保备案',
      responsibleSocialWorker: '顾文静',
    },
    health: {
      bloodType: 'O型',
      allergyHistory: [],
      chronicDiseases: ['糖尿病'],
      mobilityLevel: '自主行走',
      cognitiveStatus: '正常',
      dietRequirement: '控糖饮食',
      fallRiskLevel: 'medium',
      emergencyPlan: '低血糖时通知护理站并联系女儿。',
      lastAssessmentAt: '2026-04-10',
    },
    healthSummary: '术后康复良好，每周三复查血糖记录。',
    careNeeds: ['康复训练', '血糖记录'],
    tags: ['康复期', '活动积极'],
    familyContacts: [
      {
        id: 'fc-002',
        name: '周明',
        relation: '女儿',
        phone: '13764668892',
        address: '上海市徐汇区漕河泾街道',
        priority: 1,
        isEmergency: true,
        isGuardian: false,
        canReceiveNotice: true,
      },
    ],
    lastServiceAt: '2026-04-26 09:05',
    nextFollowUpDate: '2026-05-03',
    completenessScore: 91,
    missingFields: ['授权委托书附件'],
    createdAt: '2025-03-04 09:31',
    updatedAt: '2026-04-26 09:05',
    version: 3,
  },
  {
    id: 'res-003',
    tenantId: 'tenant-yiyang',
    facilityId: 'facility-hecheng',
    departmentId: 'dept-care-a',
    residentNo: 'CY-2025-0211',
    name: '黄秀珍',
    preferredName: '黄奶奶',
    gender: 'female',
    birthDate: '1937-06-01',
    identityType: '居民身份证',
    identityNo: '320105193706014026',
    identityNoHash: 'hash-id-003',
    nationality: '中国',
    ethnicity: '汉族',
    maritalStatus: '丧偶',
    formerOccupation: '会计',
    phone: '13588017726',
    householdAddress: '江苏省南京市建邺区',
    currentAddress: '待入住评估',
    status: 'Draft',
    admission: {
      admissionStatus: 'pre_admission',
      admissionDate: '2026-04-28',
      contractNo: '待签约',
      zoneId: HECHENG_ZONE_ID,
      buildingId: 'building-2',
      floorId: 'floor-2-2',
      roomId: 'room-2-2-218',
      room: '待分配',
      bed: '-',
      nursingZone: '待评估',
      careLevel: '评估中',
      paymentType: '待确认',
      medicalInsuranceType: '待确认',
      responsibleSocialWorker: '林亦辰',
    },
    health: {
      bloodType: '待补充',
      allergyHistory: ['待补充'],
      chronicDiseases: ['待补充'],
      mobilityLevel: '待评估',
      cognitiveStatus: '待评估',
      dietRequirement: '待评估',
      fallRiskLevel: 'medium',
      emergencyPlan: '入院评估完成后生成。',
      lastAssessmentAt: '待评估',
    },
    healthSummary: '入院评估待补充，家属已提交基础病史摘要。',
    careNeeds: ['入院评估', '合同确认', '床位分配'],
    tags: ['待评估', '新入住'],
    familyContacts: [
      {
        id: 'fc-003',
        name: '黄佳宁',
        relation: '孙女',
        phone: '13800185677',
        address: '上海市长宁区新华路街道',
        priority: 1,
        isEmergency: true,
        isGuardian: true,
        canReceiveNotice: true,
      },
    ],
    lastServiceAt: '2026-04-26 11:20',
    nextFollowUpDate: '2026-04-27',
    completenessScore: 62,
    missingFields: ['健康评估', '床位绑定', '合同编号', '医保类型'],
    createdAt: '2026-04-26 11:20',
    updatedAt: '2026-04-26 11:20',
    version: 1,
  },
];

export function maskPhone(phone: string) {
  return phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2');
}

export function maskIdentityNo(identityNo: string) {
  return identityNo.replace(/^(.{6}).+(.{4})$/, '$1********$2');
}

export function toResidentView(resident: Resident, roleKey: RoleKey): ResidentView {
  const canReadSensitive = hasPermission(roleKey, 'resident:sensitive:read');
  const primaryContact = resident.familyContacts[0];

  return {
    ...resident,
    maskedIdentityNo: canReadSensitive ? resident.identityNo : maskIdentityNo(resident.identityNo),
    maskedPhone: canReadSensitive ? resident.phone : maskPhone(resident.phone),
    room: resident.admission.room,
    bed: resident.admission.bed,
    careLevel: resident.admission.careLevel,
    responsibleSocialWorker: resident.admission.responsibleSocialWorker,
    admissionDate: resident.admission.admissionDate,
    primaryContact: {
      ...primaryContact,
      maskedPhone: canReadSensitive ? primaryContact.phone : maskPhone(primaryContact.phone),
    },
  };
}

export function filterResidents(
  source: Resident[],
  roleKey: RoleKey,
  options: { keyword?: string; facilityId?: string; departmentId?: string } = {},
) {
  const keyword = options.keyword?.trim().toLowerCase();

  return source
    .filter((resident) => {
      if (options.facilityId && resident.facilityId !== options.facilityId) return false;
      if (options.departmentId && resident.departmentId !== options.departmentId) return false;
      if (!keyword) return true;

      return [
        resident.name,
        resident.residentNo,
        resident.admission.room,
        resident.admission.responsibleSocialWorker,
        resident.admission.contractNo,
        resident.admission.nursingZone,
        resident.health.chronicDiseases.join(' '),
        resident.careNeeds.join(' '),
        resident.tags.join(' '),
      ]
        .join(' ')
        .toLowerCase()
        .includes(keyword);
    })
    .map((resident) => toResidentView(resident, roleKey));
}

export function getRiskLevelLabel(level: RiskLevel) {
  const labels: Record<RiskLevel, string> = {
    low: '低',
    medium: '中',
    high: '高',
  };

  return labels[level];
}

export function getAdmissionStatusLabel(status: AdmissionStatus) {
  const labels: Record<AdmissionStatus, string> = {
    pre_admission: '待入住',
    admitted: '在住',
    temporarily_away: '暂离',
    discharged: '退住',
  };

  return labels[status];
}

export function getResidentStatusLabel(status: ResidentStatus) {
  const labels: Record<ResidentStatus, string> = {
    Draft: '草稿',
    Active: '在住',
    Archived: '已归档',
  };

  return labels[status];
}
