import { Stack } from 'expo-router';
import { QueryClientProvider } from '@tanstack/react-query';
import { PaperProvider } from 'react-native-paper';
import { AuthProvider } from '../lib/auth-context';
import { createQueryClient } from '../lib/queryClient';
import { paperTheme } from '../lib/theme';

const queryClient = createQueryClient();

export default function RootLayout() {
  return (
    <PaperProvider theme={paperTheme}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <Stack screenOptions={{ headerShown: false }} />
        </AuthProvider>
      </QueryClientProvider>
    </PaperProvider>
  );
}
