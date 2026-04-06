import { useEffect, useState } from 'react';
import { View } from 'react-native';
import { Banner } from 'react-native-paper';

import { useAuth } from '../lib/auth-context';
import { ensureDevicePushTokenListener, registerDevicePushWithBackend } from '../lib/notifications';

/**
 * UX-DR16: consolidated push registration + dismissible banner when push is unavailable.
 */
export function PushNotificationSetup() {
  const { isAuthenticated, isRestoringSession } = useAuth();
  const [visible, setVisible] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!isAuthenticated || isRestoringSession) {
      return;
    }
    ensureDevicePushTokenListener();
    let cancelled = false;
    void (async () => {
      const r = await registerDevicePushWithBackend();
      if (cancelled) return;
      if (r.ok || r.reason === 'simulator' || r.reason === 'no_org') {
        return;
      }
      if (r.reason === 'denied') {
        setMessage(
          'Push notifications are off. You can still use the in-app Notifications tab for activity.',
        );
        setVisible(true);
        return;
      }
      setMessage(
        r.detail
          ? `Push registration failed (${r.detail}). In-app notifications still work.`
          : 'Push registration failed. In-app notifications still work.',
      );
      setVisible(true);
    })();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, isRestoringSession]);

  if (!visible) {
    return null;
  }

  return (
    <View>
      <Banner
        visible
        actions={[{ label: 'Dismiss', onPress: () => setVisible(false) }]}
      >
        {message}
      </Banner>
    </View>
  );
}
