import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Platform,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';
import {
  Banner,
  Button,
  Checkbox,
  Dialog,
  FAB,
  IconButton,
  Menu,
  Portal,
  SegmentedButtons,
  Text,
  useTheme,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { IssueRow } from '../../../../components/IssueRow';
import { getSessionOrganizationId } from '../../../../lib/auth/session';
import {
  defaultIssueListParams,
  listIssues,
  type IssueListParams,
  type IssueListScope,
  type IssueListSortField,
} from '../../../../lib/issue-api';
import { issuePriorities, issueStatuses } from '../../../../lib/issue-create-schema';
import { ApiProblemError } from '../../../../lib/http';

function errorMessage(err: unknown): string {
  if (err instanceof ApiProblemError) return err.detail ?? err.title ?? 'Request failed';
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

function hasActiveFilters(p: IssueListParams): boolean {
  return (
    p.statuses.length > 0 ||
    p.priorities.length > 0 ||
    p.sort !== 'updatedAt' ||
    p.direction !== 'desc'
  );
}

/** Main query is already unfiltered org-wide with default sort — if empty, org has no issues. */
function isDefaultAllUnfiltered(p: IssueListParams): boolean {
  return p.scope === 'all' && !hasActiveFilters(p);
}

function issueListQueryKey(orgId: string | null | undefined, p: IssueListParams) {
  return [
    'issues',
    orgId,
    p.scope,
    [...p.statuses].sort().join(','),
    [...p.priorities].sort().join(','),
    p.sort,
    p.direction,
  ] as const;
}

function sortMenuLabel(p: IssueListParams): string {
  const key = `${p.sort}|${p.direction}` as const;
  const labels: Record<string, string> = {
    'updatedAt|desc': 'Recent activity',
    'updatedAt|asc': 'Oldest activity',
    'createdAt|desc': 'Newest created',
    'createdAt|asc': 'Oldest created',
    'priority|desc': 'Priority (high first)',
    'priority|asc': 'Priority (low first)',
  };
  return labels[key] ?? 'Sort';
}

function setSort(p: IssueListParams, sort: IssueListSortField, direction: 'asc' | 'desc'): IssueListParams {
  return { ...p, sort, direction };
}

export default function IssuesHomeScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [listParams, setListParams] = useState<IssueListParams>(() => defaultIssueListParams('open'));
  const [sortMenuOpen, setSortMenuOpen] = useState(false);
  const [filterDialogOpen, setFilterDialogOpen] = useState(false);
  const orgId = getSessionOrganizationId();

  const listQuery = useQuery({
    queryKey: issueListQueryKey(orgId, listParams),
    queryFn: () => listIssues(listParams),
    enabled: !!orgId,
  });

  const items = listQuery.data?.items ?? [];
  const showEmpty = listQuery.isSuccess && items.length === 0;

  const needsProbe =
    !!orgId && listQuery.isSuccess && items.length === 0 && !isDefaultAllUnfiltered(listParams);

  const probeQuery = useQuery({
    queryKey: ['issues', orgId, 'probe-all-unfiltered'],
    queryFn: () => listIssues(defaultIssueListParams('all')),
    enabled: needsProbe,
  });

  const probeLoading = needsProbe && probeQuery.isPending;
  const orgHasIssues = (probeQuery.data?.items.length ?? 0) > 0;

  const filteredOrScopedEmpty = useMemo(() => {
    if (!showEmpty || !probeQuery.isSuccess) return false;
    return orgHasIssues;
  }, [showEmpty, probeQuery.isSuccess, orgHasIssues]);

  const orgWideEmpty = useMemo(() => {
    if (!showEmpty) return false;
    if (isDefaultAllUnfiltered(listParams)) return true;
    if (!probeQuery.isSuccess) return false;
    return !orgHasIssues;
  }, [showEmpty, listParams, probeQuery.isSuccess, orgHasIssues]);

  const onRefresh = () => {
    listQuery.refetch();
    if (needsProbe) void probeQuery.refetch();
  };

  const filterCount = listParams.statuses.length + listParams.priorities.length;

  const toggleStatus = (s: string) => {
    setListParams((p) => {
      const next = p.statuses.includes(s) ? p.statuses.filter((x) => x !== s) : [...p.statuses, s];
      return { ...p, statuses: next };
    });
  };

  const togglePriority = (pr: string) => {
    setListParams((p) => {
      const next = p.priorities.includes(pr)
        ? p.priorities.filter((x) => x !== pr)
        : [...p.priorities, pr];
      return { ...p, priorities: next };
    });
  };

  const resetFiltersKeepScope = () => {
    setListParams((p) => defaultIssueListParams(p.scope));
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <Text variant="headlineSmall">Issues</Text>
        <SegmentedButtons
          value={listParams.scope}
          onValueChange={(v) => setListParams((p) => ({ ...p, scope: v as IssueListScope }))}
          buttons={[
            { value: 'open', label: 'Open', disabled: !orgId },
            { value: 'all', label: 'All', disabled: !orgId },
            { value: 'mine', label: 'Mine', disabled: !orgId },
          ]}
        />
        <View style={styles.toolbar}>
          <Menu
            visible={sortMenuOpen}
            onDismiss={() => setSortMenuOpen(false)}
            anchor={
              <Button
                mode="outlined"
                compact
                onPress={() => setSortMenuOpen(true)}
                accessibilityLabel={`Sort issues. Current: ${sortMenuLabel(listParams)}`}
                style={styles.toolbarBtn}
                contentStyle={styles.toolbarBtnContent}
              >
                {sortMenuLabel(listParams)}
              </Button>
            }
          >
            <Menu.Item
              title="Recent activity"
              onPress={() => {
                setListParams((p) => setSort(p, 'updatedAt', 'desc'));
                setSortMenuOpen(false);
              }}
            />
            <Menu.Item
              title="Oldest activity"
              onPress={() => {
                setListParams((p) => setSort(p, 'updatedAt', 'asc'));
                setSortMenuOpen(false);
              }}
            />
            <Menu.Item
              title="Newest created"
              onPress={() => {
                setListParams((p) => setSort(p, 'createdAt', 'desc'));
                setSortMenuOpen(false);
              }}
            />
            <Menu.Item
              title="Oldest created"
              onPress={() => {
                setListParams((p) => setSort(p, 'createdAt', 'asc'));
                setSortMenuOpen(false);
              }}
            />
            <Menu.Item
              title="Priority (high first)"
              onPress={() => {
                setListParams((p) => setSort(p, 'priority', 'desc'));
                setSortMenuOpen(false);
              }}
            />
            <Menu.Item
              title="Priority (low first)"
              onPress={() => {
                setListParams((p) => setSort(p, 'priority', 'asc'));
                setSortMenuOpen(false);
              }}
            />
          </Menu>
          <Button
            mode="outlined"
            compact
            onPress={() => setFilterDialogOpen(true)}
            accessibilityLabel={
              filterCount > 0
                ? `Filter issues, ${filterCount} active`
                : 'Filter issues by status and priority'
            }
            style={styles.toolbarBtn}
            contentStyle={styles.toolbarBtnContent}
          >
            Filters{filterCount > 0 ? ` (${filterCount})` : ''}
          </Button>
          {hasActiveFilters(listParams) ? (
            <IconButton
              icon="filter-off"
              accessibilityLabel="Reset filters"
              onPress={resetFiltersKeepScope}
              hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
            />
          ) : null}
        </View>
      </View>

      <Portal>
        <Dialog visible={filterDialogOpen} onDismiss={() => setFilterDialogOpen(false)}>
          <Dialog.Title>Filters</Dialog.Title>
          <Dialog.ScrollArea style={styles.dialogScroll}>
            <ScrollView>
              <Text variant="labelLarge" style={styles.dialogSection}>
                Status
              </Text>
              {issueStatuses.map((s) => (
                <Checkbox.Item
                  key={s}
                  label={s.replace(/_/g, ' ')}
                  status={listParams.statuses.includes(s) ? 'checked' : 'unchecked'}
                  onPress={() => toggleStatus(s)}
                />
              ))}
              <Text variant="labelLarge" style={styles.dialogSection}>
                Priority
              </Text>
              {issuePriorities.map((pr) => (
                <Checkbox.Item
                  key={pr}
                  label={pr}
                  status={listParams.priorities.includes(pr) ? 'checked' : 'unchecked'}
                  onPress={() => togglePriority(pr)}
                />
              ))}
            </ScrollView>
          </Dialog.ScrollArea>
          <Dialog.Actions>
            <Button
              onPress={() => {
                setListParams((p) => ({ ...p, statuses: [], priorities: [] }));
              }}
            >
              Clear
            </Button>
            <Button onPress={() => setFilterDialogOpen(false)}>Done</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

      {!orgId ? (
        <Banner
          visible
          icon="account-alert-outline"
          accessibilityLiveRegion="polite"
          actions={[{ label: 'Sign in', onPress: () => router.replace('/(auth)') }]}
        >
          Organization is not loaded. Sign in again to view issues.
        </Banner>
      ) : null}

      {orgId && listQuery.error ? (
        <Banner
          visible
          accessibilityLiveRegion="polite"
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
              title={Platform.OS === 'ios' ? 'Pull to refresh' : undefined}
              accessibilityLabel="Refresh issue list"
            />
          }
          contentContainerStyle={styles.listContent}
        />
      ) : null}

      {orgId && showEmpty && listQuery.isSuccess ? (
        <View style={styles.emptyWrap}>
          {probeLoading ? (
            <ActivityIndicator accessibilityLabel="Checking for other issues" />
          ) : filteredOrScopedEmpty ? (
            <View style={styles.emptyCard}>
              <Text variant="bodyLarge">
                {hasActiveFilters(listParams) ? 'Nothing matches filters' : 'Nothing in this queue'}
              </Text>
              <Text variant="bodyMedium" style={[styles.muted, { color: theme.colors.onSurfaceVariant }]}>
                {hasActiveFilters(listParams)
                  ? 'Try clearing filters or view all issues.'
                  : 'Try another scope or view all issues.'}
              </Text>
              <Button mode="contained" onPress={resetFiltersKeepScope} style={styles.emptyBtn}>
                Reset filters
              </Button>
              <Button mode="outlined" onPress={() => setListParams(defaultIssueListParams('all'))} style={styles.emptyBtn}>
                View all
              </Button>
            </View>
          ) : orgWideEmpty ? (
            <View style={styles.emptyCard}>
              <Text variant="bodyLarge">No issues yet</Text>
              <Text variant="bodyMedium" style={[styles.muted, { color: theme.colors.onSurfaceVariant }]}>
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
        accessibilityLabel="Create new issue"
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
  toolbar: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 8 },
  toolbarBtn: { alignSelf: 'flex-start' },
  toolbarBtnContent: { minHeight: 44, justifyContent: 'center' },
  muted: {},
  fab: { position: 'absolute', right: 16, bottom: 24 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  listContent: { paddingBottom: 88 },
  emptyWrap: { flex: 1, justifyContent: 'center', paddingHorizontal: 24 },
  emptyCard: { gap: 12, alignItems: 'flex-start' },
  emptyBtn: { marginTop: 4 },
  dialogScroll: { maxHeight: 360 },
  dialogSection: { marginTop: 8, marginBottom: 4, paddingHorizontal: 8 },
});
