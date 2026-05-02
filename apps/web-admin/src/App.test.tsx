import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

const socialWorkerSupervisor = {
  userId: 'user-social-worker-supervisor',
  displayName: '社工主管',
  role: 'social-worker-supervisor',
  tenantId: 'tenant-yiyang',
  facilityId: 'facility-hecheng',
};

const propertySupervisor = {
  userId: 'user-property-supervisor',
  displayName: '物业主管',
  role: 'property-supervisor',
  tenantId: 'tenant-yiyang',
  facilityId: 'facility-hecheng',
};

function signInAs(user: typeof socialWorkerSupervisor) {
  localStorage.setItem('erp_token', 'test-token');
  localStorage.setItem('erp_user', JSON.stringify(user));
}

function mockApiFetch() {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = input.toString();
    if (url.startsWith('/api/v1/residents/resident-api-001')) {
      return jsonResponse({
        data: residentDetail(),
      });
    }
    if (url.startsWith('/api/v1/residents') && init?.method === 'POST') {
      return jsonResponse({
        data: {
          id: 'resident-created',
        },
      });
    }
    if (url.startsWith('/api/v1/residents')) {
      return jsonResponse({
        data: {
          items: [residentListItem()],
          page: 1,
          pageSize: 100,
          total: 1,
        },
      });
    }
    return jsonResponse({}, 404);
  });
}

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

function residentListItem() {
  return {
    id: 'resident-api-001',
    tenantId: 'tenant-yiyang',
    facilityId: 'facility-hecheng',
    residentNo: 'CY-2026-API-001',
    name: '后端长者',
    status: 'Active',
    maskedPhone: '138****6721',
    maskedIdentityNo: '310101********6428',
    admissionStatus: 'admitted',
    room: '和成养老 - 1栋 - 3楼 - 301号房',
    bed: 'A床',
    careLevel: '二级护理',
    fallRiskLevel: 'high',
    completenessScore: 96,
    missingFields: ['phone', 'familyContacts', 'health'],
    tags: ['重点关注'],
    version: 7,
  };
}

