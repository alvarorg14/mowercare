import { Redirect } from 'expo-router';
import { useAuth } from '../lib/auth-context';

export default function Index() {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) {
    return <Redirect href="/(app)" />;
  }
  return <Redirect href="/(auth)" />;
}
