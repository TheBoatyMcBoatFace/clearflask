import windowIso from "../windowIso";

var envCache: Environment | undefined = undefined;

enum Environment {
  DEVELOPMENT_FRONTEND = 'FRONTEND',
  DEVELOPMENT_LOCAL = 'LOCAL',
  PRODUCTION = 'PROD',
}

function detectEnv(): Environment {
  if (envCache === undefined) {
    if (process?.env?.NODE_ENV === 'development' /* npm run start:frontend */
      || windowIso['ENV'] === 'development' /* npm run start:dev */) {
      envCache = Environment.DEVELOPMENT_FRONTEND;
    } else if (windowIso.location.hostname.endsWith('localhost.com') /* fallback */
      || windowIso.location.hostname.endsWith('localhost') /* fallback */
      || windowIso['ENV'] === 'local' /* npm run start:local */) {
      envCache = Environment.DEVELOPMENT_LOCAL;
    } else {
      envCache = Environment.PRODUCTION;
    }
  }
  return envCache;
}

function isProd(): boolean {
  return detectEnv() === Environment.PRODUCTION;
}

function isTracking(): boolean {
  return !isDoNotTrack() && isProd();
}

function isDoNotTrack(): boolean {
  return windowIso.isSsr
    || windowIso.navigator.doNotTrack === "yes"
    || windowIso.navigator.doNotTrack === "1"
    || windowIso.navigator['msDoNotTrack'] === "1"
    || windowIso.doNotTrack === "yes"
    || windowIso.doNotTrack === "1"
    || windowIso['msDoNotTrack'] === "1";
}

export { isProd, isTracking, detectEnv, Environment };

