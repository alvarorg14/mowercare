import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import {
  Appbar,
  Button,
  HelperText,
  Menu,
  Snackbar,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import {
  issueCreateSchema,
  issuePriorities,
  issueStatuses,
  type IssueCreateFormValues,
} from '../../../lib/issue-create-schema';
import { createIssue } from '../../../lib/issue-api';
import { ApiProblemError } from '../../../lib/http';

function problemMessage(err: unknown): string {
  if (err instanceof ApiProblemError) {
    if (err.status === 404 && err.code === 'NOT_FOUND') {
      return err.detail ?? 'Assignee was not found in this organization.';
    }
    return err.detail ?? err.title ?? 'Something went wrong';
  }
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

function toPayload(values: IssueCreateFormValues) {
  return {
    title: values.title.trim(),
    description: values.description?.trim() || undefined,
    status: values.status,
    priority: values.priority,
    customerLabel: values.customerLabel?.trim() || undefined,
    siteLabel: values.siteLabel?.trim() || undefined,
    assigneeUserId: values.assigneeUserId?.trim() || undefined,
  };
}

export default function CreateIssueScreen() {
  const theme = useTheme();
  const router = useRouter();
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);
  const [priorityMenuOpen, setPriorityMenuOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{ message: string; visible: boolean }>({
    message: '',
    visible: false,
  });

  const { control, handleSubmit } = useForm<IssueCreateFormValues>({
    resolver: zodResolver(issueCreateSchema),
    defaultValues: {
      title: '',
      description: '',
      status: 'OPEN',
      priority: 'MEDIUM',
      customerLabel: '',
      siteLabel: '',
      assigneeUserId: '',
    },
  });

  const mutation = useMutation({
    mutationFn: createIssue,
    onSuccess: () => {
      Alert.alert('Issue created', 'Your issue was saved.', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    },
    onError: (err) => {
      setSnackbar({ message: problemMessage(err), visible: true });
    },
  });

  const onSubmit = (values: IssueCreateFormValues) => {
    mutation.mutate(toPayload(values));
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.BackAction onPress={() => router.back()} />
        <Appbar.Content title="New issue" />
      </Appbar.Header>

      <ScrollView
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
      >
        <Controller
          control={control}
          name="title"
          render={({ field: { onChange, onBlur, value }, fieldState }) => (
            <View>
              <TextInput
                label="Title"
                mode="outlined"
                value={value}
                onBlur={onBlur}
                onChangeText={onChange}
                error={!!fieldState.error}
              />
              <HelperText type="error" visible={!!fieldState.error}>
                {fieldState.error?.message}
              </HelperText>
            </View>
          )}
        />

        <Controller
          control={control}
          name="description"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextInput
              label="Description (optional)"
              mode="outlined"
              multiline
              numberOfLines={4}
              style={styles.multiline}
              value={value}
              onBlur={onBlur}
              onChangeText={onChange}
            />
          )}
        />

        <Text variant="labelLarge" style={styles.label}>
          Status
        </Text>
        <Controller
          control={control}
          name="status"
          render={({ field: { onChange, value } }) => (
            <Menu
              visible={statusMenuOpen}
              onDismiss={() => setStatusMenuOpen(false)}
              anchor={
                <Button mode="outlined" onPress={() => setStatusMenuOpen(true)}>
                  {value}
                </Button>
              }
            >
              {issueStatuses.map((s) => (
                <Menu.Item
                  key={s}
                  onPress={() => {
                    onChange(s);
                    setStatusMenuOpen(false);
                  }}
                  title={s}
                />
              ))}
            </Menu>
          )}
        />

        <Text variant="labelLarge" style={styles.label}>
          Priority
        </Text>
        <Controller
          control={control}
          name="priority"
          render={({ field: { onChange, value } }) => (
            <Menu
              visible={priorityMenuOpen}
              onDismiss={() => setPriorityMenuOpen(false)}
              anchor={
                <Button mode="outlined" onPress={() => setPriorityMenuOpen(true)}>
                  {value}
                </Button>
              }
            >
              {issuePriorities.map((p) => (
                <Menu.Item
                  key={p}
                  onPress={() => {
                    onChange(p);
                    setPriorityMenuOpen(false);
                  }}
                  title={p}
                />
              ))}
            </Menu>
          )}
        />

        <Controller
          control={control}
          name="customerLabel"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextInput
              label="Customer (optional)"
              mode="outlined"
              value={value}
              onBlur={onBlur}
              onChangeText={onChange}
            />
          )}
        />

        <Controller
          control={control}
          name="siteLabel"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextInput
              label="Site (optional)"
              mode="outlined"
              value={value}
              onBlur={onBlur}
              onChangeText={onChange}
            />
          )}
        />

        <Controller
          control={control}
          name="assigneeUserId"
          render={({ field: { onChange, onBlur, value }, fieldState }) => (
            <View>
              <TextInput
                label="Assignee user id (optional)"
                mode="outlined"
                autoCapitalize="none"
                value={value}
                onBlur={onBlur}
                onChangeText={onChange}
                error={!!fieldState.error}
              />
              <HelperText type="error" visible={!!fieldState.error}>
                {fieldState.error?.message}
              </HelperText>
            </View>
          )}
        />

        <Button
          mode="contained"
          loading={mutation.isPending}
          onPress={handleSubmit(onSubmit)}
          disabled={mutation.isPending}
          style={styles.submit}
        >
          Create issue
        </Button>
      </ScrollView>

      <Snackbar visible={snackbar.visible} onDismiss={() => setSnackbar((s) => ({ ...s, visible: false }))} duration={4000}>
        {snackbar.message}
      </Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  scroll: { padding: 16, paddingBottom: 32, gap: 8 },
  multiline: { minHeight: 100 },
  label: { marginTop: 8 },
  submit: { marginTop: 16 },
});
