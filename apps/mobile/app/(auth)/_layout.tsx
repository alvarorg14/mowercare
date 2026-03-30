import { Redirect, Stack } from 'expo-router';

import { useAuth } from '../../lib/auth-context';

export default function AuthLayout() {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) {
    return <Redirect href="/(app)" />;
  }
  return <Stack screenOptions={{ headerShown: false }} />;
}
