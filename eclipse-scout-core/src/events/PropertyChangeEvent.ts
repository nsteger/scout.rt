import {Event, EventEmitter} from '../index';

export default interface PropertyChangeEvent<SOURCE extends EventEmitter, PROP_TYPE> extends Event {
  source: SOURCE;
  propertyName: 'string';
  newValue: PROP_TYPE;
  oldValue: PROP_TYPE;
}
