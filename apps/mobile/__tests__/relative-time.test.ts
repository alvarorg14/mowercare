import { formatRelativeTimeUtc } from '../lib/relative-time';

describe('formatRelativeTimeUtc', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2026-04-07T12:00:00.000Z'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('returns em dash for invalid ISO string', () => {
    expect(formatRelativeTimeUtc('not-a-date')).toBe('—');
  });

  it('formats a time a few minutes in the past', () => {
    const iso = '2026-04-07T11:55:00.000Z';
    const out = formatRelativeTimeUtc(iso);
    expect(out).toMatch(/minute/);
  });

  it('formats a time in the future', () => {
    const iso = '2026-04-07T13:00:00.000Z';
    const out = formatRelativeTimeUtc(iso);
    expect(out).toMatch(/hour/);
  });
});
