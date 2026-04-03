import { useQuery } from '@tanstack/react-query';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { Button, Text, useTheme } from 'react-native-paper';

import { ApiProblemError } from '../lib/http';
import {
  listIssueChangeEvents,
  type IssueChangeEventItem,
} from '../lib/issue-api';
import { formatRelativeTimeUtc } from '../lib/relative-time';

function humanizeEnum(s: string | null | undefined): string {
  if (s == null || s === '') return '—';
  return s.replace(/_/g, ' ');
}

function truncate(s: string | null | undefined, max: number): string {
  if (s == null) return '';
  if (s.length <= max) return s;
  return `${s.slice(0, max)}…`;
}

/** One-line summary for timeline row (matches story UX copy guidelines). */
export function summarizeChangeEvent(ev: IssueChangeEventItem): string {
  switch (ev.changeType) {
    case 'CREATED':
      return 'Issue created';
    case 'STATUS_CHANGED':
      return `Status: ${humanizeEnum(ev.oldValue)} → ${humanizeEnum(ev.newValue)}`;
    case 'ASSIGNEE_CHANGED': {
      const o = ev.oldAssigneeLabel ?? 'Unassigned';
      const n = ev.newAssigneeLabel ?? 'Unassigned';
      return `Assignee: ${o} → ${n}`;
    }
    case 'PRIORITY_CHANGED':
      return `Priority: ${ev.oldValue ?? '—'} → ${ev.newValue ?? '—'}`;
    case 'TITLE_CHANGED':
      return `Title: ${truncate(ev.oldValue, 40)} → ${truncate(ev.newValue, 40)}`;
    case 'DESCRIPTION_CHANGED':
      return `Description updated`;
    case 'CUSTOMER_LABEL_CHANGED':
      return `Customer: ${truncate(ev.oldValue, 32)} → ${truncate(ev.newValue, 32)}`;
    case 'SITE_LABEL_CHANGED':
      return `Site: ${truncate(ev.oldValue, 32)} → ${truncate(ev.newValue, 32)}`;
    default:
      return ev.changeType;
  }
}

function errorMessage(err: unknown): string {
  if (err instanceof ApiProblemError) return err.detail ?? err.title ?? 'Request failed';
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

type Props = {
  orgId: string | null;
  issueId: string;
};

export function IssueActivityTimeline({ orgId, issueId }: Props) {
  const theme = useTheme();
  const q = useQuery({
    queryKey: ['issue-change-events', orgId, issueId],
    queryFn: () => listIssueChangeEvents(issueId),
    enabled: !!orgId && !!issueId,
  });

  if (q.isLoading) {
    return (
      <View style={styles.block} accessibilityLabel="Loading activity">
        <ActivityIndicator />
      </View>
    );
  }

  if (q.error) {
    const errText = errorMessage(q.error);
    return (
      <View style={styles.block}>
        <Text
          variant="bodyMedium"
          style={{ color: theme.colors.error }}
          accessibilityRole="alert"
          accessibilityLiveRegion="polite"
          accessibilityLabel={`Activity failed to load. ${errText}`}
        >
          {errText}
        </Text>
        <Button
          mode="text"
          onPress={() => void q.refetch()}
          accessibilityLabel="Retry loading activity"
          style={styles.retryBtn}
          contentStyle={styles.retryContent}
        >
          Retry
        </Button>
      </View>
    );
  }

  const items = q.data?.items ?? [];
  if (items.length === 0) {
    return (
      <View style={styles.block}>
        <Text variant="bodyMedium" style={[styles.muted, { color: theme.colors.onSurfaceVariant }]}>
          No activity yet
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.block}>
      {items.map((ev) => {
        const rel = formatRelativeTimeUtc(ev.occurredAt);
        const iso = ev.occurredAt;
        const summary = summarizeChangeEvent(ev);
        const actor = ev.actorLabel?.trim() || 'Someone';
        const a11y = `${summary}. ${actor} · ${rel}. ${iso}`;
        return (
          <View
            key={ev.id}
            accessible
            style={[styles.row, { borderBottomColor: theme.colors.outlineVariant }]}
            accessibilityLabel={a11y}
          >
            <Text variant="bodyLarge" style={styles.summary}>
              {summary}
            </Text>
            <Text variant="bodySmall" style={[styles.meta, { color: theme.colors.onSurfaceVariant }]}>
              {actor} · {rel}
            </Text>
            <Text variant="labelSmall" style={[styles.iso, { color: theme.colors.onSurfaceVariant }]}>
              {iso}
            </Text>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  block: { marginTop: 8, gap: 12 },
  row: { gap: 4, paddingVertical: 8, borderBottomWidth: StyleSheet.hairlineWidth },
  summary: { fontSize: 16 },
  meta: {},
  iso: {},
  muted: {},
  retryBtn: { alignSelf: 'flex-start' },
  retryContent: { minHeight: 44, justifyContent: 'center' },
});
