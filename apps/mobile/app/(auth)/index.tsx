import { useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { Button, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../../lib/auth-context';

export default function AuthWelcomeScreen() {
  const theme = useTheme();
  const { signInPlaceholder } = useAuth();
  const router = useRouter();

  return (
    <SafeAreaView
      style={[styles.safe, { backgroundColor: theme.colors.background }]}
      edges={['top', 'bottom']}
    >
      <View style={styles.container}>
        <Text variant="headlineMedium">MowerCare</Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          Field service coordination
        </Text>
        <Button
          mode="contained"
          onPress={() => {
            signInPlaceholder();
            router.replace('/');
          }}
        >
          Continue (placeholder)
        </Button>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
    gap: 16,
  },
  subtitle: { textAlign: 'center', opacity: 0.8 },
});
