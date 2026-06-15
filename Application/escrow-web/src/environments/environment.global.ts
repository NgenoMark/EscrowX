import { environment as devEnv } from './environment';
import { environment as prodEnv } from './environment.prod';

// Flip this to true when you need production config.
const USE_PRODUCTION = false;

export const environment = USE_PRODUCTION ? prodEnv : devEnv;
