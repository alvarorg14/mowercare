import { authenticatedFetchJson } from '../lib/api';
import { setSession } from '../lib/auth/session';

jest.mock('../lib/config', () => ({
  getApiBaseUrl: () => 'http://localhost:8080',
}));

describe('authenticatedFetchJson', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    setSession(null, undefined, null, null);
  });

  it('GETs with Bearer header and parses JSON', async () => {
    setSession('a.b.c', 'Bearer', '550e8400-e29b-41d4-a716-446655440000', 'ADMIN');
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => '{"ok":true}',
    });
    await expect(authenticatedFetchJson('/api/v1/ping')).resolves.toEqual({ ok: true });
    expect(global.fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/ping',
      expect.objectContaining({
        method: 'GET',
        headers: expect.any(Headers),
      }),
    );
    const call = (global.fetch as jest.Mock).mock.calls[0];
    const headers = call[1].headers as Headers;
    expect(headers.get('Authorization')).toMatch(/^Bearer a\.b\.c$/);
  });

  it('throws when not signed in', async () => {
    await expect(authenticatedFetchJson('/x')).rejects.toMatchObject({ status: 401 });
  });
});
