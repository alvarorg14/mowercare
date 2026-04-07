import { issueStatusTokens, paperTheme } from '../lib/theme';

describe('theme', () => {
  it('exposes issue status token colors', () => {
    expect(issueStatusTokens.open).toMatch(/^#/);
    expect(issueStatusTokens.inProgress).toMatch(/^#/);
    expect(issueStatusTokens.resolved).toMatch(/^#/);
  });

  it('paperTheme overrides primary and surfaces', () => {
    expect(paperTheme.colors.primary).toBe('#1B5E20');
    expect(paperTheme.colors.surface).toBe('#FFFBFF');
    expect(paperTheme.colors.onSurface).toBe('#1A1C1A');
    expect(paperTheme.roundness).toBe(12);
  });
});
