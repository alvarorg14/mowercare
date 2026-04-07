import { issueStatusTokens, paperTheme } from '../lib/theme';

describe('theme', () => {
  it('exposes issue status token colors', () => {
    expect(issueStatusTokens.open).toMatch(/^#/);
    expect(issueStatusTokens.inProgress).toMatch(/^#/);
    expect(issueStatusTokens.resolved).toMatch(/^#/);
  });

  it('paperTheme overrides primary and surfaces', () => {
    expect(paperTheme.colors.primary).toBe('#2E7D32');
    expect(paperTheme.colors.surface).toBe('#FFFFFF');
    expect(paperTheme.colors.onSurface).toBe('#1C1B1F');
  });
});
