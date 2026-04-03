import { Pressable, StyleSheet, View } from 'react-native';
import { Chip, Text, useTheme } from 'react-native-paper';

import { issueStatusTokens } from '../lib/theme';
import { formatRelativeTimeUtc } from '../lib/relative-time';
import type { IssueListItem } from '../lib/issue-api';

function statusColor(status: string): string {
  switch (status) {
    case 'OPEN':
      return issueStatusTokens.open;
    case 'IN_PROGRESS':
      return issueStatusTokens.inProgress;
    case 'WAITING':
      return issueStatusTokens.blocked;
    case 'RESOLVED':
    case 'CLOSED':
      return issueStatusTokens.resolved;
    default:
      return issueStatusTokens.open;
  }
}

function statusLabel(status: string): string {
  return status.replace(/_/g, ' ');
}

type Props = {
  item: IssueListItem;
  onPress: () => void;
};

export function IssueRow({ item, onPress }: Props) {
  const theme = useTheme();
  const sc = statusColor(item.status);
  const siteLine = [item.customerLabel, item.siteLabel].filter(Boolean).join(' · ');
  const assignee = item.assigneeLabel?.trim() || 'Unassigned';
  const rel = formatRelativeTimeUtc(item.updatedAt);
  const st = statusLabel(item.status);
  const a11yLabel = `Issue ${item.title}. Status ${st}. Priority ${item.priority}. Assignee ${assignee}. Updated ${rel}.`;

  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={a11yLabel}
    >
      <View
        style={[
          styles.row,
          { backgroundColor: theme.colors.surface, borderBottomColor: theme.colors.outlineVariant },
        ]}
      >
        <View style={styles.topLine}>
          <Text variant="titleMedium" numberOfLines={2} style={styles.flex}>
            {item.title}
          </Text>
          <Text variant="labelSmall" style={[styles.time, { color: theme.colors.onSurfaceVariant }]}>
            {rel}
          </Text>
        </View>
        <Text
          variant="labelSmall"
          style={[styles.idHint, { color: theme.colors.onSurfaceVariant }]}
          numberOfLines={1}
        >
          {item.id.slice(0, 8)}…
        </Text>
        {siteLine ? (
          <Text
            variant="bodySmall"
            numberOfLines={2}
            style={[styles.muted, { color: theme.colors.onSurfaceVariant }]}
          >
            {siteLine}
          </Text>
        ) : null}
        <View style={styles.chips}>
          <Chip
            mode="flat"
            compact
            textStyle={styles.chipText}
            style={[styles.chip, { backgroundColor: sc + '22' }]}
            icon="circle-outline"
          >
            {statusLabel(item.status)}
          </Chip>
          <Chip mode="outlined" compact icon="flag-outline">
            {item.priority}
          </Chip>
        </View>
        <Text variant="bodySmall" numberOfLines={1}>
          {assignee}
        </Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 6,
    minHeight: 56,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  topLine: { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  flex: { flex: 1 },
  time: { maxWidth: '28%' },
  idHint: { fontFamily: 'monospace' },
  muted: {},
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  chip: { minHeight: 32 },
  chipText: { fontSize: 12 },
});
