import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { FlatList, StyleSheet, View } from 'react-native';
import {
  ActivityIndicator,
  Appbar,
  Button,
  Chip,
  Dialog,
  Divider,
  IconButton,
  List,
  Menu,
  Portal,
  RadioButton,
  Snackbar,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { inviteUserFormSchema, type InviteUserFormValues } from '../../lib/organization-users-invite-schema';
import {
  createEmployeeUser,
  deactivateEmployeeUser,
  employeeUsersQueryKey,
  listEmployeeUsers,
  type EmployeeUserResponse,
  type UserRole,
  updateEmployeeRole,
} from '../../lib/organization-users-api';
import { getAccessToken, getSessionOrganizationId, getSessionRole } from '../../lib/auth/session';
import { getSubjectFromAccessToken } from '../../lib/jwt-org';
import { ApiProblemError } from '../../lib/http';

function problemMessage(err: unknown): string {
  if (err instanceof ApiProblemError) {
    return err.detail ?? err.title ?? 'Something went wrong';
  }
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

export default function TeamScreen() {
  const theme = useTheme();
  const router = useRouter();
  const queryClient = useQueryClient();
  const orgId = getSessionOrganizationId();
  const isAdmin = getSessionRole() === 'ADMIN';
  const accessToken = getAccessToken();
  const selfUserId = accessToken ? getSubjectFromAccessToken(accessToken) : null;

  const [snackbar, setSnackbar] = useState<{ message: string; visible: boolean }>({
    message: '',
    visible: false,
  });
  const [menuUserId, setMenuUserId] = useState<string | null>(null);
  const [inviteVisible, setInviteVisible] = useState(false);
  const [inviteTokenDialog, setInviteTokenDialog] = useState<string | null>(null);
  const [roleTarget, setRoleTarget] = useState<EmployeeUserResponse | null>(null);
  const [roleChoice, setRoleChoice] = useState<UserRole>('TECHNICIAN');
  const [deactivateTarget, setDeactivateTarget] = useState<EmployeeUserResponse | null>(null);

  const usersQuery = useQuery({
    queryKey: orgId ? employeeUsersQueryKey(orgId) : ['employee-users', 'none'],
    queryFn: listEmployeeUsers,
    enabled: !!orgId && isAdmin,
  });

  const inviteForm = useForm<InviteUserFormValues>({
    resolver: zodResolver(inviteUserFormSchema),
    defaultValues: { email: '', role: 'TECHNICIAN', initialPassword: '' },
  });

  const createMutation = useMutation({
    mutationFn: createEmployeeUser,
    onSuccess: (data) => {
      if (orgId) {
        queryClient.invalidateQueries({ queryKey: employeeUsersQueryKey(orgId) });
      }
      setInviteVisible(false);
      inviteForm.reset({ email: '', role: 'TECHNICIAN', initialPassword: '' });
      if (data.inviteToken) {
        setInviteTokenDialog(data.inviteToken);
      } else {
        setSnackbar({ message: 'User created', visible: true });
      }
    },
    onError: (err) => {
      setSnackbar({ message: problemMessage(err), visible: true });
    },
  });

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: UserRole }) =>
      updateEmployeeRole(userId, role),
    onSuccess: (_data, variables) => {
      if (orgId) {
        queryClient.invalidateQueries({ queryKey: employeeUsersQueryKey(orgId) });
      }
      setRoleTarget(null);
      const selfDemoted =
        selfUserId !== null &&
        variables.userId === selfUserId &&
        variables.role !== 'ADMIN';
      setSnackbar({
        message: selfDemoted
          ? 'Role updated. Admin actions may require signing in again.'
          : 'Role updated',
        visible: true,
      });
    },
    onError: (err) => {
      setSnackbar({ message: problemMessage(err), visible: true });
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: deactivateEmployeeUser,
    onSuccess: () => {
      if (orgId) {
        queryClient.invalidateQueries({ queryKey: employeeUsersQueryKey(orgId) });
      }
      setDeactivateTarget(null);
      setSnackbar({ message: 'User deactivated', visible: true });
    },
    onError: (err) => {
      setSnackbar({ message: problemMessage(err), visible: true });
    },
  });

  const onInviteSubmit = (values: InviteUserFormValues) => {
    const pwd = values.initialPassword?.trim();
    createMutation.mutate({
      email: values.email.trim(),
      role: values.role,
      ...(pwd ? { initialPassword: pwd } : {}),
    });
  };

  const openRoleDialog = (user: EmployeeUserResponse) => {
    setMenuUserId(null);
    if (user.accountStatus === 'DEACTIVATED') {
      setSnackbar({
        message: 'This account is deactivated. Role cannot be changed.',
        visible: true,
      });
      return;
    }
    setRoleChoice(user.role);
    setRoleTarget(user);
  };

  const confirmRoleChange = () => {
    if (!roleTarget) return;
    if (roleTarget.role === roleChoice) {
      setRoleTarget(null);
      return;
    }
    roleMutation.mutate({ userId: roleTarget.id, role: roleChoice });
  };

  const confirmDeactivate = () => {
    if (!deactivateTarget) return;
    deactivateMutation.mutate(deactivateTarget.id);
  };

  if (isAdmin && !orgId) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
        <Appbar.Header mode="center-aligned">
          <Appbar.BackAction onPress={() => router.back()} accessibilityLabel="Go back" />
          <Appbar.Content title="Team" />
        </Appbar.Header>
        <View style={styles.block}>
          <Text variant="bodyLarge" style={styles.muted}>
            Organization context is missing. Try signing out and signing in again, or go back.
          </Text>
          <Button mode="contained" onPress={() => router.back()} style={styles.retry}>
            Go back
          </Button>
        </View>
      </SafeAreaView>
    );
  }

  if (!isAdmin) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
        <Appbar.Header mode="center-aligned">
          <Appbar.BackAction onPress={() => router.back()} accessibilityLabel="Go back" />
          <Appbar.Content title="Team" />
        </Appbar.Header>
        <View style={styles.block}>
          <Text variant="bodyLarge" style={styles.muted}>
            Only organization admins can invite employees, change roles, or deactivate accounts.
          </Text>
          <Text variant="bodyMedium" style={styles.hint}>
            Contact an admin if you need changes to team access.
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.BackAction onPress={() => router.back()} accessibilityLabel="Go back" />
        <Appbar.Content title="Team" />
        <Appbar.Action
          icon="account-plus"
          accessibilityLabel="Invite or create user"
          onPress={() => setInviteVisible(true)}
        />
      </Appbar.Header>

      {usersQuery.isPending && (
        <View style={styles.centered}>
          <ActivityIndicator size="large" />
          <Text variant="bodyMedium" style={styles.muted}>
            Loading…
          </Text>
        </View>
      )}

      {usersQuery.isError && (
        <View style={styles.block}>
          <Text variant="bodyLarge">{problemMessage(usersQuery.error)}</Text>
          <Button mode="contained" onPress={() => usersQuery.refetch()} style={styles.retry}>
            Retry
          </Button>
        </View>
      )}

      {usersQuery.data && (
        <FlatList
          data={usersQuery.data}
          keyExtractor={(item) => item.id}
          contentContainerStyle={usersQuery.data.length === 0 ? styles.emptyList : styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text variant="bodyLarge" style={styles.muted}>
                No team members yet.
              </Text>
              <Text variant="bodyMedium" style={styles.hint}>
                Invite someone to join your organization.
              </Text>
              <Button mode="contained" onPress={() => setInviteVisible(true)} style={styles.emptyBtn}>
                Invite
              </Button>
            </View>
          }
          renderItem={({ item }) => {
            const isSelf = selfUserId !== null && item.id === selfUserId;
            const deactivated = item.accountStatus === 'DEACTIVATED';
            return (
              <List.Item
                title={item.email}
                titleStyle={deactivated ? { opacity: 0.65 } : undefined}
                description={
                  <View style={styles.chipRow}>
                    <Chip compact style={styles.chip} mode="outlined">
                      {item.role}
                    </Chip>
                    <Chip compact style={styles.chip} mode="flat">
                      {item.accountStatus}
                    </Chip>
                    {isSelf ? (
                      <Chip compact style={styles.chip}>
                        You
                      </Chip>
                    ) : null}
                  </View>
                }
                right={() => (
                  <Menu
                    visible={menuUserId === item.id}
                    onDismiss={() => setMenuUserId(null)}
                    anchor={
                      <IconButton
                        icon="dots-vertical"
                        accessibilityLabel={`Actions for ${item.email}`}
                        onPress={() => setMenuUserId(item.id)}
                      />
                    }
                  >
                    <Menu.Item
                      onPress={() => openRoleDialog(item)}
                      title="Change role"
                      disabled={deactivated}
                    />
                    <Menu.Item
                      onPress={() => {
                        setMenuUserId(null);
                        if (!deactivated) setDeactivateTarget(item);
                      }}
                      title="Deactivate"
                      disabled={deactivated}
                    />
                  </Menu>
                )}
              />
            );
          }}
          ItemSeparatorComponent={() => <Divider />}
        />
      )}

      <Portal>
        <Dialog visible={inviteVisible} onDismiss={() => !createMutation.isPending && setInviteVisible(false)}>
          <Dialog.Title>Invite or create user</Dialog.Title>
          <Dialog.Content>
            <Controller
              control={inviteForm.control}
              name="email"
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  label="Email"
                  mode="outlined"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                  error={!!inviteForm.formState.errors.email}
                  disabled={createMutation.isPending}
                  accessibilityLabel="Employee email"
                />
              )}
            />
            <Text variant="bodySmall" style={styles.fieldError}>
              {inviteForm.formState.errors.email?.message}
            </Text>
            <Text variant="labelLarge" style={styles.radioLabel}>
              Role
            </Text>
            <Controller
              control={inviteForm.control}
              name="role"
              render={({ field: { onChange, value } }) => (
                <RadioButton.Group
                  onValueChange={(v) => onChange(v as UserRole)}
                  value={value}
                >
                  <RadioButton.Item label="Technician" value="TECHNICIAN" disabled={createMutation.isPending} />
                  <RadioButton.Item label="Admin" value="ADMIN" disabled={createMutation.isPending} />
                </RadioButton.Group>
              )}
            />
            <Controller
              control={inviteForm.control}
              name="initialPassword"
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  label="Initial password (optional)"
                  mode="outlined"
                  value={value ?? ''}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  secureTextEntry
                  style={styles.passwordField}
                  disabled={createMutation.isPending}
                  accessibilityLabel="Initial password optional"
                />
              )}
            />
            <Text variant="bodySmall" style={styles.fieldError}>
              {inviteForm.formState.errors.initialPassword?.message}
            </Text>
            <Text variant="bodySmall" style={styles.muted}>
              Leave password empty to send an invite (pending) instead of an active account.
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setInviteVisible(false)} disabled={createMutation.isPending}>
              Cancel
            </Button>
            <Button
              loading={createMutation.isPending}
              disabled={createMutation.isPending}
              onPress={() => void inviteForm.handleSubmit(onInviteSubmit)()}
            >
              Submit
            </Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={!!roleTarget} onDismiss={() => !roleMutation.isPending && setRoleTarget(null)}>
          <Dialog.Title>Change role</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium" style={styles.dialogEmail}>
              {roleTarget?.email}
            </Text>
            <RadioButton.Group
              onValueChange={(v) => setRoleChoice(v as UserRole)}
              value={roleChoice}
            >
              <RadioButton.Item label="Technician" value="TECHNICIAN" disabled={roleMutation.isPending} />
              <RadioButton.Item label="Admin" value="ADMIN" disabled={roleMutation.isPending} />
            </RadioButton.Group>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setRoleTarget(null)} disabled={roleMutation.isPending}>
              Cancel
            </Button>
            <Button loading={roleMutation.isPending} onPress={confirmRoleChange}>
              Save
            </Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={!!deactivateTarget} onDismiss={() => !deactivateMutation.isPending && setDeactivateTarget(null)}>
          <Dialog.Title>Deactivate user?</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">
              {deactivateTarget?.email} will lose access immediately. This cannot be undone from the app.
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDeactivateTarget(null)} disabled={deactivateMutation.isPending}>
              Cancel
            </Button>
            <Button
              buttonColor={theme.colors.error}
              textColor={theme.colors.onError ?? '#FFFFFF'}
              loading={deactivateMutation.isPending}
              onPress={confirmDeactivate}
            >
              Deactivate
            </Button>
          </Dialog.Actions>
        </Dialog>

        <Dialog visible={!!inviteTokenDialog} onDismiss={() => setInviteTokenDialog(null)}>
          <Dialog.Title>Invite ready</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium" style={styles.muted}>
              Share this invite token with the user once. It is not shown again.
            </Text>
            <Text variant="bodyLarge" selectable style={styles.token}>
              {inviteTokenDialog}
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setInviteTokenDialog(null)}>Done</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>

      <Snackbar
        visible={snackbar.visible}
        onDismiss={() => setSnackbar((s) => ({ ...s, visible: false }))}
        duration={4000}
      >
        {snackbar.message}
      </Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  list: { paddingBottom: 32 },
  emptyList: { flexGrow: 1 },
  block: { padding: 16, gap: 12 },
  centered: { alignItems: 'center', paddingVertical: 24, gap: 8 },
  empty: { padding: 24, alignItems: 'center', gap: 12 },
  emptyBtn: { marginTop: 8 },
  muted: { opacity: 0.75 },
  hint: { opacity: 0.85, marginTop: 4 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 4 },
  chip: { alignSelf: 'flex-start' },
  retry: { alignSelf: 'flex-start' },
  fieldError: { color: '#B3261E', minHeight: 18 },
  passwordField: { marginTop: 8 },
  radioLabel: { marginTop: 8, marginBottom: 4 },
  dialogEmail: { marginBottom: 8 },
  token: { marginTop: 12, fontFamily: 'monospace' },
});
