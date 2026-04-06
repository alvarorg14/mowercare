import { Pressable, StyleSheet, View } from 'react-native';
import { Text, useTheme } from 'react-native-paper';

import { formatNotificationEventType, type NotificationItem } from '../lib/notification-api';
import { formatRelativeTimeUtc } from '../lib/relative-time';

type Props = {
  item: NotificationItem;
  onPress: () => void;
};

export function NotificationRow({ item, onPress }: Props) {
  const theme = useTheme();
  const unread = !item.read;
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`${formatNotificationEventType(item.eventType)}, ${item.issueTitle}`}
      style={({ pressed }) => [
        styles.row,
        {
          backgroundColor: pressed ? theme.colors.elevation.level1 : theme.colors.surface,
          borderBottomColor: theme.colors.outlineVariant,
        },
        unread && { backgroundColor: theme.colors.elevation.level1 },
      ]}
    >
      <View style={styles.textBlock}>
        <Text
          variant="titleSmall"
          style={[styles.issueTitle, unread && styles.unreadText]}
          numberOfLines={2}
        >
          {item.issueTitle}
        </Text>
        <Text variant="bodyMedium" style={{ color: theme.colors.primary }}>
          {formatNotificationEventType(item.eventType)}
        </Text>
        <Text variant="bodySmall" style={[styles.time, { color: theme.colors.onSurfaceVariant }]}>
          {formatRelativeTimeUtc(item.occurredAt)}
        </Text>
      </View>
      {unread ? (
        <View style={[styles.dot, { backgroundColor: theme.colors.primary }]} accessibilityLabel="Unread" />
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    minHeight: 72,
  },
  textBlock: { flex: 1, gap: 4, paddingRight: 8 },
  issueTitle: { fontWeight: '500' },
  unreadText: { fontWeight: '700' },
  time: { marginTop: 2 },
  dot: { width: 10, height: 10, borderRadius: 5 },
});
