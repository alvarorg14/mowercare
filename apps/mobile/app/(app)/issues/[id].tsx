import { useLocalSearchParams, useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { Appbar, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

/**
 * Placeholder until Story 3.4 (issue detail). Row navigation lands here with `id`.
 */
export default function IssueDetailPlaceholderScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { id } = useLocalSearchParams<{ id: string }>();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.BackAction onPress={() => router.back()} />
        <Appbar.Content title="Issue" />
      </Appbar.Header>
      <View style={styles.body}>
        <Text variant="titleMedium">Issue detail</Text>
        <Text variant="bodyMedium" style={styles.muted}>
          Full detail and edits ship in the next story. Issue id:
        </Text>
        <Text variant="bodySmall" selectable style={styles.mono}>
          {typeof id === 'string' ? id : String(id)}
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  body: { padding: 24, gap: 8 },
  muted: { opacity: 0.75 },
  mono: { fontFamily: 'monospace' },
});
