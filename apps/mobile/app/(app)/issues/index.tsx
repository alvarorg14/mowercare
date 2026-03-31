import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { Banner, Button, FAB, SegmentedButtons, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { IssueRow } from '../../../components/IssueRow';
import { getSessionOrganizationId } from '../../../lib/auth/session';
import { listIssues, type IssueListScope } from '../../../lib/issue-api';
import { ApiProblemError } from '../../../lib/http';

function errorMessage(err: unknown): string {
  if (err instanceof ApiProblemError) return err.detail ?? err.title ?? 'Request failed';
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

export default function IssuesHomeScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [scope, setScope] = useState<IssueListScope>('open');
  const orgId = getSessionOrganizationId();

  const listQuery = useQuery({
    queryKey: ['issues', orgId, scope],
    queryFn: () => listIssues(scope),
    enabled: !!orgId,
  });

  const probeQuery = useQuery({
    queryKey: ['issues', orgId, 'all'],
    queryFn: () => listIssues('all'),
    enabled:
      !!orgId &&
      scope !== 'all' &&
      listQuery.isSuccess &&
      (listQuery.data?.items.length ?? 0) === 0,
  });

  const items = listQuery.data?.items ?? [];
  const showEmpty = listQuery.isSuccess && items.length === 0;
  const probeLoading = scope !== 'all' && showEmpty && probeQuery.isPending;
  const filteredEmpty =
    showEmpty &&
    scope !== 'all' &&
    probeQuery.isSuccess &&
    (probeQuery.data?.items.length ?? 0) > 0;
  const orgWideEmpty =
    showEmpty &&
    (scope === 'all' ||
      (probeQuery.isSuccess && (probeQuery.data?.items.length ?? 0) === 0));

  const onRefresh = () => {
    listQuery.refetch();
    if (probeQuery.isEnabled) void probeQuery.refetch();
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <Text variant="headlineSmall">Issues</Text>
        <SegmentedButtons
          value={scope}
          onValueChange={(v) => setScope(v as IssueListScope)}
          buttons={[
            { value: 'open', label: 'Open', disabled: !orgId },
            { value: 'all', label: 'All', disabled: !orgId },
            { value: 'mine', label: 'Mine', disabled: !orgId },
          ]}
        />
      </View>

      {!orgId ? (
        <Banner
          visible
          icon="account-alert-outline"
          actions={[{ label: 'Sign in', onPress: () => router.replace('/(auth)') }]}
        >
          Organization is not loaded. Sign in again to view issues.
        </Banner>
      ) : null}

      {orgId && listQuery.error ? (
        <Banner
          visible
          actions={[{ label: 'Retry', onPress: () => void listQuery.refetch() }]}
          icon="alert-circle-outline"
        >
          {errorMessage(listQuery.error)}
        </Banner>
      ) : null}

      {orgId && listQuery.isLoading && !listQuery.data ? (
        <View style={styles.centered} accessibilityLabel="Loading issues">
          <ActivityIndicator size="large" />
        </View>
      ) : null}

      {orgId && listQuery.isSuccess && items.length > 0 ? (
        <FlatList
          style={styles.flex}
          data={items}
          keyExtractor={(i) => i.id}
          renderItem={({ item }) => (
            <IssueRow item={item} onPress={() => router.push(`/issues/${item.id}`)} />
          )}
          refreshControl={
            <RefreshControl
              refreshing={listQuery.isFetching && !listQuery.isLoading}
              onRefresh={onRefresh}
            />
          }
          contentContainerStyle={styles.listContent}
        />
      ) : null}

      {orgId && showEmpty && listQuery.isSuccess ? (
        <View style={styles.emptyWrap}>
          {probeLoading ? (
            <ActivityIndicator accessibilityLabel="Checking for other issues" />
          ) : filteredEmpty ? (
            <View style={styles.emptyCard}>
              <Text variant="bodyLarge">Nothing in this queue</Text>
              <Text variant="bodyMedium" style={styles.muted}>
                Try another scope or view all issues.
              </Text>
              <Button mode="contained" onPress={() => setScope('all')} style={styles.emptyBtn}>
                View all
              </Button>
            </View>
          ) : orgWideEmpty ? (
            <View style={styles.emptyCard}>
              <Text variant="bodyLarge">No issues yet</Text>
              <Text variant="bodyMedium" style={styles.muted}>
                Capture work with New issue.
              </Text>
              <Button mode="contained" onPress={() => router.push('/issues/create')} style={styles.emptyBtn}>
                New issue
              </Button>
            </View>
          ) : null}
        </View>
      ) : null}

      <FAB
        icon="plus"
        label="New issue"
        disabled={!orgId}
        style={[styles.fab, { backgroundColor: theme.colors.primary }]}
        onPress={() => router.push('/issues/create')}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  header: { paddingHorizontal: 16, paddingTop: 8, gap: 12 },
  muted: { opacity: 0.75 },
  fab: { position: 'absolute', right: 16, bottom: 24 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  listContent: { paddingBottom: 88 },
  emptyWrap: { flex: 1, justifyContent: 'center', paddingHorizontal: 24 },
  emptyCard: { gap: 12, alignItems: 'flex-start' },
  emptyBtn: { marginTop: 4 },
});
