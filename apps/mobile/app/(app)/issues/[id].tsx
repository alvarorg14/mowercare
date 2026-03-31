import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, View } from 'react-native';
import {
  Appbar,
  Banner,
  Button,
  Chip,
  Dialog,
  Menu,
  Portal,
  Snackbar,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getAccessToken, getSessionOrganizationId } from '../../../lib/auth/session';
import { getIssue, patchIssue, type IssueDetail, type IssueUpdatePayload } from '../../../lib/issue-api';
import { issuePriorities, issueStatuses } from '../../../lib/issue-create-schema';
import { ApiProblemError } from '../../../lib/http';
import { getSubjectFromAccessToken } from '../../../lib/jwt-org';
import { formatRelativeTimeUtc } from '../../../lib/relative-time';
import { issueStatusTokens } from '../../../lib/theme';
import { AssigneePicker } from '../../../components/AssigneePicker';

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

function buildPatch(prev: IssueDetail, draft: Draft): IssueUpdatePayload {
  const patch: IssueUpdatePayload = {};
  if (draft.title !== prev.title) patch.title = draft.title.trim();
  const descPrev = prev.description?.trim() ?? '';
  const descDraft = draft.description.trim();
  if (descDraft !== descPrev) {
    patch.description = descDraft === '' ? null : descDraft;
  }
  if (draft.status !== prev.status) patch.status = draft.status;
  if (draft.priority !== prev.priority) patch.priority = draft.priority;
  const assignPrev = prev.assigneeUserId ?? '';
  const assignDraft = draft.assigneeUserId.trim();
  if (assignDraft !== assignPrev) {
    patch.assigneeUserId = assignDraft === '' ? null : assignDraft;
  }
  const cPrev = prev.customerLabel?.trim() ?? '';
  const cDraft = draft.customerLabel.trim();
  if (cDraft !== cPrev) patch.customerLabel = cDraft === '' ? null : cDraft;
  const sPrev = prev.siteLabel?.trim() ?? '';
  const sDraft = draft.siteLabel.trim();
  if (sDraft !== sPrev) patch.siteLabel = sDraft === '' ? null : sDraft;
  return patch;
}

type Draft = {
  title: string;
  description: string;
  status: string;
  priority: string;
  customerLabel: string;
  siteLabel: string;
  assigneeUserId: string;
};

function draftFromDetail(d: IssueDetail): Draft {
  return {
    title: d.title,
    description: d.description ?? '',
    status: d.status,
    priority: d.priority,
    customerLabel: d.customerLabel ?? '',
    siteLabel: d.siteLabel ?? '',
    assigneeUserId: d.assigneeUserId ?? '',
  };
}

