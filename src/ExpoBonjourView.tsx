import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoBonjourViewProps } from './ExpoBonjour.types';

const NativeView: React.ComponentType<ExpoBonjourViewProps> =
  requireNativeView('ExpoBonjour');

export default function ExpoBonjourView(props: ExpoBonjourViewProps) {
  return <NativeView {...props} />;
}
