import { useLocalSearchParams, useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { Appbar, Button, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

const ISSUES_HREF = '/(app)/(tabs)/issues' as const;

export default function PushLinkErrorScreen() {
  const theme = useTheme();
  const router = useRouter();
  const { reason } = useLocalSearchParams<{ reason?: string | string[] }>();
  const r = typeof reason === 'string' ? reason : reason?.[0];

  const title = r === 'wrong_org' ? 'Different organization' : 'Link problem';
  const body =
    r === 'wrong_org'
      ? 'This notification belongs to another organization than the one you are signed into. Open Issues to continue with your current workspace.'
      : 'This notification does not contain a valid issue link. Open Issues to return to your queue.';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.BackAction
          accessibilityLabel="Go back"
          onPress={() => router.replace(ISSUES_HREF)}
        />
        <Appbar.Content title={title} />
      </Appbar.Header>
      <View style={styles.pad}>
        <Text variant="bodyLarge" style={styles.body}>
          {body}
        </Text>
        <Button mode="contained" onPress={() => router.replace(ISSUES_HREF)} style={styles.btn}>
          Back to issues
        </Button>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  pad: { padding: 16, gap: 16 },
  body: { marginBottom: 8 },
  btn: { alignSelf: 'flex-start' },
});
