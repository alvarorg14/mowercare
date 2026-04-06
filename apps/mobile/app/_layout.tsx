import { Stack } from 'expo-router';
import { QueryClientProvider } from '@tanstack/react-query';
import { PaperProvider } from 'react-native-paper';
import { PushDeepLinkBootstrap } from '../components/PushDeepLinkBootstrap';
import { AuthProvider } from '../lib/auth-context';
import { queryClient } from '../lib/queryClient';
import { paperTheme } from '../lib/theme';

export default function RootLayout() {
  return (
    <PaperProvider theme={paperTheme}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <PushDeepLinkBootstrap />
          <Stack screenOptions={{ headerShown: false }} />
        </AuthProvider>
      </QueryClientProvider>
    </PaperProvider>
  );
}