function residentDetail() {
  return {
    ...residentListItem(),
    departmentId: 'dept-care',
    preferredName: '后端阿姨',
    gender: 'female',
    birthDate: '1944-05-12',
    identityType: '居民身份证',
    maskedIdentityNo: '310101194405126428',
    maskedPhone: '13821886721',
    householdAddress: '上海市黄浦区',
    currentAddress: '和成养老',
    admission: {
      admissionStatus: 'admitted',
      admissionDate: '2024-09-12',
      contractNo: 'HT-2024-0912-001',
      room: '和成养老 - 1栋 - 3楼 - 301号房',
      bed: 'A床',
      nursingZone: '护理一区',
      careLevel: '二级护理',
      paymentType: '月付',
      medicalInsuranceType: '城镇职工医保',
      responsibleSocialWorkerId: 'staff-001',
    },
    familyContacts: [
      {
        id: 'contact-001',
        name: '后端家属',
        relation: '儿子',
        maskedPhone: '13917223455',
        address: '上海市浦东新区',
        isEmergency: true,
        isGuardian: true,
        canReceiveNotice: true,
        priority: 1,
      },
    ],
    primaryContact: {
      id: 'contact-001',
      name: '后端家属',
      relation: '儿子',
      maskedPhone: '13917223455',
      address: '上海市浦东新区',
      isEmergency: true,
      isGuardian: true,
      canReceiveNotice: true,
      priority: 1,
    },
    health: {
      bloodType: 'A型',
      allergyHistory: ['青霉素'],
      chronicDiseases: ['高血压'],
      mobilityLevel: '扶手杖辅助行走',
      cognitiveStatus: '轻度记忆下降',
      dietRequirement: '低盐软食',
      fallRiskLevel: 'high',
      emergencyPlan: '夜间离床触发巡查',
    },
    healthSummary: '高血压稳定。',
    careNeeds: ['夜间巡查'],
    createdByName: '建档社工',
    updatedByName: '社工主管',
    lastServiceAt: '2026-04-25T15:40:00Z',
    nextFollowUpDate: '2026-04-30',
    createdAt: '2026-04-26T10:00:00Z',
    updatedAt: '2026-04-26T12:00:00Z',
  };
}

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    mockApiFetch();
  });

  it('renders resident profile workspace from backend list API', async () => {
    signInAs(socialWorkerSupervisor);
    render(<App />);

    expect(screen.getByText('长者档案管理')).toBeInTheDocument();
    expect(await screen.findByText('后端长者')).toBeInTheDocument();
  });

  it('uses light theme by default on login and workspace screens', async () => {
    const loginRender = render(<App />);
    expect(loginRender.container.querySelector('.login-page')).toHaveClass('light-theme');
    loginRender.unmount();

    signInAs(socialWorkerSupervisor);
    const workspaceRender = render(<App />);
    await screen.findByText('后端长者');
    expect(workspaceRender.container.querySelector('.app-shell')).toHaveClass('light-theme');
  });

  it('disables create action for property supervisor role', async () => {
    signInAs(propertySupervisor);
    render(<App />);

    expect(screen.getByRole('button', { name: /新建档案/ })).toBeDisabled();
  });

  it('supports runtime light and dark theme switching', async () => {
    const user = userEvent.setup();
    signInAs(socialWorkerSupervisor);
    const { container } = render(<App />);

    expect(container.querySelector('.app-shell')).toHaveClass('light-theme');

    await user.click(screen.getByRole('button', { name: '切换主题' }));

    expect(container.querySelector('.app-shell')).toHaveClass('dark-theme');
  });

  it('creates resident profile through backend API instead of frontend draft', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.mocked(globalThis.fetch);
    signInAs(socialWorkerSupervisor);
    render(<App />);

    await screen.findByText('后端长者');
    await user.click(screen.getByRole('button', { name: /新建档案/ }));
    expect(screen.getByRole('button', { name: '下一页' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /保\s*存/ })).not.toBeInTheDocument();
    await user.type(screen.getByLabelText('档案号'), 'CY-2026-TEST-001');
    await user.type(screen.getByLabelText('姓名'), '测试长者');
    await user.type(screen.getByLabelText('证件号'), '310101194405126428');
    await waitFor(() => expect(screen.getByLabelText('出生日期')).toHaveValue('1944-05-12'));
    expect((screen.getByLabelText('年龄') as HTMLInputElement).value).toBe(String(calculateAgeForTest('1944-05-12')));
    await user.click(screen.getByRole('button', { name: '下一页' }));
    expect(await screen.findByLabelText('房间床位')).toBeInTheDocument();
    await user.type(screen.getByLabelText('房间床位'), '301');
    await user.type(screen.getByLabelText('责任社工'), '社工A');
    expect(screen.queryByLabelText('个案经理')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '下一页' }));
    expect(await screen.findByLabelText('主要联系人')).toBeInTheDocument();
    await user.type(screen.getByLabelText('主要联系人'), '测试家属');
    await user.type(screen.getByLabelText('主要联系人电话'), '13917223455');
    await user.click(screen.getByRole('button', { name: '下一页' }));
    expect(await screen.findByLabelText('健康摘要')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/residents',
      expect.objectContaining({
        method: 'POST',
      }),
    ));
    const createCall = fetchMock.mock.calls.find(([url, init]) => url.toString() === '/api/v1/residents' && init?.method === 'POST');
    expect(createCall).toBeDefined();
    const [, createRequest] = createCall!;
    const payload = JSON.parse((createRequest as RequestInit).body as string);
    expect(payload.residentNo).toBe('CY-2026-TEST-001');
    expect(payload.birthDate).toBe('1944-05-12');
    expect(payload.admission.room).toBe('301');
    expect(payload.admission.bed).toBe('');
    expect(payload.admission.responsibleSocialWorkerId).toBe('社工A');
    expect(payload.admission).not.toHaveProperty('caseManagerId');
    expect(payload.health).not.toHaveProperty('pressureSoreRiskLevel');
    expect(payload.familyContacts[0].phone).toBe('13917223455');
    await waitFor(() => expect(fetchMock.mock.calls.filter(([url]) => url.toString().startsWith('/api/v1/residents?'))).toHaveLength(2));
    expect(screen.queryByText('档案已保存到前端草稿，等待后端 API 接入。')).not.toBeInTheDocument();
  }, 10000);

  it('shows field format validation errors before creating resident profile', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.mocked(globalThis.fetch);
    signInAs(socialWorkerSupervisor);
    render(<App />);

    await screen.findByText('后端长者');
    await user.click(screen.getByRole('button', { name: /新建档案/ }));
    await user.type(screen.getByLabelText('档案号'), 'bad-no');
    await user.type(screen.getByLabelText('姓名'), '测试长者');
    await user.type(screen.getByLabelText('证件号'), '123456');
    await user.click(screen.getByRole('button', { name: '下一页' }));

    expect(await screen.findByText('档案号格式不正确')).toBeInTheDocument();
    expect(screen.getByText('证件号格式不正确')).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url, init]) => url.toString() === '/api/v1/residents' && init?.method === 'POST')).toBe(false);
  });

  it('shows visible resident list load error and retry action', async () => {
    const fetchMock = vi.mocked(globalThis.fetch);
    fetchMock.mockImplementation(async (input) => {
      const url = input.toString();
      if (url.startsWith('/api/v1/residents')) {
        return jsonResponse({ message: '数据库连接失败' }, 500);
      }
      return jsonResponse({}, 404);
    });
    signInAs(socialWorkerSupervisor);
    render(<App />);

    expect(await screen.findByText('长者档案加载失败')).toBeInTheDocument();
    expect(screen.getByText('数据库连接失败')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /重试/ })).toBeInTheDocument();
  });

  it('loads resident detail from backend when opening drawer', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.mocked(globalThis.fetch);
    signInAs(socialWorkerSupervisor);
    render(<App />);

    await user.click(await screen.findByRole('button', { name: '后端长者' }));

    expect(await screen.findByText('后端阿姨')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: '联系人' }));
    expect(screen.getByText('后端家属')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: '服务时间线' }));
    expect(screen.getByText(/2026-04-25 \d{2}:40:00 最近一次服务跟进 · 操作人：社工主管/)).toBeInTheDocument();
    expect(screen.getByText(/2026-04-26 \d{2}:00:00 档案更新 · 操作人：社工主管/)).toBeInTheDocument();
    expect(screen.getByText(/2026-04-26 \d{2}:00:00 档案创建 · 操作人：建档社工/)).toBeInTheDocument();
    expect(screen.queryByText('所有敏感字段查看和修改将写入审计日志')).not.toBeInTheDocument();
    expect(screen.queryByText(/建立入住档案并绑定房间床位/)).not.toBeInTheDocument();
    expect(screen.queryByText('个案经理')).not.toBeInTheDocument();
    expect(screen.queryByText('压疮风险')).not.toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: '完整度' }));
    expect(screen.getByText('本人电话、主要联系人、健康风险')).toBeInTheDocument();
    expect(screen.queryByText('phone')).not.toBeInTheDocument();
    expect(screen.queryByText('familyContacts')).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/residents/resident-api-001',
      expect.any(Object),
    );
  });

  it('updates resident profile from detail editor through backend API', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.mocked(globalThis.fetch);
    signInAs(socialWorkerSupervisor);
    render(<App />);

    await screen.findByText('后端长者');
    await user.click(screen.getByRole('button', { name: '编辑档案' }));
    expect(await screen.findByText('编辑长者档案')).toBeInTheDocument();
    const editorModal = document.querySelector('.resident-editor-modal') as HTMLElement;
    expect(editorModal).toBeInTheDocument();
    const editor = within(editorModal);
    expect(editor.getByRole('tab', { name: '档案摘要' })).toBeInTheDocument();
    expect(editor.getByRole('tab', { name: '入住信息' })).toBeInTheDocument();
    expect(editor.getByRole('tab', { name: '联系人' })).toBeInTheDocument();
    expect(editor.getByRole('tab', { name: '健康风险' })).toBeInTheDocument();
    expect(editorModal.closest('.ant-modal-root')?.querySelector('.ant-modal-wrap')).toHaveStyle({ zIndex: '1400' });
    await user.click(editor.getByRole('tab', { name: '健康风险' }));
    expect(editor.queryByLabelText('压疮风险')).not.toBeInTheDocument();
    await user.clear(screen.getByLabelText('健康摘要'));
    await user.type(screen.getByLabelText('健康摘要'), '编辑后的健康摘要');
    await user.click(screen.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/residents/resident-api-001',
      expect.objectContaining({
        method: 'PATCH',
      }),
    ));
    const patchCall = fetchMock.mock.calls.find(([url, init]) => url.toString() === '/api/v1/residents/resident-api-001' && init?.method === 'PATCH');
    expect(patchCall).toBeDefined();
    const [, patchRequest] = patchCall!;
    const payload = JSON.parse((patchRequest as RequestInit).body as string);
    expect(payload.version).toBe(7);
    expect(payload.healthSummary).toBe('编辑后的健康摘要');
    expect(payload.admission.responsibleSocialWorkerId).toBe('staff-001');
    expect(payload.admission).not.toHaveProperty('caseManagerId');
    expect(payload.health).not.toHaveProperty('pressureSoreRiskLevel');
    expect(payload.familyContacts[0].phone).toBe('13917223455');
  });
});

function calculateAgeForTest(birthDate: string) {
  const birthday = new Date(`${birthDate}T00:00:00`);
  const today = new Date();
  let age = today.getFullYear() - birthday.getFullYear();
  const hasBirthdayPassed =
    today.getMonth() > birthday.getMonth()
    || (today.getMonth() === birthday.getMonth() && today.getDate() >= birthday.getDate());
  return hasBirthdayPassed ? age : age - 1;
}
