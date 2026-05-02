const API_BASE = '';

export interface AuthUser {
    userId: string;
    displayName: string;
    role: string;
    tenantId: string;
    facilityId: string;
}

interface LoginResponse {
    token: string;
    user: AuthUser;
}

const TOKEN_KEY = 'erp_token';
const USER_KEY = 'erp_user';

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function getUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
        return JSON.parse(raw);
    } catch {
        return null;
    }
}

export function isAuthenticated(): boolean {
    return !!getToken() && !!getUser();
}

export async function login(username: string, password: string): Promise<AuthUser> {
    const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    });

    if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.message || '用户名或密码错误');
    }

    const data: LoginResponse = await res.json();
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    return data.user;
}

export function logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
    const token = getToken();
    const headers = new Headers(options.headers);
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

    if (res.status === 401) {
        logout();
        window.location.href = '/';
    }

    return res;
}
