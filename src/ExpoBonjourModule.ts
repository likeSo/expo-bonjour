import { NativeModule, requireNativeModule } from 'expo';

import { ExpoBonjourModuleEvents } from './ExpoBonjour.types';

declare class ExpoBonjourModule extends NativeModule<ExpoBonjourModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoBonjourModule>('ExpoBonjour');
