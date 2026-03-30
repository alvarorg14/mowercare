import type { ExpoConfig } from 'expo/config';

import { DEFAULT_API_BASE_URL } from './lib/config';

/** Public API origin for dev; physical devices need LAN IP or tunnel (not committed). */
const apiBaseUrl =
  process.env.EXPO_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;

const config: ExpoConfig = {
  name: 'mobile',
  slug: 'mobile',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'light',
  scheme: 'mowercare',
  splash: {
    image: './assets/splash-icon.png',
    resizeMode: 'contain',
    backgroundColor: '#ffffff',
  },
  ios: {
    supportsTablet: true,
  },
  android: {
    adaptiveIcon: {
      backgroundColor: '#E6F4FE',
      foregroundImage: './assets/android-icon-foreground.png',
      backgroundImage: './assets/android-icon-background.png',
      monochromeImage: './assets/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  web: {
    favicon: './assets/favicon.png',
  },
  plugins: ['expo-router'],
  extra: {
    apiBaseUrl,
  },
};

export default config;
