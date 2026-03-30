import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, StyleSheet, View } from 'react-native';
import {
  ActivityIndicator,
  Appbar,
  Button,
  HelperText,
  Snackbar,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { orgProfileSchema, type OrgProfileFormValues } from '../../lib/auth/org-profile-schema';
import { getSessionOrganizationId, getSessionRole } from '../../lib/auth/session';
import { ApiProblemError } from '../../lib/http';
import {
  fetchOrganizationProfile,
  organizationProfileQueryKey,
  patchOrganizationProfile,
} from '../../lib/org-profile-api';

function problemMessage(err: unknown): string {
  if (err instanceof ApiProblemError) {
    return err.detail ?? err.title ?? 'Something went wrong';
  }
  if (err instanceof Error) return err.message;
  return 'Something went wrong';
}

export default function OrganizationProfileScreen() {
  const theme = useTheme();
  const router = useRouter();
  const queryClient = useQueryClient();
  const orgId = getSessionOrganizationId();
  const isAdmin = getSessionRole() === 'ADMIN';

  const [snackbar, setSnackbar] = useState<{ message: string; visible: boolean }>({
    message: '',
    visible: false,
  });

  const profileQuery = useQuery({
    queryKey: orgId ? organizationProfileQueryKey(orgId) : ['organization-profile', 'none'],
    queryFn: fetchOrganizationProfile,
    enabled: !!orgId,
  });

  const { control, handleSubmit, reset, formState } = useForm<OrgProfileFormValues>({
    resolver: zodResolver(orgProfileSchema),
    defaultValues: { name: '' },
  });

  useEffect(() => {
    if (profileQuery.data) {
      reset({ name: profileQuery.data.name });
    }
  }, [profileQuery.data, reset]);

  const saveMutation = useMutation({
    mutationFn: patchOrganizationProfile,
    onSuccess: (data) => {
      if (orgId) {
        queryClient.setQueryData(organizationProfileQueryKey(orgId), data);
        queryClient.invalidateQueries({ queryKey: organizationProfileQueryKey(orgId) });
      }
      reset({ name: data.name });
      setSnackbar({ message: 'Saved', visible: true });
    },
    onError: (err) => {
      setSnackbar({ message: problemMessage(err), visible: true });
    },
  });

  const onSubmit = (values: OrgProfileFormValues) => {
    saveMutation.mutate({ name: values.name.trim() });
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.BackAction onPress={() => router.back()} accessibilityLabel="Go back" />
        <Appbar.Content title="Organization" />
      </Appbar.Header>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {profileQuery.isPending && (
          <View style={styles.centered}>
            <ActivityIndicator size="large" />
            <Text variant="bodyMedium" style={styles.muted}>
              Loading…
            </Text>
          </View>
        )}

        {profileQuery.isError && (
          <View style={styles.block}>
            <Text variant="bodyLarge">{problemMessage(profileQuery.error)}</Text>
            <Button mode="contained" onPress={() => profileQuery.refetch()} style={styles.retry}>
              Retry
            </Button>
          </View>
        )}

        {profileQuery.data && (
          <View style={styles.block}>
            <Text variant="bodySmall" style={styles.muted}>
              Organization ID (read-only)
            </Text>
            <Text variant="bodyMedium" selectable>
              {profileQuery.data.id}
            </Text>

            <Controller
              control={control}
              name="name"
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  label="Organization name"
                  mode="outlined"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={!!formState.errors.name}
                  disabled={!isAdmin || saveMutation.isPending}
                  accessibilityLabel="Organization name"
                />
              )}
            />
            <HelperText type="error" visible={!!formState.errors.name}>
              {formState.errors.name?.message}
            </HelperText>

            {!isAdmin && (
              <Text variant="bodySmall" style={styles.hint}>
                Only organization admins can edit the name. Contact an admin to update it.
              </Text>
            )}

            {isAdmin && (
              <Button
                mode="contained"
                onPress={() => void handleSubmit(onSubmit)()}
                loading={saveMutation.isPending}
                disabled={saveMutation.isPending}
              >
                Save
              </Button>
            )}
          </View>
        )}
      </ScrollView>

      <Snackbar visible={snackbar.visible} onDismiss={() => setSnackbar((s) => ({ ...s, visible: false }))} duration={3000}>
        {snackbar.message}
      </Snackbar>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  scroll: { padding: 16, paddingBottom: 32 },
  block: { gap: 12 },
  centered: { alignItems: 'center', paddingVertical: 24, gap: 8 },
  muted: { opacity: 0.7 },
  retry: { marginTop: 8, alignSelf: 'flex-start' },
  hint: { opacity: 0.85, marginTop: 4 },
});
