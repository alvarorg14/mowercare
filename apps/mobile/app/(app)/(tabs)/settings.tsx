import { useRouter } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Appbar, List, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getSessionRole } from '../../../lib/auth/session';

export default function SettingsScreen() {
  const theme = useTheme();
  const router = useRouter();
  const role = getSessionRole();
  const teamDescription =
    role === 'ADMIN'
      ? 'Invite employees, roles, and access'
      : 'Who is on the team';
  const teamA11yLabel =
    role === 'ADMIN' ? 'Open team management' : 'Open team information';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: theme.colors.background }]} edges={['top']}>
      <Appbar.Header mode="center-aligned">
        <Appbar.Content title="Settings" />
      </Appbar.Header>

      <ScrollView contentContainerStyle={styles.scroll}>
        <Text variant="titleSmall" style={styles.section}>
          Organization
        </Text>
        <List.Section>
          <List.Item
            title="Organization profile"
            description="Name and organization ID"
            left={(props) => <List.Icon {...props} icon="office-building" />}
            onPress={() => router.push('/organization')}
            accessibilityLabel="Open organization profile"
          />
          <List.Item
            title="Team"
            description={teamDescription}
            left={(props) => <List.Icon {...props} icon="account-group" />}
            onPress={() => router.push('/team')}
            accessibilityLabel={teamA11yLabel}
          />
        </List.Section>
        <View style={styles.hint}>
          <Text variant="bodySmall" style={styles.muted}>
            Admin-only actions on Team are hidden for technicians.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  scroll: { paddingBottom: 32 },
  section: { paddingHorizontal: 16, paddingTop: 8, opacity: 0.85 },
  hint: { paddingHorizontal: 16, paddingTop: 8 },
  muted: { opacity: 0.75 },
});
