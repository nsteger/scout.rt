import {Session, Widget} from '../index';
import {PartialAndRequired} from '../types';

export default interface WidgetModel {
  /**
   * The parent widget.
   */
  parent: Widget;
  owner?: Widget;
  /**
   * If not specified, the session of the parent widget is used
   */
  session?: Session;
  objectType?: string | { new(): Widget };

  [property: string]: any; // TODO CGU necessary for variable model properties, required?
}

export type RefWidgetModel<T extends WidgetModel> = PartialAndRequired<T, 'parent', 'objectType'>;
