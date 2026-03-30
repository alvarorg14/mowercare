import { useQuery } from '@tanstack/react-query';
import { StyleSheet, View } from 'react-native';
import { Button, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '../../lib/auth-context';
import { getApiBaseUrl } from '../../lib/config';

export default function AppHomeScreen() {
  const theme = useTheme();
  const { signOut } = useAuth();
  const shellQuery = useQuery({
    queryKey: ['shell-demo'],
    queryFn: async () => 'Shell ready',
  });

  const apiBase = getApiBaseUrl();

  return (
    <SafeAreaView
      style={[styles.safe, { backgroundColor: theme.colors.background }]}
      edges={['top', 'bottom']}
    >
      <View style={styles.container}>
        <Text variant="headlineSmall">App shell</Text>
        <Text variant="bodyMedium" style={styles.line}>
          TanStack Query:{' '}
          {shellQuery.isPending
            ? '…'
            : shellQuery.isError
              ? `Error: ${shellQuery.error instanceof Error ? shellQuery.error.message : 'Unknown'}`
              : shellQuery.data}
        </Text>
        <Text variant="bodySmall" style={styles.muted}>
          API base (config): {apiBase}
        </Text>
        <Button mode="outlined" onPress={() => signOut()}>
          Sign out
        </Button>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
    gap: 12,
  },
  line: { textAlign: 'center' },
  muted: { opacity: 0.7, textAlign: 'center' },
});
