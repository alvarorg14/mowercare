import { Redirect } from 'expo-router';

/** Default signed-in entry: Issues tab (UX-DR11). */
export default function AppEntryRedirect() {
  return <Redirect href="/(app)/(tabs)/issues" />;
}
