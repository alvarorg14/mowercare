import { MD3LightTheme } from 'react-native-paper';
import type { MD3Theme } from 'react-native-paper';

/**
 * Semantic issue status colors (UX-DR1) — chip backgrounds use light alpha; **text** on chips uses theme `onSurface` (not these hexes alone).
 * Contrast vs white + `onSurface` is checked in `scripts/check-issue-theme-contrast.mjs` (Story 3.9).
 */
export const issueStatusTokens = {
  open: '#2196F3',
  inProgress: '#FF9800',
  blocked: '#F57C00',
  resolved: '#66BB6A',
} as const;

/**
 * MD3 light theme tuned for outdoor legibility and WCAG 2.1 AA **intent** on default surfaces (NFR-A1, UX-DR15).
 * Audit: `npm run check:contrast` (WebAIM or similar for ad hoc spot checks when changing hexes).
 */
export const paperTheme: MD3Theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#2E7D32',
    onPrimary: '#FFFFFF',
    primaryContainer: '#C8E6C9',
    onPrimaryContainer: '#1B5E20',
    surface: '#FFFFFF',
    background: '#F5F5F5',
    onBackground: '#1C1B1F',
    onSurface: '#1C1B1F',
    onSurfaceVariant: '#49454F',
    error: '#B3261E',
    onError: '#FFFFFF',
    outline: '#79747E',
    outlineVariant: '#CAC4D0',
  },
};
