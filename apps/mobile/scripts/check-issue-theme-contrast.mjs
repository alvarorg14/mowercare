#!/usr/bin/env node
/**
 * WCAG 2.1 contrast sanity check for issue-flow theme tokens (Story 3.9).
 * Run: node scripts/check-issue-theme-contrast.mjs
 * Uses relative luminance for sRGB hex pairs — no extra dependencies.
 */

const issueStatusTokens = {
  open: '#2196F3',
  inProgress: '#FF9800',
  blocked: '#F57C00',
  resolved: '#66BB6A',
};

const surfaces = {
  surface: '#FFFFFF',
  background: '#F5F5F5',
};

const theme = {
  primary: '#2E7D32',
  onPrimary: '#FFFFFF',
  onSurface: '#1C1B1F',
  onSurfaceVariant: '#49454F',
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

console.log('\nIssue status chip text uses theme onSurface on tinted background (approximate chip fill = token 13% alpha on white)');
for (const [k, hex] of Object.entries(issueStatusTokens)) {
  const blended = mixWithWhite(hex, 0.13);
  check(`status token ${k} tint + onSurface`, theme.onSurface, blended, 4.5);
}

function mixWithWhite(hex, a) {
  const { r, g, b } = hexToRgb(hex);
  const br = Math.round(r * a + 255 * (1 - a));
  const bg = Math.round(g * a + 255 * (1 - a));
  const bb = Math.round(b * a + 255 * (1 - a));
  return `#${((1 << 24) + (br << 16) + (bg << 8) + bb).toString(16).slice(1)}`;
}

if (failed) {
  console.error('\nContrast check failed.');
  process.exit(1);
}
console.log('\nAll checks passed.');
