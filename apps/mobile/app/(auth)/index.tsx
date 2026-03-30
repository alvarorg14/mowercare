import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';
import { Button, HelperText, Text, TextInput, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiProblemError } from '../../lib/http';
import { loginSchema, type LoginFormValues } from '../../lib/auth/login-schema';
import { useAuth } from '../../lib/auth-context';

export default function AuthWelcomeScreen() {
  const theme = useTheme();
  const { signIn } = useAuth();

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      organizationId: '',
      email: '',
      password: '',
    },
  });

  const mutation = useMutation({
    mutationFn: signIn,
  });

  const submit = handleSubmit((data) => mutation.mutateAsync(data));

  const problem =
    mutation.error instanceof ApiProblemError
      ? mutation.error
      : mutation.error instanceof Error
        ? mutation.error
        : null;

  return (
    <SafeAreaView
      style={[styles.safe, { backgroundColor: theme.colors.background }]}
      edges={['top', 'bottom']}
    >
      <View style={styles.container}>
        <Text variant="headlineMedium">MowerCare</Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          Sign in with your organization ID and employee credentials
        </Text>

        <Controller
          control={control}
          name="organizationId"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextInput
              label="Organization ID"
              accessibilityLabel="Organization ID"
              value={value}
              onBlur={onBlur}
              onChangeText={onChange}
              autoCapitalize="none"
              autoCorrect={false}
              error={!!errors.organizationId}
            />
          )}
        />
        <HelperText type="error" visible={!!errors.organizationId}>
          {errors.organizationId?.message}
        </HelperText>

        <Controller
          control={control}
          name="email"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextInput
              label="Email"
              accessibilityLabel="Email"
              value={value}
              onBlur={onBlur}
              onChangeText={onChange}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
              error={!!errors.email}
            />
          )}
        />
        <HelperText type="error" visible={!!errors.email}>
          {errors.email?.message}
        </HelperText>

        <Controller
          control={control}
          name="password"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextInput
              label="Password"
              accessibilityLabel="Password"
              value={value}
              onBlur={onBlur}
              onChangeText={onChange}
              secureTextEntry
              error={!!errors.password}
            />
          )}
        />
        <HelperText type="error" visible={!!errors.password}>
          {errors.password?.message}
        </HelperText>

        {problem ? (
          <HelperText type="error" visible style={styles.banner}>
            {problem instanceof ApiProblemError
              ? [problem.title, problem.detail, problem.code].filter(Boolean).join(' — ')
              : problem.message}
          </HelperText>
        ) : null}

        <Button
          mode="contained"
          onPress={() => void submit().catch(() => undefined)}
          disabled={mutation.isPending}
          loading={mutation.isPending}
        >
          Sign in
        </Button>

        {mutation.isError ? (
          <Button
            mode="outlined"
            onPress={() => void submit().catch(() => undefined)}
            disabled={mutation.isPending}
          >
            Retry
          </Button>
        ) : null}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  container: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
    gap: 4,
  },
  subtitle: { textAlign: 'center', opacity: 0.8, marginBottom: 12 },
  banner: { marginBottom: 8 },
});