export default function IssueDetailScreen() {
  const theme = useTheme();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { id: idParam } = useLocalSearchParams<{ id: string | string[] }>();
  const issueId = normalizeIdParam(idParam);
  const orgId = getSessionOrganizationId();
  const idValid = UUID_RE.test(issueId);

  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);
  const [priorityMenuOpen, setPriorityMenuOpen] = useState(false);
  const [closeDialog, setCloseDialog] = useState(false);
  const [assignPickerOpen, setAssignPickerOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{
    visible: boolean;
    message: string;
    mutationRetry?: boolean;
  }>({ visible: false, message: '' });
  const lastFailedPatchRef = useRef<IssueUpdatePayload | null>(null);

  const detailQuery = useQuery({
    queryKey: ['issue', orgId, issueId],
    queryFn: () => getIssue(issueId),
    enabled: !!orgId && idValid,
  });

  const data = detailQuery.data;

  useEffect(() => {
    if (data && !editing) {
      setDraft(draftFromDetail(data));
    }
  }, [data, editing]);

  const enterEdit = useCallback(() => {
    if (data) {
      setDraft(draftFromDetail(data));
      setEditing(true);
    }
  }, [data]);

  const cancelEdit = useCallback(() => {
    if (data) setDraft(draftFromDetail(data));
    setEditing(false);
  }, [data]);

  const mutation = useMutation({
    mutationFn: (payload: IssueUpdatePayload) => patchIssue(issueId, payload),
    onSuccess: async () => {
      lastFailedPatchRef.current = null;
      await queryClient.invalidateQueries({ queryKey: ['issue', orgId, issueId] });
      await queryClient.invalidateQueries({ queryKey: ['issues'] });
      setEditing(false);
      setSnackbar({ visible: true, message: 'Saved', mutationRetry: false });
    },
    onError: (err, variables) => {
      lastFailedPatchRef.current = variables;
      setSnackbar({ visible: true, message: errorMessage(err), mutationRetry: true });
    },
  });

  const submitPatch = useCallback(() => {
    if (!data || !draft) return;
    const payload = buildPatch(data, draft);
    if (Object.keys(payload).length === 0) {
      setEditing(false);
      return;
    }
    mutation.mutate(payload);
  }, [data, draft, mutation]);

  const onChooseStatus = useCallback(
    (value: string) => {
      setStatusMenuOpen(false);
      if (!draft) return;
      if (value === 'CLOSED' && data?.status !== 'CLOSED') {
        setCloseDialog(true);
        return;
      }
      setDraft({ ...draft, status: value });
    },
    [draft, data?.status],
  );

  const confirmCloseIssue = useCallback(() => {
    setCloseDialog(false);
    if (draft) setDraft({ ...draft, status: 'CLOSED' });
  }, [draft]);

  const dismissCloseDialog = useCallback(() => {
    setCloseDialog(false);
  }, []);

  const assignToMe = useCallback(() => {
    if (!draft) return;
    const t = getAccessToken();
    const sid = t ? getSubjectFromAccessToken(t) : null;
    if (!sid) {
      setSnackbar({
        visible: true,
        message: 'Sign in again to assign yourself.',
        mutationRetry: false,
      });
      return;
    }
    setDraft({ ...draft, assigneeUserId: sid });
  }, [draft]);

  const unassign = useCallback(() => {
    if (!draft) return;
    setDraft({ ...draft, assigneeUserId: '' });
  }, [draft]);

  const titleText = data?.title ?? 'Issue';
  const idShort = issueId ? `${issueId.slice(0, 8)}…` : '';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        {editing ? (
          <Appbar.Action icon="close" accessibilityLabel="Cancel editing" onPress={cancelEdit} />
        ) : (
          <Appbar.BackAction onPress={() => router.back()} />
        )}
        <Appbar.Content title={titleText} subtitle={idShort} />
        {data && !editing ? (
          <Appbar.Action icon="pencil" accessibilityLabel="Edit issue" onPress={enterEdit} />
        ) : null}
        {editing ? (
          <Appbar.Action
            icon="check"
            accessibilityLabel="Save changes"
            disabled={mutation.isPending}
            onPress={submitPatch}
          />
        ) : null}
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

      {orgId && idValid && detailQuery.isSuccess && data && draft ? (
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          {editing ? (
            <View style={styles.form}>
              <TextInput
                label="Title"
                mode="outlined"
                value={draft.title}
                onChangeText={(t) => setDraft({ ...draft, title: t })}
              />
              <TextInput
                label="Description"
                mode="outlined"
                multiline
                value={draft.description}
                onChangeText={(t) => setDraft({ ...draft, description: t })}
              />
              <TextInput
                label="Customer"
                mode="outlined"
                value={draft.customerLabel}
                onChangeText={(t) => setDraft({ ...draft, customerLabel: t })}
              />
              <TextInput
                label="Site"
                mode="outlined"
                value={draft.siteLabel}
                onChangeText={(t) => setDraft({ ...draft, siteLabel: t })}
              />
              <View style={styles.row}>
                <Menu
                  visible={statusMenuOpen}
                  onDismiss={() => setStatusMenuOpen(false)}
                  anchor={
                    <Button mode="outlined" onPress={() => setStatusMenuOpen(true)}>
                      Status: {statusLabel(draft.status)}
                    </Button>
                  }
                >
                  {issueStatuses.map((s) => (
                    <Menu.Item key={s} title={statusLabel(s)} onPress={() => onChooseStatus(s)} />
                  ))}
                </Menu>
              </View>
              <View style={styles.row}>
                <Menu
                  visible={priorityMenuOpen}
                  onDismiss={() => setPriorityMenuOpen(false)}
                  anchor={
                    <Button mode="outlined" onPress={() => setPriorityMenuOpen(true)}>
                      Priority: {draft.priority}
                    </Button>
                  }
                >
                  {issuePriorities.map((p) => (
                    <Menu.Item
                      key={p}
                      title={p}
                      onPress={() => {
                        setPriorityMenuOpen(false);
                        setDraft({ ...draft, priority: p });
                      }}
                    />
                  ))}
                </Menu>
              </View>
              <View style={styles.assignRow}>
                <Button mode="contained-tonal" onPress={assignToMe}>
                  Assign to me
                </Button>
                <Button mode="outlined" onPress={() => setAssignPickerOpen(true)}>
                  Choose assignee
                </Button>
                <Button mode="outlined" onPress={unassign}>
                  Unassign
                </Button>
              </View>
              {mutation.isPending ? (
                <Text variant="bodyMedium" style={styles.muted}>
                  Saving…
                </Text>
              ) : null}
            </View>
          ) : (
            <>
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
            </>
          )}
        </ScrollView>
      ) : null}

      <AssigneePicker
        visible={assignPickerOpen}
        onDismiss={() => setAssignPickerOpen(false)}
        onSelectUserId={(userId) => {
          if (draft) setDraft({ ...draft, assigneeUserId: userId });
        }}
      />

      <Portal>
        <Dialog visible={closeDialog} onDismiss={dismissCloseDialog}>
          <Dialog.Title>Close issue?</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">Closed issues cannot be edited afterward.</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={dismissCloseDialog}>Cancel</Button>
            <Button onPress={confirmCloseIssue}>Close issue</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

      <Snackbar
        visible={snackbar.visible}
        onDismiss={() => {
          setSnackbar((s) => ({ ...s, visible: false }));
          lastFailedPatchRef.current = null;
        }}
        duration={snackbar.mutationRetry ? 8000 : 3000}
        action={
          snackbar.mutationRetry && lastFailedPatchRef.current
            ? {
                label: 'Retry',
                onPress: () => {
                  const p = lastFailedPatchRef.current;
                  if (p) mutation.mutate(p);
                },
              }
            : undefined
        }
      >
        {snackbar.message}
      </Snackbar>
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
  form: { gap: 12 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  assignRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
});
