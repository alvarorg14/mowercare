import * as Notifications from 'expo-notifications';
import { useRouter } from 'expo-router';
import { useEffect, useRef } from 'react';

import { useAuth } from '../lib/auth-context';
import { getSessionOrganizationId } from '../lib/auth/session';
import {
  buildPushResponseDedupeKey,
  flushPendingPushDeepLink,
  handlePushNotificationOpen,
} from '../lib/push-navigation';

/**
 * Story 4.5: global notification response handling (auth + cold start + pending queue).
 * Mounted under AuthProvider so useAuth / router work.
 */
export function PushDeepLinkBootstrap() {
  const router = useRouter();
  const { isAuthenticated, isRestoringSession } = useAuth();

  const authRef = useRef({ isAuthenticated, isRestoringSession });
  authRef.current = { isAuthenticated, isRestoringSession };

  const coldStartCheckedRef = useRef(false);
  const coldStartHandledKeyRef = useRef<string | null>(null);
  const coldStartHandledAtRef = useRef(0);

  useEffect(() => {
    const sub = Notifications.addNotificationResponseReceivedListener((response) => {
      const { isAuthenticated: authed, isRestoringSession: restoring } = authRef.current;
      if (restoring) return;
      const key = buildPushResponseDedupeKey(response);
      if (
        coldStartHandledKeyRef.current === key &&
        Date.now() - coldStartHandledAtRef.current < 4000
      ) {
        return;
      }
      handlePushNotificationOpen(response, {
        router,
        isAuthenticated: authed,
        isRestoringSession: false,
        sessionOrgId: getSessionOrganizationId(),
        source: 'listener',
      });
    });
    return () => sub.remove();
  }, [router]);

  useEffect(() => {
    if (isRestoringSession || coldStartCheckedRef.current) return;
    coldStartCheckedRef.current = true;
    let cancelled = false;
    void Notifications.getLastNotificationResponseAsync().then((last) => {
      if (cancelled || !last) return;
      const key = buildPushResponseDedupeKey(last);
      coldStartHandledKeyRef.current = key;
      coldStartHandledAtRef.current = Date.now();
      handlePushNotificationOpen(last, {
        router,
        isAuthenticated: authRef.current.isAuthenticated,
        isRestoringSession: false,
        sessionOrgId: getSessionOrganizationId(),
        source: 'cold_start',
      });
    });
    return () => {
      cancelled = true;
    };
  }, [isRestoringSession, router]);

  useEffect(() => {
    if (!isAuthenticated || isRestoringSession) return;
    flushPendingPushDeepLink({
      router,
      isAuthenticated,
      sessionOrgId: getSessionOrganizationId(),
    });
  }, [isAuthenticated, isRestoringSession, router]);

  return null;
}
