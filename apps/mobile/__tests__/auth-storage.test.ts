import * as SecureStore from 'expo-secure-store';

import { clearRefreshToken, getRefreshToken, setRefreshToken } from '../lib/auth-storage';

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

describe('auth-storage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('getRefreshToken returns null when SecureStore throws', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockRejectedValue(new Error('unavailable'));
    await expect(getRefreshToken()).resolves.toBeNull();
  });

  it('getRefreshToken returns stored value', async () => {
    (SecureStore.getItemAsync as jest.Mock).mockResolvedValue('tok');
    await expect(getRefreshToken()).resolves.toBe('tok');
  });

  it('setRefreshToken writes to SecureStore', async () => {
    await setRefreshToken('abc');
    expect(SecureStore.setItemAsync).toHaveBeenCalled();
  });

  it('clearRefreshToken swallows delete errors', async () => {
    (SecureStore.deleteItemAsync as jest.Mock).mockRejectedValue(new Error('fail'));
    await expect(clearRefreshToken()).resolves.toBeUndefined();
  });
});
