import { loginApi, logoutApi, refreshApi } from '../lib/auth-api';
import { fetchWithoutAuth } from '../lib/http';

jest.mock('../lib/http', () => ({
  fetchWithoutAuth: jest.fn(),
  ApiProblemError: class extends Error {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    constructor(_s: number, _p: any) {
      super();
    }
  },
}));

const mockFetch = fetchWithoutAuth as jest.MockedFunction<typeof fetchWithoutAuth>;

describe('auth-api', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('loginApi POSTs to login', async () => {
    mockFetch.mockResolvedValue({ accessToken: 'a' });
    await loginApi({
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      email: 'a@b.com',
      password: 'password1',
    });
    expect(mockFetch).toHaveBeenCalledWith('POST', '/api/v1/auth/login', {
      organizationId: '550e8400-e29b-41d4-a716-446655440000',
      email: 'a@b.com',
      password: 'password1',
    });
  });

  it('refreshApi POSTs refresh body', async () => {
    mockFetch.mockResolvedValue({ accessToken: 'a' });
    await refreshApi({ refreshToken: 'r' });
    expect(mockFetch).toHaveBeenCalledWith('POST', '/api/v1/auth/refresh', {
      refreshToken: 'r',
    });
  });

  it('logoutApi POSTs logout', async () => {
    mockFetch.mockResolvedValue(undefined);
    await logoutApi({ refreshToken: 'r' });
    expect(mockFetch).toHaveBeenCalledWith('POST', '/api/v1/auth/logout', {
      refreshToken: 'r',
    });
  });
});
