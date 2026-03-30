import { MD3LightTheme } from 'react-native-paper';
import type { MD3Theme } from 'react-native-paper';

/**
 * Semantic issue status colors (UX-DR1 stub) — import from here in later stories; avoid ad hoc hex in screens.
 */
export const issueStatusTokens = {
  open: '#2196F3',
  inProgress: '#FF9800',
  blocked: '#F57C00',
  resolved: '#66BB6A',
} as const;

/** MD3 baseline; outdoor-friendly body sizes can be tuned in a follow-up via configureFonts. */
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
  },
};
