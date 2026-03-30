import { fetchWithoutAuth } from './http';

export type LoginRequestBody = {
  organizationId: string;
  email: string;
  password: string;
};

export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
};

export type RefreshRequestBody = { refreshToken: string };

export async function loginApi(body: LoginRequestBody): Promise<TokenResponse> {
  return fetchWithoutAuth<TokenResponse>('POST', '/api/v1/auth/login', body);
}

export async function refreshApi(body: RefreshRequestBody): Promise<TokenResponse> {
  return fetchWithoutAuth<TokenResponse>('POST', '/api/v1/auth/refresh', body);
}

export async function logoutApi(body: RefreshRequestBody): Promise<void> {
  await fetchWithoutAuth<void>('POST', '/api/v1/auth/logout', body);
}
