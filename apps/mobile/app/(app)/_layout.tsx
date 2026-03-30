import { Redirect, Stack } from 'expo-router';

import { useAuth } from '../../lib/auth-context';

export default function AppGroupLayout() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Redirect href="/(auth)" />;
  }
  return <Stack screenOptions={{ headerShown: false }} />;
}
