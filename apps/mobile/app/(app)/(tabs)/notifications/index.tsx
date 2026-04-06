import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { Banner, Button, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { NotificationRow } from '../../../../components/NotificationRow';
import { getSessionOrganizationId } from '../../../../lib/auth/session';
import {
  listNotifications,
  markNotificationRead,
  notificationListQueryKey,
  type NotificationItem,
} from '../../../../lib/notification-api';
import { ApiProblemError } from '../../../../lib/http';

function errorMessage(err: unknown): string {
  if (err instanceof ApiProblemError) return err.detail ?? err.title ?? 'Request failed';
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

export default function NotificationsScreen() {
  const theme = useTheme();
  const router = useRouter();
  const queryClient = useQueryClient();
  const orgId = getSessionOrganizationId();
  const [markReadError, setMarkReadError] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: notificationListQueryKey(orgId),
    queryFn: () => listNotifications(0, 50),
    enabled: !!orgId,
  });

  const markReadMutation = useMutation({
    mutationFn: (recipientId: string) => markNotificationRead(recipientId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationListQueryKey(orgId) });
    },
  });

  const onOpen = useCallback(
    async (item: NotificationItem) => {
      setMarkReadError(null);
      if (!item.read) {
        try {
          await markReadMutation.mutateAsync(item.id);
        } catch (err) {
          setMarkReadError(errorMessage(err));
          return;
        }
      }
      router.push(`/issues/${item.issueId}`);
    },
    [markReadMutation, router],
  );

  const items = listQuery.data?.items ?? [];
  const showEmpty = listQuery.isSuccess && items.length === 0;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <Text variant="headlineSmall">Notifications</Text>
        <Text variant="bodySmall" style={[styles.sub, { color: theme.colors.onSurfaceVariant }]}>
          Issue creates, assignments, and status changes you are eligible to see.
        </Text>
      </View>

      {listQuery.isError ? (
        <Banner visible actions={[{ label: 'Retry', onPress: () => void listQuery.refetch() }]}>
          {errorMessage(listQuery.error)}
        </Banner>
      ) : null}

      {markReadError ? (
        <Banner
          visible
          actions={[{ label: 'Dismiss', onPress: () => setMarkReadError(null) }]}
        >
          {markReadError}
        </Banner>
      ) : null}

      {!orgId ? (
        <View style={styles.centered}>
          <Text variant="bodyMedium">Sign in to see notifications.</Text>
        </View>
      ) : listQuery.isPending ? (
        <View style={styles.centered}>
          <ActivityIndicator accessibilityLabel="Loading notifications" />
        </View>
      ) : showEmpty ? (
        <View style={styles.emptyWrap}>
          <View style={styles.emptyCard}>
            <Text variant="bodyLarge">No activity yet</Text>
            <Text variant="bodyMedium" style={[styles.muted, { color: theme.colors.onSurfaceVariant }]}>
              When teammates create or update issues you follow, entries appear here. Push can be added
              later; this list works even when push is off.
            </Text>
            <Button mode="outlined" onPress={() => router.push('/issues')} style={styles.emptyBtn}>
              Go to Issues
            </Button>
          </View>
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(it) => it.id}
          refreshControl={
            <RefreshControl refreshing={listQuery.isFetching} onRefresh={() => void listQuery.refetch()} />
          }
          renderItem={({ item }) => <NotificationRow item={item} onPress={() => onOpen(item)} />}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: { paddingHorizontal: 16, paddingBottom: 8, gap: 4 },
  sub: { marginTop: 4 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyWrap: { flex: 1, padding: 24, justifyContent: 'center' },
  emptyCard: { gap: 12 },
  muted: { lineHeight: 22 },
  emptyBtn: { alignSelf: 'flex-start', marginTop: 8 },
});
