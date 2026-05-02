import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, ConfigProvider, Form, Input, Typography, message, theme as antdTheme } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { Moon, Sun } from 'lucide-react';
import { useState } from 'react';
import { login } from './auth';
import type { AuthUser } from './auth';

const { Text, Title } = Typography;

interface LoginPageProps {
    onLoginSuccess: (user: AuthUser) => void;
}

export default function LoginPage({ onLoginSuccess }: LoginPageProps) {
    const [isDarkMode, setIsDarkMode] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleLogin = async (values: { username: string; password: string }) => {
        setLoading(true);
        try {
            const user = await login(values.username, values.password);
            message.success(`欢迎回来，${user.displayName}`);
            onLoginSuccess(user);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : '登录失败，请重试';
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <ConfigProvider
            locale={zhCN}
            theme={{
                algorithm: isDarkMode ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
                token: {
                    colorPrimary: isDarkMode ? '#00e5ff' : '#007bbb',
                    colorBgBase: isDarkMode ? '#050a14' : '#f0f4f8',
                    borderRadius: 6,
                    fontFamily:
                        '"Rajdhani", Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
                },
                components: {
                    Input: {
                        colorBgContainer: isDarkMode ? 'rgba(0, 229, 255, 0.03)' : 'rgba(0, 123, 187, 0.03)',
                        colorBorder: isDarkMode ? 'rgba(0, 229, 255, 0.2)' : 'rgba(0, 123, 187, 0.2)',
                        activeBorderColor: isDarkMode ? '#00e5ff' : '#007bbb',
                        hoverBorderColor: isDarkMode ? '#00e5ff' : '#007bbb',
                    },
                    Button: {
                        controlHeight: 44,
                        borderRadius: 6,
                    },
                },
            }}
        >
            <div className={`login-page ${isDarkMode ? 'dark-theme' : 'light-theme'}`}>
                {/* Scan line animation */}
                <div className="login-scanline" />

                {/* Theme toggle */}
                <button
                    className="login-theme-toggle"
                    onClick={() => setIsDarkMode(!isDarkMode)}
                    aria-label="切换主题"
                >
                    {isDarkMode ? <Sun size={18} /> : <Moon size={18} />}
                </button>

                {/* Grid overlay decoration */}
                <div className="login-grid-overlay" />

                <div className="login-card">
                    {/* Brand */}
                    <div className="login-brand">
                        <div className="login-brand-mark">颐</div>
                        <div>
                            <Title level={3} className="login-brand-title">颐养 ERP</Title>
                            <Text className="login-brand-subtitle">养老 · 物业一体化管理系统</Text>
                        </div>
                    </div>

                    <div className="login-divider" />

                    <Title level={4} className="login-heading">登录管理端</Title>
                    <Text className="login-hint">请输入管理员账号和密码</Text>

                    <Form
                        name="login"
                        onFinish={handleLogin}
                        autoComplete="off"
                        layout="vertical"
                        size="large"
                        className="login-form"
                    >
                        <Form.Item
                            name="username"
                            rules={[{ required: true, message: '请输入用户名' }]}
                        >
                            <Input
                                prefix={<UserOutlined />}
                                placeholder="用户名"
                                autoFocus
                                id="login-username"
                            />
                        </Form.Item>

                        <Form.Item
                            name="password"
                            rules={[{ required: true, message: '请输入密码' }]}
                        >
                            <Input.Password
                                prefix={<LockOutlined />}
                                placeholder="密码"
                                id="login-password"
                            />
                        </Form.Item>

                        <Form.Item>
                            <Button
                                type="primary"
                                htmlType="submit"
                                loading={loading}
                                block
                                className="login-submit-btn"
                                id="login-submit"
                            >
                                登 录
                            </Button>
                        </Form.Item>
                    </Form>

                    <div className="login-footer">
                        <Text className="login-footer-text">
                            初始账号：social_worker / sw_supervisor / dept_manager / prop_manager / prop_supervisor
                        </Text>
                        <Text className="login-footer-text">
                            初始密码：Erp@2026
                        </Text>
                    </div>
                </div>

                {/* Decorative corner elements */}
                <div className="login-corner login-corner-tl" />
                <div className="login-corner login-corner-tr" />
                <div className="login-corner login-corner-bl" />
                <div className="login-corner login-corner-br" />
            </div>
        </ConfigProvider>
    );
}
