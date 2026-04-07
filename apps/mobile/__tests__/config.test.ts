describe('getApiBaseUrl', () => {
  const originalEnv = process.env.EXPO_PUBLIC_API_BASE_URL;

  afterEach(() => {
    jest.resetModules();
    if (originalEnv === undefined) {
      delete process.env.EXPO_PUBLIC_API_BASE_URL;
    } else {
      process.env.EXPO_PUBLIC_API_BASE_URL = originalEnv;
    }
  });

  it('strips trailing slash from EXPO_PUBLIC_API_BASE_URL', () => {
    jest.doMock('expo-constants', () => ({
      __esModule: true,
      default: { expoConfig: { extra: {} } },
    }));
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://192.168.1.5:8080/';
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { getApiBaseUrl } = require('../lib/config');
    expect(getApiBaseUrl()).toBe('http://192.168.1.5:8080');
  });

  it('uses expo extra when env is unset', () => {
    delete process.env.EXPO_PUBLIC_API_BASE_URL;
    jest.doMock('expo-constants', () => ({
      __esModule: true,
      default: {
        expoConfig: { extra: { apiBaseUrl: 'https://api.example.com/' } },
      },
    }));
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const { getApiBaseUrl } = require('../lib/config');
    expect(getApiBaseUrl()).toBe('https://api.example.com');
  });
});
