import { registerDevicePushToken, revokeDevicePushToken } from '../lib/device-token-api';
import { authenticatedFetchJson } from '../lib/api';

jest.mock('../lib/api', () => ({
  authenticatedFetchJson: jest.fn(),
}));

const mockJson = authenticatedFetchJson as jest.MockedFunction<typeof authenticatedFetchJson>;

describe('device-token-api', () => {
  beforeEach(() => {
    mockJson.mockReset();
    mockJson.mockResolvedValue({ id: 'id-1' });
  });

  it('registerDevicePushToken PUTs token and platform', async () => {
    await registerDevicePushToken('org-1', 'device-token', 'ANDROID');
    expect(mockJson).toHaveBeenCalledWith(
      '/api/v1/organizations/org-1/device-push-tokens',
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify({ token: 'device-token', platform: 'ANDROID' }),
      }),
    );
  });

  it('revokeDevicePushToken DELETEs with body', async () => {
    mockJson.mockResolvedValue(undefined);
    await revokeDevicePushToken('org-1', 'device-token');
    expect(mockJson).toHaveBeenCalledWith(
      '/api/v1/organizations/org-1/device-push-tokens',
      expect.objectContaining({
        method: 'DELETE',
        body: JSON.stringify({ token: 'device-token' }),
      }),
    );
  });
});
