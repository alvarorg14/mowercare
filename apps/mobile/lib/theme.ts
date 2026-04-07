import { MD3LightTheme } from 'react-native-paper';
import type { MD3Theme } from 'react-native-paper';

/**
 * Semantic issue status colors (UX-DR1) — chip backgrounds use light alpha; **text** on chips uses theme `onSurface` (not these hexes alone).
 * Contrast vs white + `onSurface` is checked in `scripts/check-issue-theme-contrast.mjs` (Story 3.9).
 */
export const issueStatusTokens = {
  open: '#1E88E5',
  inProgress: '#FB8C00',
  blocked: '#EF6C00',
  resolved: '#43A047',
} as const;

/**
 * MD3 light theme: contemporary spacing/surface hierarchy (Story 5.5), outdoor legibility (NFR-A1, UX-DR15).
 * Single source of truth — evolve tokens here; keep `check-issue-theme-contrast.mjs` in sync when palette changes.
 * Audit: `npm run check:contrast`
 */
export const paperTheme: MD3Theme = {
  ...MD3LightTheme,
  roundness: 12,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#1B5E20',
    onPrimary: '#FFFFFF',
    primaryContainer: '#C8E6C9',
    onPrimaryContainer: '#0D260F',
    secondary: '#4A6352',
    onSecondary: '#FFFFFF',
    secondaryContainer: '#CDE7D4',
    onSecondaryContainer: '#062112',
    tertiary: '#386660',
    onTertiary: '#FFFFFF',
    tertiaryContainer: '#BCECE4',
    onTertiaryContainer: '#00201C',
    surface: '#FFFBFF',
    surfaceVariant: '#E0E4E0',
    background: '#F2F5F2',
    onBackground: '#1A1C1A',
    onSurface: '#1A1C1A',
    onSurfaceVariant: '#424943',
    error: '#B3261E',
    onError: '#FFFFFF',
    errorContainer: '#F9DEDC',
    onErrorContainer: '#410E0B',
    outline: '#72796F',
    outlineVariant: '#C3C9C3',
    elevation: {
      ...MD3LightTheme.colors.elevation,
      level1: 'rgb(241, 246, 241)',
      level2: 'rgb(235, 242, 236)',
      level3: 'rgb(230, 238, 231)',
      level4: 'rgb(226, 235, 227)',
      level5: 'rgb(222, 232, 224)',
    },
  },
};
