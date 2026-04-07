import { ApiProblemError, fetchWithoutAuth } from '../lib/http';

jest.mock('../lib/config', () => ({
  getApiBaseUrl: () => 'http://localhost:8080',
}));

describe('ApiProblemError', () => {
  it('sets message from detail over title', () => {
    const e = new ApiProblemError(400, { title: 'Bad', detail: 'Specific' });
    expect(e.message).toBe('Specific');
    expect(e.status).toBe(400);
    expect(e.name).toBe('ApiProblemError');
  });

  it('falls back to title when no detail', () => {
    const e = new ApiProblemError(500, { title: 'Server' });
    expect(e.message).toBe('Server');
  });
});

describe('fetchWithoutAuth', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it('returns JSON body on 200', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => '{"a":1}',
    });
    await expect(fetchWithoutAuth('GET', '/x')).resolves.toEqual({ a: 1 });
    expect(global.fetch).toHaveBeenCalledWith('http://localhost:8080/x', {
      method: 'GET',
      headers: undefined,
      body: undefined,
    });
  });

  it('returns undefined on 204', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: true,
      status: 204,
      text: async () => '',
    });
    await expect(fetchWithoutAuth('POST', '/x', {})).resolves.toBeUndefined();
  });

  it('throws ApiProblemError on 400 with problem JSON', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: false,
      status: 400,
      text: async () => JSON.stringify({ title: 'Bad', code: 'X', detail: 'Nope' }),
    });
    await expect(fetchWithoutAuth('GET', '/bad')).rejects.toMatchObject({
      status: 400,
      detail: 'Nope',
    });
  });

  it('throws ApiProblemError on non-JSON error body', async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      ok: false,
      status: 502,
      statusText: 'Bad Gateway',
      text: async () => 'plain',
    });
    await expect(fetchWithoutAuth('GET', '/bad')).rejects.toMatchObject({
      status: 502,
    });
  });
});
