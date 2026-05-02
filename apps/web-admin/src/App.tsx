import {
  ApartmentOutlined,
  AuditOutlined,
  BellOutlined,
  ReloadOutlined,
  DashboardOutlined,
  HomeOutlined,
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Checkbox,
  ConfigProvider,
  Descriptions,
  Drawer,
  Empty,
  Flex,
  Form,
  Input,
  DatePicker,
  Layout,
  Menu,
  Modal,
  Progress,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  message,
  theme as antdTheme,
} from 'antd';
import zhCN from 'antd/locale/zh_CN';
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import { Building2, Download, Eye, FileEdit, LogOut, Moon, ShieldCheck, Sun, UserRoundCheck } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  type AdmissionStatus,
  type Gender,
  getAdmissionStatusLabel,
  getResidentStatusLabel,
  getRiskLevelLabel,
  type ResidentStatus,
  type ResidentView,
  type RiskLevel,
} from '@erp-real/shared-domain';
import {
  getRoleDepartmentLabel,
  hasPermission,
  roleByKey,
  RoleKey,
} from '@erp-real/shared-domain';
import { apiFetch, isAuthenticated, getUser, logout as authLogout } from './auth';
import type { AuthUser } from './auth';
import LoginPage from './LoginPage';

const { Header, Sider, Content } = Layout;
const { Text, Title } = Typography;

const residentNoPattern = /^[A-Z]{2,10}-\d{4}-[A-Z0-9-]{3,20}$/;
const identityNoPattern = /^\d{6}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
const mainlandPhonePattern = /^1[3-9]\d{9}$/;
const riskOptions = [
  { value: 'low', label: '低' },
  { value: 'medium', label: '中' },
  { value: 'high', label: '高' },
];
const residentEditorStepKeys = ['profile', 'admission', 'contacts', 'health'] as const;
type ResidentEditorStepKey = typeof residentEditorStepKeys[number];
const residentEditorStepFields: Record<ResidentEditorStepKey, Array<keyof ResidentEditorFormValues>> = {
  profile: [
    'residentNo',
    'preferredName',
    'name',
    'gender',
    'identityNo',
    'phone',
    'birthDate',
    'tagsText',
    'householdAddress',
    'currentAddress',
  ],
  admission: [
    'admissionStatus',
    'admissionDate',
    'contractNo',
    'room',
    'nursingZone',
    'careLevel',
    'paymentType',
    'medicalInsuranceType',
    'responsibleSocialWorker',
  ],
  contacts: [
    'contactName',
    'contactRelation',
    'contactPhone',
    'contactAddress',
    'contactIsEmergency',
    'contactIsGuardian',
    'contactCanReceiveNotice',
  ],
  health: [
    'healthSummary',
    'bloodType',
    'allergyHistoryText',
    'chronicDiseasesText',
    'mobilityLevel',
    'cognitiveStatus',
    'dietRequirement',
    'fallRiskLevel',
    'careNeedsText',
    'emergencyPlan',
  ],
};

const statusColor: Record<ResidentView['status'], string> = {
  Active: 'green',
  Draft: 'gold',
  Archived: 'default',
};

