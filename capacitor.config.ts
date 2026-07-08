import type { CapacitorConfig } from '@capacitor/cli';

const appId = process.env.CAPACITOR_APP_ID || 'com.muses.player';

const config: CapacitorConfig = {
  appId,
  appName: 'muses',
  webDir: 'dist',
  loggingBehavior: 'none',
};

export default config;
