import { createQueryClient } from '../lib/queryClient';

describe('createQueryClient', () => {
  it('uses capped exponential retry delay', () => {
    const qc = createQueryClient();
    const delay = qc.getDefaultOptions().queries?.retryDelay;
    expect(typeof delay).toBe('function');
    const fn = delay as (n: number) => number;
    expect(fn(0)).toBe(1000);
    expect(fn(1)).toBe(2000);
    expect(fn(10)).toBe(30_000);
  });

  it('sets query retry to 3 and mutation retry to 0', () => {
    const qc = createQueryClient();
    expect(qc.getDefaultOptions().queries?.retry).toBe(3);
    expect(qc.getDefaultOptions().mutations?.retry).toBe(0);
  });
});
