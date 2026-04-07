#!/usr/bin/env node
/**
 * WCAG 2.1 contrast sanity check for issue-flow theme tokens (Story 3.9; palette synced with `lib/theme.ts` Story 5.5).
 * Run: node scripts/check-issue-theme-contrast.mjs
 * Uses relative luminance for sRGB hex pairs — no extra dependencies.
 */

const issueStatusTokens = {
  open: '#1E88E5',
  inProgress: '#FB8C00',
  blocked: '#EF6C00',
  resolved: '#43A047',
};

const surfaces = {
  surface: '#FFFBFF',
  background: '#F2F5F2',
};

const theme = {
  primary: '#1B5E20',
  onPrimary: '#FFFFFF',
  onSurface: '#1A1C1A',
  onSurfaceVariant: '#424943',
  error: '#B3261E',
};

function hexToRgb(hex) {
  const h = hex.replace('#', '');
  const n = parseInt(h, 16);
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
}

function lin(c) {
  const s = c / 255;
  return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
}

function lum(hex) {
  const { r, g, b } = hexToRgb(hex);
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
}

function ratio(a, b) {
  const L1 = lum(a);
  const L2 = lum(b);
  const lighter = Math.max(L1, L2);
  const darker = Math.min(L1, L2);
  return (lighter + 0.05) / (darker + 0.05);
}

let failed = false;
function check(name, fg, bg, min) {
  const r = ratio(fg, bg);
  const ok = r >= min;
  if (!ok) failed = true;
  console.log(`${ok ? 'OK' : 'FAIL'} ${name}: ${r.toFixed(2)}:1 (min ${min}:1) ${fg} on ${bg}`);
}

console.log('Theme + semantic text');
check('onSurface on surface', theme.onSurface, surfaces.surface, 4.5);
check('onSurfaceVariant on surface', theme.onSurfaceVariant, surfaces.surface, 4.5);
check('onPrimary on primary', theme.onPrimary, theme.primary, 4.5);
check('error on surface (body)', theme.error, surfaces.surface, 4.5);

/** IssueRow tints status chips with `token + '22'` (8-digit hex → alpha 34/255). */
const CHIP_ALPHA = 34 / 255;
console.log(
  `\nIssue status chip text uses theme onSurface on tinted background (chip fill ≈ token ${CHIP_ALPHA.toFixed(3)} alpha on surface ${surfaces.surface})`,
);
for (const [k, hex] of Object.entries(issueStatusTokens)) {
  const blended = mixTokenOverSurface(hex, CHIP_ALPHA, surfaces.surface);
  check(`status token ${k} tint + onSurface`, theme.onSurface, blended, 4.5);
}

/** Alpha-composite `hex` over `baseHex` (same model as token + '22' over list surface). */
function mixTokenOverSurface(hex, a, baseHex) {
  const { r, g, b } = hexToRgb(hex);
  const { r: br0, g: bg0, b: bb0 } = hexToRgb(baseHex);
  const br = Math.round(r * a + br0 * (1 - a));
  const bg = Math.round(g * a + bg0 * (1 - a));
  const bb = Math.round(b * a + bb0 * (1 - a));
  return `#${((1 << 24) + (br << 16) + (bg << 8) + bb).toString(16).slice(1)}`;
}

if (failed) {
  console.error('\nContrast check failed.');
  process.exit(1);
}
console.log('\nAll checks passed.');
