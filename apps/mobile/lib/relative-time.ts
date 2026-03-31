/**
 * Relative time for an ISO-8601 UTC instant (e.g. issue `updatedAt`).
 */
export function formatRelativeTimeUtc(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return '—';
  }
  const now = Date.now();
  const diffSec = Math.round((then - now) / 1000);
  if (Number.isNaN(diffSec)) {
    return '—';
  }
  const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' });
  const abs = Math.abs(diffSec);
  if (abs < 60) {
    return rtf.format(Math.round(diffSec), 'second');
  }
  if (abs < 3600) {
    return rtf.format(Math.round(diffSec / 60), 'minute');
  }
  if (abs < 86400) {
    return rtf.format(Math.round(diffSec / 3600), 'hour');
  }
  return rtf.format(Math.round(diffSec / 86400), 'day');
}
