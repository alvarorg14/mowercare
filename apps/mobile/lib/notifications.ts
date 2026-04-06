import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import type { EventSubscription } from 'expo-modules-core';
import { Platform } from 'react-native';

import { getSessionOrganizationId } from './auth/session';
import {
  registerDevicePushToken,
  revokeDevicePushToken,
  type DevicePushPlatform,
} from './device-token-api';

/** Last native FCM/APNs token registered with the API (for sign-out revoke). */
let cachedPushToken: string | null = null;

let pushTokenSubscription: EventSubscription | null = null;

/** Subscribes once: when Expo rolls the device token, re-register with the backend (AC7). */
export function ensureDevicePushTokenListener(): void {
  if (pushTokenSubscription != null) {
    return;
  }
  pushTokenSubscription = Notifications.addPushTokenListener(async (devicePushToken) => {
    const token = devicePushToken.data;
    if (!token || token.length < 10) {
      return;
    }
    const orgId = getSessionOrganizationId();
    if (!orgId) {
      return;
    }
    try {
      cachedPushToken = token;
      await registerDevicePushToken(orgId, token, mapPlatform());
    } catch {
      /* best-effort */
    }
  });
}

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

function mapPlatform(): DevicePushPlatform {
  if (Platform.OS === 'ios') return 'IOS';
  if (Platform.OS === 'android') return 'ANDROID';
  return 'UNKNOWN';
}

export type PushRegistrationResult =
  | { ok: true }
  | { ok: false; reason: 'simulator' | 'denied' | 'no_org' | 'error'; detail?: string };

/**
 * Requests permission, reads the device push token, and PUTs it to the API for the current org.
 * Safe to call on simulator (no-op / unavailable). Does not throw — returns result object.
 */
export async function registerDevicePushWithBackend(): Promise<PushRegistrationResult> {
  const orgId = getSessionOrganizationId();
  if (!orgId) {
    return { ok: false, reason: 'no_org' };
  }

  if (!Device.isDevice) {
    return { ok: false, reason: 'simulator' };
  }

  try {
    const { status: existing } = await Notifications.getPermissionsAsync();
    let finalStatus = existing;
    if (existing !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }
    if (finalStatus !== 'granted') {
      return { ok: false, reason: 'denied' };
    }

    const { data: token } = await Notifications.getDevicePushTokenAsync();
    if (!token || token.length < 10) {
      return { ok: false, reason: 'error', detail: 'Invalid token from device' };
    }

    cachedPushToken = token;
    await registerDevicePushToken(orgId, token, mapPlatform());
    return { ok: true };
  } catch (e) {
    const detail = e instanceof Error ? e.message : String(e);
    return { ok: false, reason: 'error', detail };
  }
}

/** Best-effort revoke on sign-out so stale tokens are not targeted. */
export async function revokeRegisteredDevicePushToken(): Promise<void> {
  const orgId = getSessionOrganizationId();
  const token = cachedPushToken;
  if (!orgId || !token) {
    return;
  }
  try {
    await revokeDevicePushToken(orgId, token);
    cachedPushToken = null;
  } catch {
    /* idempotent local sign-out */
  }
}