function App() {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [authedUser, setAuthedUser] = useState<AuthUser | null>(() => {
    return isAuthenticated() ? getUser() : null;
  });
  const [keyword, setKeyword] = useState('');
  const [facilityId, setFacilityId] = useState<string | undefined>('facility-hecheng');
  const [residentViews, setResidentViews] = useState<ResidentView[]>([]);
  const [loadingResidents, setLoadingResidents] = useState(false);
  const [residentLoadError, setResidentLoadError] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [selectedResident, setSelectedResident] = useState<ResidentView | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingResident, setEditingResident] = useState<ResidentView | null>(null);

  useEffect(() => {
    document.body.className = isDarkMode ? 'dark-theme' : 'light-theme';
  }, [isDarkMode]);

  const handleLoginSuccess = useCallback((user: AuthUser) => {
    setAuthedUser(user);
  }, []);

  const handleLogout = useCallback(() => {
    authLogout();
    setAuthedUser(null);
    setResidentViews([]);
    setSelectedResident(null);
  }, []);

  const reloadResidents = useCallback(() => {
    setRefreshToken((value) => value + 1);
  }, []);

  const roleKey = (authedUser?.role ?? 'social-worker') as RoleKey;
  const currentRole = roleByKey[roleKey];
  const canCreate = hasPermission(roleKey, 'resident:create');
  const canUpdate = hasPermission(roleKey, 'resident:update');
  const canExport = hasPermission(roleKey, 'resident:export');

  useEffect(() => {
    if (!authedUser) {
      return;
    }
    let cancelled = false;
    const loadResidents = async () => {
      setLoadingResidents(true);
      try {
        setResidentLoadError(null);
        const params = new URLSearchParams({
          page: '1',
          pageSize: '100',
        });
        if (keyword.trim()) {
          params.set('keyword', keyword.trim());
        }
        if (facilityId) {
          params.set('facilityId', facilityId);
        }
        const response = await apiFetch(`/api/v1/residents?${params.toString()}`);
        if (!response.ok) {
          const error = await response.json().catch(() => null);
          throw new Error(error?.message || '长者档案列表加载失败');
        }
        const body: ResidentListApiResponse = await response.json();
        if (!cancelled) {
          setResidentViews(body.data.items.map((item) => residentListItemToView(item)));
        }
      } catch (error: unknown) {
        if (!cancelled) {
          setResidentViews([]);
          setResidentLoadError(error instanceof Error ? error.message : '长者档案列表加载失败');
        }
      } finally {
        if (!cancelled) {
          setLoadingResidents(false);
        }
      }
    };

    loadResidents();

    return () => {
      cancelled = true;
    };
  }, [authedUser, facilityId, keyword, refreshToken]);

  const handleOpenResident = useCallback(async (resident: ResidentView) => {
    setSelectedResident(resident);
    try {
      const response = await apiFetch(`/api/v1/residents/${resident.id}`);
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || '长者档案详情加载失败');
      }
      const body: ResidentDetailApiResponse = await response.json();
      setSelectedResident(residentDetailToView(body.data));
    } catch (error: unknown) {
      message.error(error instanceof Error ? error.message : '长者档案详情加载失败');
    }
  }, []);

  const loadResidentDetail = useCallback(async (resident: ResidentView) => {
    const response = await apiFetch(`/api/v1/residents/${resident.id}`);
    if (!response.ok) {
      const error = await response.json().catch(() => null);
      throw new Error(error?.message || '长者档案详情加载失败');
    }
    const body: ResidentDetailApiResponse = await response.json();
    return residentDetailToView(body.data);
  }, []);

  const handleEditResident = useCallback(async (resident: ResidentView) => {
    try {
      const detail = await loadResidentDetail(resident);
      setEditingResident(detail);
      setSelectedResident(detail);
      setEditorOpen(true);
    } catch (error: unknown) {
      message.error(error instanceof Error ? error.message : '长者档案详情加载失败');
    }
  }, [loadResidentDetail]);

  const handleNewResident = useCallback(() => {
    setEditingResident(null);
    setEditorOpen(true);
  }, []);

  const handleResidentSaved = useCallback((resident: ResidentView) => {
    setSelectedResident((current) => (current?.id === resident.id ? resident : current));
    reloadResidents();
  }, [reloadResidents]);

  if (!authedUser) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  const columns: ColumnsType<ResidentView> = [
    {
      title: '档案',
      dataIndex: 'name',
      width: 210,
      render: (_, resident) => (
        <Space size={12}>
          <Avatar className="resident-avatar">{resident.name.slice(0, 1)}</Avatar>
          <div>
            <Button type="link" className="table-link" onClick={() => handleOpenResident(resident)}>
              {resident.name}
            </Button>
            <div className="muted">{resident.residentNo}</div>
          </div>
        </Space>
      ),
    },
    {
      title: '入住状态',
      dataIndex: 'status',
      width: 120,
      render: (status: ResidentView['status']) => (
        <Tag color={statusColor[status]}>{getResidentStatusLabel(status)}</Tag>
      ),
    },
    {
      title: '房间床位',
      dataIndex: 'room',
      width: 190,
      render: (_, resident) => (
        <div>
          <Text strong>{displayText(resident.room)}</Text>
          <div className="muted">{displayText(resident.bed)}</div>
        </div>
      ),
    },
    {
      title: '护理等级',
      dataIndex: 'careLevel',
      width: 120,
      render: (careLevel?: string) => displayText(careLevel),
    },
    {
      title: '风险',
      key: 'risk',
      width: 140,
      render: (_, resident) => (
        <Space direction="vertical" size={2}>
          <Tag color={resident.health.fallRiskLevel === 'high' ? 'red' : resident.health.fallRiskLevel ? 'gold' : 'default'}>
            跌倒{displayRiskLevel(resident.health.fallRiskLevel)}
          </Tag>
        </Space>
      ),
    },
    {
      title: '完整度',
      dataIndex: 'completenessScore',
      width: 150,
      render: (_, resident) => (
        <div className="completeness-cell">
          <Progress
            percent={resident.completenessScore}
            size="small"
            strokeColor={resident.completenessScore >= 90 ? '#2f855a' : '#b7791f'}
          />
          {resident.missingFields.length > 0 && (
            <div className="muted">缺 {resident.missingFields.length} 项</div>
          )}
        </div>
      ),
    },
    {
      title: '社工',
      dataIndex: 'responsibleSocialWorker',
      width: 120,
    },
    {
      title: '联系人',
      dataIndex: 'primaryContact',
      width: 180,
      render: (_, resident) => (
        <div>
          <Text>{resident.primaryContact.name}</Text>
          <div className="muted">
            {resident.primaryContact.relation} · {resident.primaryContact.maskedPhone}
          </div>
        </div>
      ),
    },
    {
      title: '标签',
      dataIndex: 'tags',
      width: 230,
      render: (tags: string[]) => (
        <Space wrap size={[4, 4]}>
          {tags.map((tag) => (
            <Tag key={tag} className="soft-tag">
              {tag}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 128,
      render: (_, resident) => (
        <Space>
          <Tooltip title="查看档案">
            <Button
              aria-label="查看档案"
              icon={<Eye size={16} />}
              onClick={() => handleOpenResident(resident)}
            />
          </Tooltip>
          <Tooltip title={canUpdate ? '编辑档案' : '当前角色无编辑权限'}>
            <Button
              aria-label="编辑档案"
              icon={<FileEdit size={16} />}
              disabled={!canUpdate}
              onClick={() => handleEditResident(resident)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: isDarkMode ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
        token: {
          colorPrimary: isDarkMode ? '#00e5ff' : '#007bbb',
          colorInfo: isDarkMode ? '#00e5ff' : '#007bbb',
          colorSuccess: isDarkMode ? '#00fa9a' : '#10b981',
          colorWarning: isDarkMode ? '#ffea00' : '#f59e0b',
          colorError: isDarkMode ? '#ff0055' : '#ef4444',
          colorBgBase: isDarkMode ? '#050a14' : '#f0f4f8',
          borderRadius: 6,
          fontFamily:
            '"Rajdhani", Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        },
        components: {
          Button: {
            controlHeight: 38,
            borderRadius: 6,
          },
          Card: {
            borderRadiusLG: 8,
          },
          Table: {
            headerBg: isDarkMode ? 'rgba(0, 229, 255, 0.05)' : 'rgba(0, 123, 187, 0.05)',
            headerColor: isDarkMode ? '#00e5ff' : '#007bbb',
            rowHoverBg: isDarkMode ? 'rgba(0, 229, 255, 0.1)' : 'rgba(0, 123, 187, 0.08)',
            borderColor: isDarkMode ? 'rgba(0, 229, 255, 0.15)' : 'rgba(0, 123, 187, 0.15)',
          },
          Input: {
            colorBgContainer: isDarkMode ? 'rgba(0, 229, 255, 0.03)' : 'rgba(0, 123, 187, 0.03)',
            colorBorder: isDarkMode ? 'rgba(0, 229, 255, 0.2)' : 'rgba(0, 123, 187, 0.2)',
            activeBorderColor: isDarkMode ? '#00e5ff' : '#007bbb',
            hoverBorderColor: isDarkMode ? '#00e5ff' : '#007bbb',
          },
          Select: {
            colorBgContainer: isDarkMode ? 'rgba(0, 229, 255, 0.03)' : 'rgba(0, 123, 187, 0.03)',
            colorBorder: isDarkMode ? 'rgba(0, 229, 255, 0.2)' : 'rgba(0, 123, 187, 0.2)',
          },
          Modal: {
            contentBg: isDarkMode ? 'rgba(10, 15, 25, 0.95)' : 'rgba(255, 255, 255, 0.95)',
            headerBg: 'transparent',
          },
          Drawer: {
            colorBgElevated: isDarkMode ? 'rgba(5, 10, 20, 0.85)' : 'rgba(255, 255, 255, 0.85)',
          },
        },
      }}
    >
      <Layout className={`app-shell ${isDarkMode ? 'dark-theme' : 'light-theme'}`}>
        <Sider width={248} className="side-nav">
          <div className="brand">
            <div className="brand-mark">颐</div>
            <div>
              <Text className="brand-title">颐养 ERP</Text>
              <div className="brand-subtitle">养老 · 物业一体化</div>
            </div>
          </div>
          <Menu
            mode="inline"
            selectedKeys={['resident-profile']}
            items={[
              { key: 'dashboard', icon: <DashboardOutlined />, label: '运营驾驶舱' },
              { key: 'resident-profile', icon: <TeamOutlined />, label: '长者档案' },
              { key: 'admission', icon: <HomeOutlined />, label: '入住生活' },
              { key: 'work-order', icon: <ToolOutlined />, label: '工单中心' },
              { key: 'audit', icon: <AuditOutlined />, label: '审计合规' },
              { key: 'organization', icon: <ApartmentOutlined />, label: '组织权限' },
            ]}
          />
        </Sider>

        <Layout>
          <Header className="top-bar">
            <div className="title-group">
              <Text className="eyebrow">Web 管理端</Text>
              <Title level={3}>长者档案管理</Title>
            </div>
            <Space size={12}>
              <Tag color="cyan" className="role-tag">
                {getRoleDepartmentLabel(currentRole.department)} · {currentRole.name}
              </Tag>
              <Tooltip title="切换主题">
                <Button
                  aria-label="切换主题"
                  icon={isDarkMode ? <Sun size={16} /> : <Moon size={16} />}
                  onClick={() => setIsDarkMode(!isDarkMode)}
                />
              </Tooltip>
              <Tooltip title="通知中心">
                <Badge dot>
                  <Button aria-label="通知中心" icon={<BellOutlined />} />
                </Badge>
              </Tooltip>
              <Avatar className="user-avatar">{authedUser.displayName.slice(0, 1)}</Avatar>
              <Text className="user-display-name">{authedUser.displayName}</Text>
              <Tooltip title="退出登录">
                <Button
                  aria-label="退出登录"
                  icon={<LogOut size={16} />}
                  onClick={handleLogout}
                  danger
                />
              </Tooltip>
            </Space>
          </Header>

          <Content className="workspace">
            <section className="overview-band">
              <div className="overview-copy">
                <Text className="eyebrow">{getRoleDepartmentLabel(currentRole.department)} · {currentRole.name}</Text>
                <Title level={1}>一人一档，跨部门协同可追溯</Title>
                <Text className="overview-text">
                  当前视图按租户、机构、角色权限裁剪。敏感证件、手机号和联系人信息会随角色自动脱敏。
                </Text>
              </div>
              <div className="metric-grid">
                <Statistic title="在住长者" value={residentViews.filter((item) => item.status === 'Active').length} />
                <Statistic
                  title="平均完整率"
                  value={
                    residentViews.length
                      ? Math.round(
                        residentViews.reduce((sum, resident) => sum + resident.completenessScore, 0) /
                        residentViews.length,
                      )
                      : 0
                  }
                  suffix="%"
                />
                <Statistic
                  title="高跌倒风险"
                  value={residentViews.filter((item) => item.health.fallRiskLevel === 'high').length}
                />
                <div className="health-score">
                  <Flex justify="space-between" align="center">
                    <Text strong>审计覆盖</Text>
                    <ShieldCheck size={18} />
                  </Flex>
                  <Progress percent={100} size="small" strokeColor="#2f855a" />
                </div>
              </div>
            </section>

            <section className="toolbar-band">
              <Space size={10} wrap>
                <Input
                  className="search-input"
                  prefix={<SearchOutlined />}
                  placeholder="搜索姓名、档案号、房间、标签"
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                />
                <Select
                  className="facility-select"
                  value={facilityId}
                  onChange={setFacilityId}
                  options={[
                    { value: 'facility-hecheng', label: '和成养老' },
                  ]}
                />
              </Space>
              <Space>
                <Tooltip title={canExport ? '导出需记录原因和范围' : '当前角色无导出权限'}>
                  <Button
                    icon={<Download size={16} />}
                    disabled={!canExport}
                    onClick={() => message.info('已进入导出审批流程，需补充导出原因。')}
                  >
                    导出
                  </Button>
                </Tooltip>
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  disabled={!canCreate}
                  onClick={handleNewResident}
                >
                  新建档案
                </Button>
              </Space>
            </section>

            <section className="table-band">
              {residentLoadError && (
                <Alert
                  type="error"
                  showIcon
                  message="长者档案加载失败"
                  description={residentLoadError}
                  action={
                    <Button icon={<ReloadOutlined />} onClick={reloadResidents}>
                      重试
                    </Button>
                  }
                  style={{ marginBottom: 16 }}
                />
              )}
              <Table
                rowKey="id"
                columns={columns}
                dataSource={residentViews}
                loading={loadingResidents}
                pagination={{ pageSize: 8, showSizeChanger: false }}
                scroll={{ x: 1420 }}
                locale={{ emptyText: <Empty description="没有匹配的长者档案" /> }}
              />
            </section>
          </Content>
        </Layout>

        <ResidentDrawer
          resident={selectedResident}
          onClose={() => setSelectedResident(null)}
          canUpdate={canUpdate}
          operatorName={authedUser.displayName}
          onEdit={(resident) => handleEditResident(resident)}
        />
        <ResidentEditor
          open={editorOpen}
          resident={editingResident}
          onClose={() => {
            setEditorOpen(false);
            setEditingResident(null);
          }}
          onSaved={handleResidentSaved}
          canUpdate={canUpdate || canCreate}
        />
      </Layout>
    </ConfigProvider>
  );
}

function ResidentDrawer({
  resident,
  onClose,
  canUpdate,
  operatorName,
  onEdit,
}: {
  resident: ResidentView | null;
  onClose: () => void;
  canUpdate: boolean;
  operatorName: string;
  onEdit: (resident: ResidentView) => void;
}) {
  const timelineItems = resident ? buildTimelineItems(resident, operatorName) : [];

  return (
    <Drawer
      width={620}
      title={resident ? `${resident.name} · ${resident.residentNo}` : '长者档案'}
      open={Boolean(resident)}
      onClose={onClose}
      extra={
        <Button
          icon={<FileEdit size={16} />}
          disabled={!canUpdate || !resident}
          onClick={() => resident && onEdit(resident)}
        >
          编辑
        </Button>
      }
    >
      {resident && (
        <Tabs
          defaultActiveKey="profile"
          items={[
            {
              key: 'profile',
              label: '档案摘要',
              children: (
                <Space direction="vertical" size={20} className="drawer-stack">
                  <Descriptions column={1} size="middle" bordered>
                    <Descriptions.Item label="姓名">{resident.name}</Descriptions.Item>
                    <Descriptions.Item label="常用称呼">{displayText(resident.preferredName)}</Descriptions.Item>
                    <Descriptions.Item label="性别年龄">
                      {resident.gender === 'female' ? '女' : '男'} · {displayAge(resident.birthDate)}
                    </Descriptions.Item>
                    <Descriptions.Item label="出生日期">{displayText(resident.birthDate)}</Descriptions.Item>
                    <Descriptions.Item label="证件号">{displayText(resident.maskedIdentityNo)}</Descriptions.Item>
                    <Descriptions.Item label="联系电话">{displayText(resident.maskedPhone)}</Descriptions.Item>
                    <Descriptions.Item label="户籍地址">{displayText(resident.householdAddress)}</Descriptions.Item>
                    <Descriptions.Item label="现住址">{displayText(resident.currentAddress)}</Descriptions.Item>
                    <Descriptions.Item label="房间床位">
                      {displayText(resident.room)} · {displayText(resident.bed)}
                    </Descriptions.Item>
                    <Descriptions.Item label="健康摘要">{displayText(resident.healthSummary)}</Descriptions.Item>
                  </Descriptions>
                  <Space wrap>
                    {resident.tags.map((tag) => (
                      <Tag key={tag} color="cyan">
                        {tag}
                      </Tag>
                    ))}
                  </Space>
                </Space>
              ),
            },
            {
              key: 'admission',
              label: '入住信息',
              children: (
                <Descriptions column={1} bordered>
                  <Descriptions.Item label="入住状态">
                    {getAdmissionStatusLabel(resident.admission.admissionStatus)}
                  </Descriptions.Item>
                  <Descriptions.Item label="入住日期">{displayText(resident.admission.admissionDate)}</Descriptions.Item>
                  <Descriptions.Item label="合同编号">{displayText(resident.admission.contractNo)}</Descriptions.Item>
                  <Descriptions.Item label="护理区域">{displayText(resident.admission.nursingZone)}</Descriptions.Item>
                  <Descriptions.Item label="护理等级">{displayText(resident.admission.careLevel)}</Descriptions.Item>
                  <Descriptions.Item label="缴费方式">{displayText(resident.admission.paymentType)}</Descriptions.Item>
                  <Descriptions.Item label="医保类型">{displayText(resident.admission.medicalInsuranceType)}</Descriptions.Item>
                  <Descriptions.Item label="责任社工">
                    {displayText(resident.admission.responsibleSocialWorker)}
                  </Descriptions.Item>
                </Descriptions>
              ),
            },
            {
              key: 'contacts',
              label: '联系人',
              children: (
                <Descriptions column={1} bordered>
                  <Descriptions.Item label="主要联系人">{displayText(resident.primaryContact.name)}</Descriptions.Item>
                  <Descriptions.Item label="关系">{displayText(resident.primaryContact.relation)}</Descriptions.Item>
                  <Descriptions.Item label="电话">{displayText(resident.primaryContact.maskedPhone)}</Descriptions.Item>
                  <Descriptions.Item label="地址">{displayText(resident.primaryContact.address)}</Descriptions.Item>
                  <Descriptions.Item label="紧急联系人">
                    {resident.primaryContact.isEmergency ? '是' : '否'}
                  </Descriptions.Item>
                  <Descriptions.Item label="监护/委托人">
                    {resident.primaryContact.isGuardian ? '是' : '否'}
                  </Descriptions.Item>
                  <Descriptions.Item label="接收通知">
                    {resident.primaryContact.canReceiveNotice ? '是' : '否'}
                  </Descriptions.Item>
                </Descriptions>
              ),
            },
            {
              key: 'health',
              label: '健康风险',
              children: (
                <Descriptions column={1} bordered>
                  <Descriptions.Item label="血型">{displayText(resident.health.bloodType)}</Descriptions.Item>
                  <Descriptions.Item label="过敏史">
                    {resident.health.allergyHistory.join('、') || '未填写'}
                  </Descriptions.Item>
                  <Descriptions.Item label="慢病">{resident.health.chronicDiseases.join('、') || '未填写'}</Descriptions.Item>
                  <Descriptions.Item label="行动能力">{displayText(resident.health.mobilityLevel)}</Descriptions.Item>
                  <Descriptions.Item label="认知状态">{displayText(resident.health.cognitiveStatus)}</Descriptions.Item>
                  <Descriptions.Item label="饮食要求">{displayText(resident.health.dietRequirement)}</Descriptions.Item>
                  <Descriptions.Item label="跌倒风险">
                    {displayRiskLevel(resident.health.fallRiskLevel)}
                  </Descriptions.Item>
                  <Descriptions.Item label="应急预案">{displayText(resident.health.emergencyPlan)}</Descriptions.Item>
                </Descriptions>
              ),
            },
            {
              key: 'quality',
              label: '完整度',
              children: (
                <Space direction="vertical" size={16} className="drawer-stack">
                  <Progress percent={resident.completenessScore} />
                  <Descriptions column={1} bordered>
                    <Descriptions.Item label="待补字段">
                      {resident.missingFields.length > 0 ? displayMissingFieldNames(resident.missingFields) : '无'}
                    </Descriptions.Item>
                    <Descriptions.Item label="下次跟进">{displayDateTime(resident.nextFollowUpDate)}</Descriptions.Item>
                    <Descriptions.Item label="版本">{resident.version}</Descriptions.Item>
                    <Descriptions.Item label="更新时间">{displayDateTime(resident.updatedAt)}</Descriptions.Item>
                  </Descriptions>
                </Space>
              ),
            },
            {
              key: 'timeline',
              label: '服务时间线',
              children: (
                timelineItems.length > 0 ? <Timeline items={timelineItems} /> : <Empty description="暂无服务时间线" />
              ),
            },
          ]}
        />
      )}
    </Drawer>
  );
}

function ResidentEditor({
  open,
  resident,
  onClose,
  onSaved,
  canUpdate,
}: {
  open: boolean;
  resident: ResidentView | null;
  onClose: () => void;
  onSaved: (resident: ResidentView) => void;
  canUpdate: boolean;
}) {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [activeStep, setActiveStep] = useState<ResidentEditorStepKey>('profile');
  const identityNo = Form.useWatch('identityNo', form);
  const birthDateValue = Form.useWatch('birthDate', form);
  const calculatedAge = displayAgeValue(birthDateValue);
  const editing = Boolean(resident);
  const activeStepIndex = residentEditorStepKeys.indexOf(activeStep);
  const isLastStep = activeStepIndex === residentEditorStepKeys.length - 1;

  useEffect(() => {
    if (!open) {
      return;
    }
    setActiveStep('profile');
    if (resident) {
      form.setFieldsValue(residentToEditorValues(resident));
      return;
    }
    form.resetFields();
    form.setFieldsValue({ gender: 'female', admissionStatus: 'admitted' });
  }, [form, open, resident]);

  useEffect(() => {
    if (!open || !identityNoPattern.test(identityNo ?? '')) {
      return;
    }
    const derivedBirthDate = deriveBirthDateFromIdentityNo(identityNo);
    if (!derivedBirthDate) {
      return;
    }
    form.setFieldsValue({
      birthDate: dayjs(derivedBirthDate),
    });
  }, [form, identityNo, open]);

  const handleNextStep = async () => {
    try {
      await form.validateFields(residentEditorStepFields[activeStep]);
      setActiveStep(residentEditorStepKeys[activeStepIndex + 1]);
    } catch (error: unknown) {
      if (!isValidationError(error)) {
        message.error('当前分页校验失败，请检查后继续。');
      }
    }
  };

  const handleSave = async () => {
    if (!canUpdate) return;
    try {
      const values = await form.validateFields();
      setSaving(true);
      const response = await apiFetch(resident ? `/api/v1/residents/${resident.id}` : '/api/v1/residents', {
        method: resident ? 'PATCH' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(resident ? {} : { 'Idempotency-Key': `resident-create-${values.residentNo}` }),
        },
        body: JSON.stringify(toUpsertResidentPayload(values, resident)),
      });
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || '档案保存失败');
      }
      const body: ResidentDetailApiResponse = await response.json();
      message.success(resident ? '档案已更新。' : '档案已保存到后端。');
      form.resetFields();
      onSaved(residentDetailToView(body.data));
      onClose();
    } catch (error: unknown) {
      if (isValidationError(error)) {
        return;
      }
      message.error(error instanceof Error ? error.message : '档案保存失败');
    } finally {
      setSaving(false);
    }
  };

  const footer = [
    <Button key="cancel" onClick={onClose}>
      取消
    </Button>,
    activeStepIndex > 0 ? (
      <Button
        key="previous"
        onClick={() => setActiveStep(residentEditorStepKeys[activeStepIndex - 1])}
      >
        上一页
      </Button>
    ) : null,
    isLastStep ? (
      <Button key="save" type="primary" loading={saving} disabled={!canUpdate} onClick={handleSave}>
        保存
      </Button>
    ) : (
      <Button key="next" type="primary" disabled={!canUpdate} onClick={handleNextStep}>
        下一页
      </Button>
    ),
  ];

  return (
    <Modal
      title={editing ? '编辑长者档案' : '新建长者档案'}
      open={open}
      onCancel={onClose}
      footer={footer}
      width={820}
      className="resident-editor-modal"
      zIndex={1400}
    >
      <Form layout="vertical" form={form}>
        <Tabs
          className="resident-editor-tabs"
          activeKey={activeStep}
          onChange={(key) => setActiveStep(key as ResidentEditorStepKey)}
          items={[
            {
              key: 'profile',
              label: '档案摘要',
              forceRender: true,
              children: (
                <EditorTabPanel>
                  <Form.Item
                    label="档案号"
                    name="residentNo"
                    rules={[
                      { required: true, message: '请输入档案号' },
                      { pattern: residentNoPattern, message: '档案号格式不正确' },
                    ]}
                  >
                    <Input placeholder="例如 CY-2026-0001" />
                  </Form.Item>
                  <Form.Item label="常用称呼" name="preferredName" rules={[{ max: 40, message: '常用称呼格式不正确' }]}>
                    <Input placeholder="请输入常用称呼" />
                  </Form.Item>
                  <Form.Item
                    label="姓名"
                    name="name"
                    rules={[
                      { required: true, message: '请输入姓名' },
                      { max: 40, message: '姓名格式不正确' },
                    ]}
                  >
                    <Input placeholder="请输入长者姓名" prefix={<UserRoundCheck size={16} />} />
                  </Form.Item>
                  <Form.Item label="性别" name="gender" initialValue="female" rules={[{ required: true, message: '请选择性别' }]}>
                    <Select
                      options={[
                        { value: 'female', label: '女' },
                        { value: 'male', label: '男' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item
                    label="证件号"
                    name="identityNo"
                    rules={[
                      { required: true, message: '请输入证件号' },
                      { pattern: identityNoPattern, message: '证件号格式不正确' },
                    ]}
                  >
                    <Input placeholder="用于防重，保存时后端需加密并生成 hash" />
                  </Form.Item>
                  <Form.Item
                    label="本人电话"
                    name="phone"
                    rules={[{ pattern: mainlandPhonePattern, message: '本人电话格式不正确' }]}
                  >
                    <Input placeholder="请输入本人电话" />
                  </Form.Item>
                  <Form.Item label="出生日期" name="birthDate">
                    <DatePicker
                      placeholder="由证件号自动生成"
                      format="YYYY-MM-DD"
                      inputReadOnly
                      disabled
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                  <Form.Item label="年龄">
                    <Input aria-label="年龄" disabled value={calculatedAge} placeholder="由证件号自动生成" suffix="岁" />
                  </Form.Item>
                  <Form.Item label="标签" name="tagsText">
                    <Input placeholder="多个标签用顿号或逗号分隔" />
                  </Form.Item>
                  <Form.Item label="户籍地址" name="householdAddress" className="editor-field-wide">
                    <Input placeholder="请输入户籍地址" />
                  </Form.Item>
                  <Form.Item label="现住址" name="currentAddress" className="editor-field-wide">
                    <Input placeholder="请输入现住址" />
                  </Form.Item>
                </EditorTabPanel>
              ),
            },
            {
              key: 'admission',
              label: '入住信息',
              forceRender: true,
              children: (
                <EditorTabPanel>
                  <Form.Item label="入住状态" name="admissionStatus" rules={[{ required: true, message: '请选择入住状态' }]}>
                    <Select
                      options={[
                        { value: 'pre_admission', label: '待入住' },
                        { value: 'admitted', label: '在住' },
                        { value: 'temporarily_away', label: '临时外出' },
                        { value: 'discharged', label: '已退住' },
                      ]}
                    />
                  </Form.Item>
                  <Form.Item label="入住日期" name="admissionDate">
                    <DatePicker placeholder="请选择入住日期" format="YYYY-MM-DD" inputReadOnly style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item label="合同编号" name="contractNo">
                    <Input placeholder="请输入合同编号" />
                  </Form.Item>
                  <Form.Item
                    label="房间床位"
                    name="room"
                    rules={[
                      { required: true, message: '请输入房间床位' },
                    ]}
                  >
                    <Input placeholder="例如 3F-护理一区-301 A床" prefix={<Building2 size={16} />} />
                  </Form.Item>
                  <Form.Item label="护理区域" name="nursingZone">
                    <Input placeholder="请输入护理区域" />
                  </Form.Item>
                  <Form.Item label="护理等级" name="careLevel">
                    <Select
                      placeholder="请选择护理等级"
                      options={['自理', '三级护理', '二级护理', '一级护理', '专护'].map((value) => ({
                        value,
                        label: value,
                      }))}
                    />
                  </Form.Item>
                  <Form.Item label="缴费方式" name="paymentType">
                    <Select
                      placeholder="请选择缴费方式"
                      allowClear
                      options={['月付', '季付', '半年付', '年付'].map((value) => ({ value, label: value }))}
                    />
                  </Form.Item>
                  <Form.Item label="医保类型" name="medicalInsuranceType">
                    <Input placeholder="请输入医保类型" />
                  </Form.Item>
                  <Form.Item label="责任社工" name="responsibleSocialWorker">
                    <Input placeholder="请输入责任社工姓名或编号" />
                  </Form.Item>
                </EditorTabPanel>
              ),
            },
            {
              key: 'contacts',
              label: '联系人',
              forceRender: true,
              children: (
                <EditorTabPanel>
                  <Form.Item label="主要联系人" name="contactName">
                    <Input placeholder="请输入主要联系人姓名" />
                  </Form.Item>
                  <Form.Item label="主要联系人关系" name="contactRelation">
                    <Input placeholder="例如 儿子、女儿、配偶" />
                  </Form.Item>
                  <Form.Item
                    label="主要联系人电话"
                    name="contactPhone"
                    rules={[
                      { pattern: mainlandPhonePattern, message: '主要联系人电话格式不正确' },
                    ]}
                  >
                    <Input placeholder="保存时后端需脱敏展示并加密存储" />
                  </Form.Item>
                  <Form.Item label="主要联系人地址" name="contactAddress">
                    <Input placeholder="请输入主要联系人地址" />
                  </Form.Item>
                  <Form.Item label="联系人属性" className="editor-field-wide">
                    <Space wrap>
                      <Form.Item name="contactIsEmergency" valuePropName="checked" noStyle>
                        <Checkbox aria-label="紧急联系人" />
                      </Form.Item>
                      <Text>紧急联系人</Text>
                      <Form.Item name="contactIsGuardian" valuePropName="checked" noStyle>
                        <Checkbox aria-label="监护或委托人" />
                      </Form.Item>
                      <Text>监护/委托人</Text>
                      <Form.Item name="contactCanReceiveNotice" valuePropName="checked" noStyle>
                        <Checkbox aria-label="接收通知" />
                      </Form.Item>
                      <Text>接收通知</Text>
                    </Space>
                  </Form.Item>
                </EditorTabPanel>
              ),
            },
            {
              key: 'health',
              label: '健康风险',
              forceRender: true,
              children: (
                <EditorTabPanel>
                  <Form.Item label="健康摘要" name="healthSummary" className="editor-field-wide">
                    <Input.TextArea rows={3} placeholder="仅填写摘要，不录入完整医疗病历" />
                  </Form.Item>
                  <Form.Item label="血型" name="bloodType">
                    <Select
                      placeholder="请选择血型"
                      allowClear
                      options={['A型', 'B型', 'AB型', 'O型', '其他', '未知'].map((value) => ({ value, label: value }))}
                    />
                  </Form.Item>
                  <Form.Item label="过敏史" name="allergyHistoryText">
                    <Input placeholder="多个项目用顿号或逗号分隔" />
                  </Form.Item>
                  <Form.Item label="慢病" name="chronicDiseasesText">
                    <Input placeholder="多个项目用顿号或逗号分隔" />
                  </Form.Item>
                  <Form.Item label="行动能力" name="mobilityLevel">
                    <Input placeholder="请输入行动能力" />
                  </Form.Item>
                  <Form.Item label="认知状态" name="cognitiveStatus">
                    <Input placeholder="请输入认知状态" />
                  </Form.Item>
                  <Form.Item label="饮食要求" name="dietRequirement">
                    <Input placeholder="请输入饮食要求" />
                  </Form.Item>
                  <Form.Item label="跌倒风险" name="fallRiskLevel">
                    <Select
                      placeholder="请选择跌倒风险"
                      allowClear
                      options={riskOptions}
                    />
                  </Form.Item>
                  <Form.Item label="照护需求" name="careNeedsText">
                    <Input placeholder="多个项目用顿号或逗号分隔" />
                  </Form.Item>
                  <Form.Item label="应急预案" name="emergencyPlan" className="editor-field-wide">
                    <Input.TextArea rows={3} placeholder="请输入应急预案" />
                  </Form.Item>
                </EditorTabPanel>
              ),
            },
          ]}
        />
      </Form>
      {!canUpdate && <Text type="secondary">当前角色只能查看档案，不能创建或修改。</Text>}
    </Modal>
  );
}

function EditorTabPanel({ children }: { children: ReactNode }) {
  return <div className="resident-editor-grid">{children}</div>;
}

interface ResidentEditorFormValues {
  residentNo: string;
  name: string;
  preferredName?: string;
  gender: 'male' | 'female';
  identityNo: string;
  phone?: string;
  birthDate: Dayjs | string;
  householdAddress?: string;
  currentAddress?: string;
  admissionStatus: AdmissionStatus;
  admissionDate?: Dayjs | string;
  contractNo?: string;
  room: string;
  nursingZone?: string;
  careLevel?: string;
  paymentType?: string;
  medicalInsuranceType?: string;
  responsibleSocialWorker?: string;
  healthSummary?: string;
  bloodType?: string;
  allergyHistoryText?: string;
  chronicDiseasesText?: string;
  mobilityLevel?: string;
  cognitiveStatus?: string;
  dietRequirement?: string;
  fallRiskLevel?: RiskLevel;
  emergencyPlan?: string;
  careNeedsText?: string;
  tagsText?: string;
  contactName?: string;
  contactRelation?: string;
  contactPhone?: string;
  contactAddress?: string;
  contactIsEmergency?: boolean;
  contactIsGuardian?: boolean;
  contactCanReceiveNotice?: boolean;
}

function residentToEditorValues(resident: ResidentView): Partial<ResidentEditorFormValues> {
  return {
    residentNo: resident.residentNo,
    name: resident.name,
    preferredName: resident.preferredName,
    gender: resident.gender,
    identityNo: resident.identityNo,
    phone: resident.phone,
    birthDate: resident.birthDate ? dayjs(resident.birthDate) : undefined,
    householdAddress: resident.householdAddress,
    currentAddress: resident.currentAddress,
    admissionStatus: resident.admission.admissionStatus,
    admissionDate: resident.admission.admissionDate ? dayjs(resident.admission.admissionDate) : undefined,
    contractNo: resident.admission.contractNo,
    room: [resident.admission.room, resident.admission.bed].filter(Boolean).join(' '),
    nursingZone: resident.admission.nursingZone,
    careLevel: resident.admission.careLevel,
    paymentType: resident.admission.paymentType,
    medicalInsuranceType: resident.admission.medicalInsuranceType,
    responsibleSocialWorker: resident.admission.responsibleSocialWorker,
    healthSummary: resident.healthSummary,
    bloodType: resident.health.bloodType,
    allergyHistoryText: resident.health.allergyHistory.join('、'),
    chronicDiseasesText: resident.health.chronicDiseases.join('、'),
    mobilityLevel: resident.health.mobilityLevel,
    cognitiveStatus: resident.health.cognitiveStatus,
    dietRequirement: resident.health.dietRequirement,
    fallRiskLevel: normalizeRiskLevel(resident.health.fallRiskLevel),
    emergencyPlan: resident.health.emergencyPlan,
    careNeedsText: resident.careNeeds.join('、'),
    tagsText: resident.tags.join('、'),
    contactName: resident.primaryContact.name,
    contactRelation: resident.primaryContact.relation,
    contactPhone: resident.primaryContact.phone || resident.primaryContact.maskedPhone,
    contactAddress: resident.primaryContact.address,
    contactIsEmergency: resident.primaryContact.isEmergency,
    contactIsGuardian: resident.primaryContact.isGuardian,
    contactCanReceiveNotice: resident.primaryContact.canReceiveNotice,
  };
}

function toUpsertResidentPayload(values: ResidentEditorFormValues, resident: ResidentView | null) {
  const contactFilled = values.contactName && values.contactPhone;
  const birthDate = values.birthDate ? formatDateValue(values.birthDate) : deriveBirthDateFromIdentityNo(values.identityNo);
  return {
    residentNo: values.residentNo,
    name: values.name,
    preferredName: values.preferredName || values.name,
    gender: values.gender,
    birthDate,
    identityType: '居民身份证',
    identityNo: values.identityNo,
    phone: values.phone || undefined,
    householdAddress: values.householdAddress || undefined,
    currentAddress: values.currentAddress || undefined,
    admission: {
      admissionStatus: values.admissionStatus,
      admissionDate: values.admissionDate ? formatDateValue(values.admissionDate) : undefined,
      contractNo: values.contractNo || undefined,
      room: extractRoomLabel(values.room),
      bed: extractBedLabel(values.room),
      careLevel: values.careLevel,
      nursingZone: values.nursingZone || undefined,
      paymentType: values.paymentType || undefined,
      medicalInsuranceType: values.medicalInsuranceType || undefined,
      responsibleSocialWorkerId: values.responsibleSocialWorker || undefined,
    },
    familyContacts:
      contactFilled
        ? [
          {
            name: values.contactName,
            relation: values.contactRelation || '家属',
            phone: values.contactPhone,
            address: values.contactAddress || undefined,
            isEmergency: Boolean(values.contactIsEmergency),
            isGuardian: Boolean(values.contactIsGuardian),
            canReceiveNotice: Boolean(values.contactCanReceiveNotice),
            priority: 1,
          },
        ]
        : [],
    health: {
      bloodType: values.bloodType || undefined,
      allergyHistory: splitListText(values.allergyHistoryText),
      chronicDiseases: splitListText(values.chronicDiseasesText),
      mobilityLevel: values.mobilityLevel || undefined,
      cognitiveStatus: values.cognitiveStatus || undefined,
      dietRequirement: values.dietRequirement || undefined,
      fallRiskLevel: values.fallRiskLevel || undefined,
      emergencyPlan: values.emergencyPlan || undefined,
    },
    healthSummary: values.healthSummary,
    careNeeds: splitListText(values.careNeedsText),
    tags: splitListText(values.tagsText),
    version: resident?.version,
  };
}

function extractBedLabel(room: string) {
  const match = room.match(/([A-ZＡ-Ｚ甲乙丙丁一二三四五六七八九十0-9]+床)$/u);
  return match?.[1] ?? '';
}

function extractRoomLabel(room: string) {
  return room.replace(/\s*[A-ZＡ-Ｚ甲乙丙丁一二三四五六七八九十0-9]+床$/u, '').trim();
}

function formatDateValue(value: Dayjs | string) {
  return typeof value === 'string' ? value : value.format('YYYY-MM-DD');
}

function deriveBirthDateFromIdentityNo(identityNo?: string) {
  if (!identityNoPattern.test(identityNo ?? '')) {
    return undefined;
  }
  const rawDate = identityNo!.slice(6, 14);
  const birthDate = `${rawDate.slice(0, 4)}-${rawDate.slice(4, 6)}-${rawDate.slice(6, 8)}`;
  return dayjs(birthDate).isValid() ? birthDate : undefined;
}

function splitListText(value?: string) {
  return (value ?? '')
    .split(/[、,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function displayText(value?: string | null) {
  return value && value.trim() ? value : '未填写';
}

function displayAge(birthDate?: string | null) {
  const age = calculateAge(birthDate ?? '');
  return age > 0 ? `${age} 岁` : '未填写';
}

function displayAgeValue(value?: Dayjs | string | null) {
  if (!value) {
    return '';
  }
  const birthDate = typeof value === 'string' ? value : value.format('YYYY-MM-DD');
  const age = calculateAge(birthDate);
  return age > 0 ? String(age) : '';
}

function normalizeRiskLevel(value?: string): RiskLevel | undefined {
  return value === 'low' || value === 'medium' || value === 'high' ? value : undefined;
}

function displayRiskLevel(value?: string) {
  const level = normalizeRiskLevel(value);
  return level ? getRiskLevelLabel(level) : '未填写';
}

const missingFieldLabels: Record<string, string> = {
  phone: '本人电话',
  familyContacts: '主要联系人',
  health: '健康风险',
  residentNo: '档案号',
  name: '姓名',
  preferredName: '常用称呼',
  gender: '性别',
  birthDate: '出生日期',
  identityNo: '证件号',
  householdAddress: '户籍地址',
  currentAddress: '现住址',
  admission: '入住信息',
  admissionStatus: '入住状态',
  admissionDate: '入住日期',
  contractNo: '合同编号',
  room: '房间',
  bed: '床位',
  careLevel: '护理等级',
  healthSummary: '健康摘要',
  fallRiskLevel: '跌倒风险',
  emergencyPlan: '应急预案',
  tags: '标签',
};

function displayMissingFieldNames(fields: string[]) {
  return fields.map((field) => missingFieldLabels[field] ?? field).join('、');
}

function displayDateTime(value?: string | null) {
  if (!value) {
    return '未填写';
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value;
}

function displayOperatorName(value?: string, fallback?: string) {
  return displayText(value || fallback);
}

function timelineText(date: string, action: string, operatorName?: string) {
  return `${displayDateTime(date)} ${action} · 操作人：${displayOperatorName(operatorName)}`;
}

function buildTimelineItems(resident: ResidentView, currentOperatorName?: string) {
  return [
    resident.lastServiceAt
      ? {
        color: 'green',
        children: timelineText(
          resident.lastServiceAt,
          '最近一次服务跟进',
          resident.lastServiceOperatorName || resident.updatedByName || currentOperatorName,
        ),
      }
      : null,
    resident.nextFollowUpDate
      ? {
        color: 'blue',
        children: timelineText(
          resident.nextFollowUpDate,
          '下次跟进',
          resident.nextFollowUpOperatorName || resident.updatedByName || currentOperatorName,
        ),
      }
      : null,
    resident.updatedAt
      ? {
        color: 'gray',
        children: timelineText(resident.updatedAt, '档案更新', resident.updatedByName || currentOperatorName),
      }
      : null,
    resident.createdAt
      ? {
        color: 'gray',
        children: timelineText(resident.createdAt, '档案创建', resident.createdByName || currentOperatorName),
      }
      : null,
  ].filter((item): item is { color: string; children: string } => Boolean(item));
}

function isValidationError(error: unknown) {
  return Boolean(error && typeof error === 'object' && 'errorFields' in error);
}

interface ApiEnvelope<T> {
  data: T;
}

interface ResidentListApiResponse extends ApiEnvelope<{
  items: ResidentListApiItem[];
  page: number;
  pageSize: number;
  total: number;
}> {}

interface ResidentDetailApiResponse extends ApiEnvelope<ResidentDetailApiItem> {}

interface ResidentListApiItem {
  id: string;
  tenantId: string;
  facilityId: string;
  residentNo: string;
  name: string;
  status: ResidentStatus;
  maskedPhone?: string;
  maskedIdentityNo?: string;
  admissionStatus?: AdmissionStatus;
  room?: string;
  bed?: string;
  careLevel?: string;
  fallRiskLevel?: RiskLevel;
  completenessScore?: number;
  missingFields?: string[];
  tags?: string[];
  version: number;
}

interface ResidentDetailApiItem {
  id: string;
  tenantId: string;
  facilityId: string;
  departmentId?: string;
  residentNo: string;
  name: string;
  preferredName?: string;
  gender: Gender;
  birthDate: string;
  identityType?: string;
  maskedIdentityNo?: string;
  maskedPhone?: string;
  householdAddress?: string;
  currentAddress?: string;
  status: ResidentStatus;
  admission?: {
    admissionStatus?: AdmissionStatus;
    admissionDate?: string;
    contractNo?: string;
    zoneId?: string;
    buildingId?: string;
    floorId?: string;
    roomId?: string;
    room?: string;
    bed?: string;
    nursingZone?: string;
    careLevel?: string;
    paymentType?: string;
    medicalInsuranceType?: string;
    responsibleSocialWorkerId?: string;
  };
  familyContacts?: Array<{
    id: string;
    name: string;
    relation: string;
    maskedPhone?: string;
    address?: string;
    isEmergency: boolean;
    isGuardian: boolean;
    canReceiveNotice: boolean;
    priority: number;
  }>;
  primaryContact?: {
    id: string;
    name: string;
    relation: string;
    maskedPhone?: string;
    address?: string;
    isEmergency: boolean;
    isGuardian: boolean;
    canReceiveNotice: boolean;
    priority: number;
  };
  health?: {
    bloodType?: string;
    allergyHistory?: string[];
    chronicDiseases?: string[];
    mobilityLevel?: string;
    cognitiveStatus?: string;
    dietRequirement?: string;
    fallRiskLevel?: RiskLevel;
    emergencyPlan?: string;
  };
  healthSummary?: string;
  careNeeds?: string[];
  tags?: string[];
  completenessScore?: number;
  missingFields?: string[];
  lastServiceOperatorName?: string;
  nextFollowUpOperatorName?: string;
  createdByName?: string;
  updatedByName?: string;
  lastServiceAt?: string;
  nextFollowUpDate?: string;
  version: number;
  createdAt?: string;
  updatedAt?: string;
}

function residentListItemToView(item: ResidentListApiItem): ResidentView {
  return {
    id: item.id,
    tenantId: item.tenantId,
    facilityId: item.facilityId,
    departmentId: '',
    residentNo: item.residentNo,
    name: item.name,
    preferredName: item.name,
    gender: 'female',
    birthDate: '',
    identityType: '',
    identityNo: item.maskedIdentityNo ?? '',
    identityNoHash: '',
    nationality: '',
    ethnicity: '',
    maritalStatus: '',
    formerOccupation: '',
    phone: item.maskedPhone ?? '',
    householdAddress: '',
    currentAddress: '',
    status: item.status,
    admission: {
      admissionStatus: item.admissionStatus ?? 'pre_admission',
      admissionDate: '',
      contractNo: '',
      zoneId: '',
      buildingId: '',
      floorId: '',
      roomId: '',
      room: item.room ?? '',
      bed: item.bed ?? '',
      nursingZone: '',
      careLevel: item.careLevel ?? '',
      paymentType: '',
      medicalInsuranceType: '',
      responsibleSocialWorker: '',
    },
    health: {
      bloodType: '',
      allergyHistory: [],
      chronicDiseases: [],
      mobilityLevel: '',
      cognitiveStatus: '',
      dietRequirement: '',
      fallRiskLevel: normalizeRiskLevel(item.fallRiskLevel) ?? ('' as RiskLevel),
      emergencyPlan: '',
      lastAssessmentAt: '',
    },
    healthSummary: '',
    careNeeds: [],
    tags: item.tags ?? [],
    familyContacts: [emptyContact()],
    lastServiceAt: '',
    nextFollowUpDate: '',
    completenessScore: Number(item.completenessScore ?? 0),
    missingFields: item.missingFields ?? [],
    lastServiceOperatorName: '',
    nextFollowUpOperatorName: '',
    createdByName: '',
    updatedByName: '',
    createdAt: '',
    updatedAt: '',
    version: item.version,
    maskedIdentityNo: item.maskedIdentityNo ?? '',
    maskedPhone: item.maskedPhone ?? '',
    primaryContact: {
      ...emptyContact(),
      maskedPhone: '',
    },
    room: item.room ?? '',
    bed: item.bed ?? '',
    careLevel: item.careLevel ?? '',
    responsibleSocialWorker: '',
    admissionDate: '',
  };
}

function residentDetailToView(item: ResidentDetailApiItem): ResidentView {
  const admission = item.admission ?? {};
  const primaryContact = item.primaryContact ?? item.familyContacts?.[0] ?? emptyContact();

  return {
    id: item.id,
    tenantId: item.tenantId,
    facilityId: item.facilityId,
    departmentId: item.departmentId ?? '',
    residentNo: item.residentNo,
    name: item.name,
    preferredName: item.preferredName ?? item.name,
    gender: item.gender,
    birthDate: item.birthDate,
    identityType: item.identityType ?? '',
    identityNo: item.maskedIdentityNo ?? '',
    identityNoHash: '',
    nationality: '',
    ethnicity: '',
    maritalStatus: '',
    formerOccupation: '',
    phone: item.maskedPhone ?? '',
    householdAddress: item.householdAddress ?? '',
    currentAddress: item.currentAddress ?? '',
    status: item.status,
    admission: {
      admissionStatus: admission.admissionStatus ?? 'pre_admission',
      admissionDate: admission.admissionDate ?? '',
      contractNo: admission.contractNo ?? '',
      zoneId: admission.zoneId ?? '',
      buildingId: admission.buildingId ?? '',
      floorId: admission.floorId ?? '',
      roomId: admission.roomId ?? '',
      room: admission.room ?? '',
      bed: admission.bed ?? '',
      nursingZone: admission.nursingZone ?? '',
      careLevel: admission.careLevel ?? '',
      paymentType: admission.paymentType ?? '',
      medicalInsuranceType: admission.medicalInsuranceType ?? '',
      responsibleSocialWorker: admission.responsibleSocialWorkerId ?? '',
    },
    health: {
      bloodType: item.health?.bloodType ?? '',
      allergyHistory: item.health?.allergyHistory ?? [],
      chronicDiseases: item.health?.chronicDiseases ?? [],
      mobilityLevel: item.health?.mobilityLevel ?? '',
      cognitiveStatus: item.health?.cognitiveStatus ?? '',
      dietRequirement: item.health?.dietRequirement ?? '',
      fallRiskLevel: normalizeRiskLevel(item.health?.fallRiskLevel) ?? ('' as RiskLevel),
      emergencyPlan: item.health?.emergencyPlan ?? '',
      lastAssessmentAt: '',
    },
    healthSummary: item.healthSummary ?? '',
    careNeeds: item.careNeeds ?? [],
    tags: item.tags ?? [],
    familyContacts: (item.familyContacts ?? []).map(apiContactToViewContact),
    lastServiceAt: item.lastServiceAt ?? '',
    nextFollowUpDate: item.nextFollowUpDate ?? '',
    completenessScore: Number(item.completenessScore ?? 0),
    missingFields: item.missingFields ?? [],
    lastServiceOperatorName: item.lastServiceOperatorName ?? '',
    nextFollowUpOperatorName: item.nextFollowUpOperatorName ?? '',
    createdByName: item.createdByName ?? '',
    updatedByName: item.updatedByName ?? '',
    createdAt: item.createdAt ?? '',
    updatedAt: item.updatedAt ?? '',
    version: item.version,
    maskedIdentityNo: item.maskedIdentityNo ?? '',
    maskedPhone: item.maskedPhone ?? '',
    primaryContact: apiContactToViewContact(primaryContact),
    room: admission.room ?? '',
    bed: admission.bed ?? '',
    careLevel: admission.careLevel ?? '',
    responsibleSocialWorker: admission.responsibleSocialWorkerId ?? '',
    admissionDate: admission.admissionDate ?? '',
  };
}

function apiContactToViewContact(contact: ResidentDetailApiItem['primaryContact']) {
  const fallback = emptyContact();
  return {
    id: contact?.id ?? fallback.id,
    name: contact?.name ?? fallback.name,
    relation: contact?.relation ?? fallback.relation,
    phone: contact?.maskedPhone ?? fallback.phone,
    maskedPhone: contact?.maskedPhone ?? '',
    address: contact?.address ?? '',
    priority: contact?.priority ?? 1,
    isEmergency: contact?.isEmergency ?? false,
    isGuardian: contact?.isGuardian ?? false,
    canReceiveNotice: contact?.canReceiveNotice ?? false,
  };
}

function emptyContact() {
  return {
    id: '',
    name: '',
    relation: '',
    phone: '',
    address: '',
    priority: 1,
    isEmergency: false,
    isGuardian: false,
    canReceiveNotice: false,
  };
}

function calculateAge(birthDate: string) {
  if (!birthDate) {
    return 0;
  }
  const birthday = new Date(`${birthDate}T00:00:00`);
  if (Number.isNaN(birthday.getTime())) {
    return 0;
  }
  const today = new Date();
  let age = today.getFullYear() - birthday.getFullYear();
  const hasBirthdayPassed =
    today.getMonth() > birthday.getMonth()
    || (today.getMonth() === birthday.getMonth() && today.getDate() >= birthday.getDate());
  if (!hasBirthdayPassed) {
    age -= 1;
  }
  return age;
}

export default App;
