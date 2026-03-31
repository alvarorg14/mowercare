import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { ActivityIndicator, ScrollView, StyleSheet, View } from 'react-native';
import { Appbar, Banner, Button, Chip, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getSessionOrganizationId } from '../../../lib/auth/session';
import { getIssue } from '../../../lib/issue-api';
import { ApiProblemError } from '../../../lib/http';
import { formatRelativeTimeUtc } from '../../../lib/relative-time';
import { issueStatusTokens } from '../../../lib/theme';

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function errorMessage(err: unknown): string {
  if (err instanceof ApiProblemError) return err.detail ?? err.title ?? 'Request failed';
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

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

function normalizeIdParam(id: string | string[] | undefined): string {
  if (typeof id === 'string') return id;
  if (Array.isArray(id) && id[0]) return id[0];
  return '';
}

export default function IssueDetailScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { id: idParam } = useLocalSearchParams<{ id: string | string[] }>();
  const issueId = normalizeIdParam(idParam);
  const orgId = getSessionOrganizationId();
  const idValid = UUID_RE.test(issueId);

  const detailQuery = useQuery({
    queryKey: ['issue', orgId, issueId],
    queryFn: () => getIssue(issueId),
    enabled: !!orgId && idValid,
  });

  const data = detailQuery.data;
  const titleText = data?.title ?? 'Issue';
  const idShort = issueId ? `${issueId.slice(0, 8)}…` : '';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.BackAction onPress={() => router.back()} />
        <Appbar.Content title={titleText} subtitle={idShort} />
      </Appbar.Header>

      {!orgId ? (
        <Banner
          visible
          icon="account-alert-outline"
          actions={[{ label: 'Sign in', onPress: () => router.replace('/(auth)') }]}
        >
          Organization is not loaded. Sign in again to view this issue.
        </Banner>
      ) : null}

      {orgId && !idValid ? (
        <View style={styles.pad}>
          <Text variant="bodyLarge">This link does not look like a valid issue id.</Text>
          <Button mode="contained" onPress={() => router.back()} style={styles.btn}>
            Back to issues
          </Button>
        </View>
      ) : null}

      {orgId && idValid && detailQuery.error ? (
        <Banner
          visible
          actions={[{ label: 'Retry', onPress: () => void detailQuery.refetch() }]}
          icon="alert-circle-outline"
        >
          {errorMessage(detailQuery.error)}
        </Banner>
      ) : null}

      {orgId && idValid && detailQuery.isLoading && !detailQuery.data ? (
        <View style={styles.centered} accessibilityLabel="Loading issue">
          <ActivityIndicator size="large" />
        </View>
      ) : null}

      {orgId && idValid && detailQuery.isSuccess && data ? (
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.chips}>
            <Chip
              mode="flat"
              compact
              textStyle={styles.chipText}
              style={[styles.chip, { backgroundColor: statusColor(data.status) + '22' }]}
              icon="circle-outline"
            >
              {statusLabel(data.status)}
            </Chip>
            <Chip mode="outlined" compact icon="flag-outline">
              {data.priority}
            </Chip>
          </View>

          {[data.customerLabel, data.siteLabel].filter(Boolean).length > 0 ? (
            <Text variant="bodyMedium" style={styles.muted}>
              {[data.customerLabel, data.siteLabel].filter(Boolean).join(' · ')}
            </Text>
          ) : null}

          <Text variant="bodyMedium">
            {data.assigneeLabel?.trim() ? data.assigneeLabel.trim() : 'Unassigned'}
          </Text>

          <View style={styles.times}>
            <Text variant="labelSmall" style={styles.muted}>
              Updated {formatRelativeTimeUtc(data.updatedAt)}
            </Text>
            <Text variant="labelSmall" style={styles.muted}>
              Created {formatRelativeTimeUtc(data.createdAt)}
            </Text>
          </View>

          <Text variant="titleSmall" style={styles.section}>
            Description
          </Text>
          {data.description?.trim() ? (
            <Text variant="bodyLarge" style={styles.body}>
              {data.description}
            </Text>
          ) : (
            <Text variant="bodyMedium" style={styles.muted}>
              No description
            </Text>
          )}
        </ScrollView>
      ) : null}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  pad: { padding: 24, gap: 16 },
  btn: { alignSelf: 'flex-start' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  scroll: { padding: 16, paddingBottom: 32, gap: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  chip: { minHeight: 32 },
  chipText: { fontSize: 12 },
  muted: { opacity: 0.85 },
  times: { gap: 4 },
  section: { marginTop: 8 },
  body: { marginTop: 4 },
});
