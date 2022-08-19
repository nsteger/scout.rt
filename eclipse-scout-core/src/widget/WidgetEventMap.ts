import {Event, EventMap, PropertyChangeEvent, Widget} from '../index';
import {GlassPaneContribution} from './Widget';

export interface WidgetEvent extends Event {
  source: Widget;
}

export interface HierarchyChangeEvent extends WidgetEvent {
  oldParent: Widget;
  parent: Widget;
}

export interface GlassPaneContributionEvent extends WidgetEvent {
  contribution: GlassPaneContribution;
}

export default interface WidgetEventMap extends EventMap {
  'init': WidgetEvent;
  'destroy': WidgetEvent;
  'render': WidgetEvent;
  'remove': WidgetEvent;
  'removing': WidgetEvent;
  'glassPaneContributionAdded': GlassPaneContributionEvent;
  'glassPaneContributionRemoved': GlassPaneContributionEvent;
  'hierarchyChange': HierarchyChangeEvent;
  'propertyChange': PropertyChangeEvent<Widget, any>;
  'propertyChange:enabled': PropertyChangeEvent<Widget, boolean>;
}
