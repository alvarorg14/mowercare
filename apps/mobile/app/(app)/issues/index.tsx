import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { FAB, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function IssuesHomeScreen() {
  const theme = useTheme();
  const router = useRouter();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <Text variant="headlineSmall">Issues</Text>
        <Text variant="bodyMedium" style={styles.muted}>
          Your queue will show here in a future update. Use New issue to capture work now.
        </Text>
      </View>
      <FAB
        icon="plus"
        label="New issue"
        style={[styles.fab, { backgroundColor: theme.colors.primary }]}
        onPress={() => router.push('/issues/create')}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    paddingHorizontal: 24,
    paddingTop: 8,
    gap: 8,
  },
  muted: { opacity: 0.75 },
  fab: {
    position: 'absolute',
    right: 16,
    bottom: 24,
  },
});
