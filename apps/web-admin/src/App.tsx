import {
  ApartmentOutlined,
  AuditOutlined,
  BellOutlined,
  DashboardOutlined,
  HomeOutlined,
  PlusOutlined,
  SearchOutlined,
  TeamOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import {
  Avatar,
  Badge,
  Button,
  ConfigProvider,
  Descriptions,
  Drawer,
  Empty,
  Flex,
  Form,
  Input,
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
import type { ColumnsType } from 'antd/es/table';
import { Building2, Download, Eye, FileEdit, Moon, ShieldCheck, Sun, UserRoundCheck } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  filterResidents,
  getAdmissionStatusLabel,
  getResidentStatusLabel,
  getRiskLevelLabel,
  ResidentView,
  residents,
} from '@erp-real/shared-domain';
import {
  getRoleDepartmentLabel,
  hasPermission,
  roleByKey,
  roleDefinitions,
  RoleKey,
} from '@erp-real/shared-domain';

const { Header, Sider, Content } = Layout;
const { Text, Title } = Typography;

const statusColor: Record<ResidentView['status'], string> = {
  Active: 'green',
  Draft: 'gold',
  Archived: 'default',
};

function App() {
  const [isDarkMode, setIsDarkMode] = useState(true);
  const [roleKey, setRoleKey] = useState<RoleKey>('social-worker-supervisor');

  useEffect(() => {
    document.body.className = isDarkMode ? 'dark-theme' : 'light-theme';
  }, [isDarkMode]);
  const [keyword, setKeyword] = useState('');
  const [facilityId, setFacilityId] = useState<string | undefined>('facility-east');
  const [selectedResident, setSelectedResident] = useState<ResidentView | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const currentRole = roleByKey[roleKey];
  const canCreate = hasPermission(roleKey, 'resident:create');
  const canUpdate = hasPermission(roleKey, 'resident:update');
  const canExport = hasPermission(roleKey, 'resident:export');

  const residentViews = useMemo(
    () => filterResidents(residents, roleKey, { keyword, facilityId }),
    [facilityId, keyword, roleKey],
  );

  const columns: ColumnsType<ResidentView> = [
    {
      title: '档案',
      dataIndex: 'name',
      width: 210,
      render: (_, resident) => (
        <Space size={12}>
          <Avatar className="resident-avatar">{resident.name.slice(0, 1)}</Avatar>
          <div>
            <Button type="link" className="table-link" onClick={() => setSelectedResident(resident)}>
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
          <Text strong>{resident.room}</Text>
          <div className="muted">{resident.bed}</div>
        </div>
      ),
    },
    {
      title: '护理等级',
      dataIndex: 'careLevel',
      width: 120,
    },
    {
      title: '风险',
      key: 'risk',
      width: 140,
      render: (_, resident) => (
        <Space direction="vertical" size={2}>
          <Tag color={resident.health.fallRiskLevel === 'high' ? 'red' : 'gold'}>
            跌倒{getRiskLevelLabel(resident.health.fallRiskLevel)}
          </Tag>
          <span className="muted">压疮{getRiskLevelLabel(resident.health.pressureSoreRiskLevel)}</span>
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
              onClick={() => setSelectedResident(resident)}
            />
          </Tooltip>
          <Tooltip title={canUpdate ? '编辑档案' : '当前角色无编辑权限'}>
            <Button
              aria-label="编辑档案"
              icon={<FileEdit size={16} />}
              disabled={!canUpdate}
              onClick={() => setEditorOpen(true)}
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
      <Layout className="app-shell">
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
              <Select<RoleKey>
                className="role-select"
                value={roleKey}
                onChange={setRoleKey}
                options={roleDefinitions.map((role) => ({
                  value: role.key,
                  label: `${role.name} · ${getRoleDepartmentLabel(role.department)}`,
                }))}
              />
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
              <Avatar className="user-avatar">{currentRole.name.slice(0, 1)}</Avatar>
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
                    { value: 'facility-east', label: '东区颐养中心' },
                    { value: 'facility-west', label: '西区颐养中心' },
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
                  onClick={() => setEditorOpen(true)}
                >
                  新建档案
                </Button>
              </Space>
            </section>

            <section className="table-band">
              <Table
                rowKey="id"
                columns={columns}
                dataSource={residentViews}
                pagination={{ pageSize: 8, showSizeChanger: false }}
                scroll={{ x: 1420 }}
                locale={{ emptyText: <Empty description="没有匹配的长者档案" /> }}
              />
            </section>
          </Content>
        </Layout>

        <ResidentDrawer resident={selectedResident} onClose={() => setSelectedResident(null)} canUpdate={canUpdate} />
        <ResidentEditor open={editorOpen} onClose={() => setEditorOpen(false)} canUpdate={canUpdate || canCreate} />
      </Layout>
    </ConfigProvider>
  );
}

function ResidentDrawer({
  resident,
  onClose,
  canUpdate,
}: {
  resident: ResidentView | null;
  onClose: () => void;
  canUpdate: boolean;
}) {
  return (
    <Drawer
      width={620}
      title={resident ? `${resident.name} · ${resident.residentNo}` : '长者档案'}
      open={Boolean(resident)}
      onClose={onClose}
      extra={
        <Button icon={<FileEdit size={16} />} disabled={!canUpdate}>
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
                    <Descriptions.Item label="常用称呼">{resident.preferredName}</Descriptions.Item>
                    <Descriptions.Item label="性别年龄">
                      {resident.gender === 'female' ? '女' : '男'} · {resident.age} 岁
                    </Descriptions.Item>
                    <Descriptions.Item label="出生日期">{resident.birthDate}</Descriptions.Item>
                    <Descriptions.Item label="证件号">{resident.maskedIdentityNo}</Descriptions.Item>
                    <Descriptions.Item label="联系电话">{resident.maskedPhone}</Descriptions.Item>
                    <Descriptions.Item label="户籍地址">{resident.householdAddress}</Descriptions.Item>
                    <Descriptions.Item label="现住址">{resident.currentAddress}</Descriptions.Item>
                    <Descriptions.Item label="房间床位">
                      {resident.room} · {resident.bed}
                    </Descriptions.Item>
                    <Descriptions.Item label="健康摘要">{resident.healthSummary}</Descriptions.Item>
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
                  <Descriptions.Item label="入住日期">{resident.admission.admissionDate}</Descriptions.Item>
                  <Descriptions.Item label="合同编号">{resident.admission.contractNo}</Descriptions.Item>
                  <Descriptions.Item label="护理区域">{resident.admission.nursingZone}</Descriptions.Item>
                  <Descriptions.Item label="护理等级">{resident.admission.careLevel}</Descriptions.Item>
                  <Descriptions.Item label="缴费方式">{resident.admission.paymentType}</Descriptions.Item>
                  <Descriptions.Item label="医保类型">{resident.admission.medicalInsuranceType}</Descriptions.Item>
                  <Descriptions.Item label="责任社工">
                    {resident.admission.responsibleSocialWorker}
                  </Descriptions.Item>
                  <Descriptions.Item label="个案经理">{resident.admission.caseManager}</Descriptions.Item>
                </Descriptions>
              ),
            },
            {
              key: 'contacts',
              label: '联系人',
              children: (
                <Descriptions column={1} bordered>
                  <Descriptions.Item label="主要联系人">{resident.primaryContact.name}</Descriptions.Item>
                  <Descriptions.Item label="关系">{resident.primaryContact.relation}</Descriptions.Item>
                  <Descriptions.Item label="电话">{resident.primaryContact.maskedPhone}</Descriptions.Item>
                  <Descriptions.Item label="地址">{resident.primaryContact.address}</Descriptions.Item>
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
                  <Descriptions.Item label="血型">{resident.health.bloodType}</Descriptions.Item>
                  <Descriptions.Item label="过敏史">
                    {resident.health.allergyHistory.join('、') || '无'}
                  </Descriptions.Item>
                  <Descriptions.Item label="慢病">{resident.health.chronicDiseases.join('、')}</Descriptions.Item>
                  <Descriptions.Item label="行动能力">{resident.health.mobilityLevel}</Descriptions.Item>
                  <Descriptions.Item label="认知状态">{resident.health.cognitiveStatus}</Descriptions.Item>
                  <Descriptions.Item label="饮食要求">{resident.health.dietRequirement}</Descriptions.Item>
                  <Descriptions.Item label="跌倒风险">
                    {getRiskLevelLabel(resident.health.fallRiskLevel)}
                  </Descriptions.Item>
                  <Descriptions.Item label="压疮风险">
                    {getRiskLevelLabel(resident.health.pressureSoreRiskLevel)}
                  </Descriptions.Item>
                  <Descriptions.Item label="应急预案">{resident.health.emergencyPlan}</Descriptions.Item>
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
                      {resident.missingFields.length > 0 ? resident.missingFields.join('、') : '无'}
                    </Descriptions.Item>
                    <Descriptions.Item label="下次跟进">{resident.nextFollowUpDate}</Descriptions.Item>
                    <Descriptions.Item label="版本">{resident.version}</Descriptions.Item>
                    <Descriptions.Item label="更新时间">{resident.updatedAt}</Descriptions.Item>
                  </Descriptions>
                </Space>
              ),
            },
            {
              key: 'timeline',
              label: '服务时间线',
              children: (
                <Timeline
                  items={[
                    {
                      color: 'green',
                      children: `${resident.lastServiceAt} 完成最近一次服务跟进`,
                    },
                    {
                      color: 'blue',
                      children: `${resident.admissionDate} 建立入住档案并绑定房间床位`,
                    },
                    {
                      color: 'gray',
                      children: '所有敏感字段查看和修改将写入审计日志',
                    },
                  ]}
                />
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
  onClose,
  canUpdate,
}: {
  open: boolean;
  onClose: () => void;
  canUpdate: boolean;
}) {
  const [form] = Form.useForm();

  return (
    <Modal
      title="长者档案"
      open={open}
      onCancel={onClose}
      onOk={() => {
        if (!canUpdate) return;
        message.success('档案已保存到前端草稿，等待后端 API 接入。');
        form.resetFields();
        onClose();
      }}
      okButtonProps={{ disabled: !canUpdate }}
      okText="保存"
      cancelText="取消"
    >
      <Form layout="vertical" form={form}>
        <Form.Item label="姓名" name="name" rules={[{ required: true, message: '请输入姓名' }]}>
          <Input placeholder="请输入长者姓名" prefix={<UserRoundCheck size={16} />} />
        </Form.Item>
        <Form.Item label="证件号" name="identityNo" rules={[{ required: true, message: '请输入证件号' }]}>
          <Input placeholder="用于防重，保存时后端需加密并生成 hash" />
        </Form.Item>
        <Form.Item label="出生日期" name="birthDate" rules={[{ required: true, message: '请输入出生日期' }]}>
          <Input placeholder="YYYY-MM-DD" />
        </Form.Item>
        <Form.Item label="房间床位" name="room" rules={[{ required: true, message: '请输入房间床位' }]}>
          <Input placeholder="例如 3F-护理一区-301 A床" prefix={<Building2 size={16} />} />
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
        <Form.Item label="健康摘要" name="healthSummary">
          <Input.TextArea rows={4} placeholder="仅填写摘要，不录入完整医疗病历" />
        </Form.Item>
        <Form.Item label="主要联系人电话" name="contactPhone">
          <Input placeholder="保存时后端需脱敏展示并加密存储" />
        </Form.Item>
      </Form>
      {!canUpdate && <Text type="secondary">当前角色只能查看档案，不能创建或修改。</Text>}
    </Modal>
  );
}

export default App;
