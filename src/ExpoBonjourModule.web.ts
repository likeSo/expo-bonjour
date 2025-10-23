import { registerWebModule, NativeModule } from 'expo';

import { ExpoBonjourModuleEvents } from './ExpoBonjour.types';

class ExpoBonjourModule extends NativeModule<ExpoBonjourModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoBonjourModule, 'ExpoBonjourModule');
