import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { FlatList, Modal, Platform, StyleSheet, View } from 'react-native';
import {
  ActivityIndicator,
  Appbar,
  Banner,
  List,
  Portal,
  Searchbar,
  Text,
  useTheme,
} from 'react-native-paper';

import {
  assignableUsersQueryKey,
  listAssignableUsers,
  type AssignableUserResponse,
} from '../lib/assignable-users-api';
import { getSessionOrganizationId } from '../lib/auth/session';
import { ApiProblemError } from '../lib/http';

type Props = {
  visible: boolean;
  onDismiss: () => void;
  /** Called when user selects a row; parent updates draft (no PATCH until Save). */
  onSelectUserId: (userId: string) => void;
};

export function pickAssigneePickerErrorMessage(err: unknown): string {
  if (err instanceof ApiProblemError) return err.detail ?? err.title ?? 'Request failed';
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

export function AssigneePicker({ visible, onDismiss, onSelectUserId }: Props) {
  const theme = useTheme();
  const orgId = getSessionOrganizationId();
  const [query, setQuery] = useState('');

  useEffect(() => {
    if (!visible) setQuery('');
  }, [visible]);

  const q = useQuery({
    queryKey: orgId ? assignableUsersQueryKey(orgId) : ['assignable-users', 'none'],
    queryFn: listAssignableUsers,
    enabled: visible && !!orgId,
  });

  const filtered = useMemo(() => {
    const list = q.data ?? [];
    const t = query.trim().toLowerCase();
    if (!t) return list;
    return list.filter((u) => u.email.toLowerCase().includes(t));
  }, [q.data, query]);

  const onPick = (u: AssignableUserResponse) => {
    onSelectUserId(u.id);
    setQuery('');
    onDismiss();
  };

  return (
    <Portal>
      <Modal
        visible={visible}
        animationType="slide"
        onRequestClose={onDismiss}
        onShow={() => setQuery('')}
        accessibilityViewIsModal={Platform.OS === 'ios'}
        accessibilityLabel="Choose assignee"
      >
        <View style={[styles.sheet, { backgroundColor: theme.colors.background }]}>
          <Appbar.Header mode="small">
            <Appbar.Action icon="close" accessibilityLabel="Close assignee picker" onPress={onDismiss} />
            <Appbar.Content title="Assign to" />
          </Appbar.Header>

          <Searchbar
            placeholder="Search by email"
            value={query}
            onChangeText={setQuery}
            accessibilityLabel="Search assignees by email"
          />

          {q.isLoading ? (
            <View style={styles.centered} accessibilityLabel="Loading assignees">
              <ActivityIndicator size="large" />
            </View>
          ) : null}

          {q.error ? (
            <Banner
              visible
              icon="alert-circle-outline"
              actions={[{ label: 'Retry', onPress: () => void q.refetch() }]}
            >
              {pickAssigneePickerErrorMessage(q.error)}
            </Banner>
          ) : null}

          {q.isSuccess && filtered.length === 0 ? (
            <View style={styles.pad}>
              <Text variant="bodyMedium">
                {q.data?.length === 0 ? 'No active team members to assign.' : 'No team members match.'}
              </Text>
            </View>
          ) : null}

          {q.isSuccess && filtered.length > 0 ? (
            <FlatList
              data={filtered}
              keyExtractor={(item) => item.id}
              renderItem={({ item }) => (
                <List.Item
                  title={item.email}
                  description={`${item.role} · ${item.accountStatus}`}
                  onPress={() => onPick(item)}
                  accessibilityLabel={`Assign to ${item.email}`}
                />
              )}
            />
          ) : null}
        </View>
      </Modal>
    </Portal>
  );
}

const styles = StyleSheet.create({
  sheet: { flex: 1, paddingTop: 8 },
  centered: { padding: 24, alignItems: 'center' },
  pad: { padding: 16 },
});
