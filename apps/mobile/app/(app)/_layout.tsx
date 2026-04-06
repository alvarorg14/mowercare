import { Redirect, Stack } from 'expo-router';
import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { PushNotificationSetup } from '../../components/PushNotificationSetup';
import { useAuth } from '../../lib/auth-context';

export default function AppGroupLayout() {
  const { isAuthenticated, isRestoringSession } = useAuth();

  if (isRestoringSession) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (!isAuthenticated) {
    return <Redirect href="/(auth)" />;
  }
  return (
    <>
      <PushNotificationSetup />
      <Stack screenOptions={{ headerShown: false }} />
    </>
  );
}

const styles = StyleSheet.create({
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
});
