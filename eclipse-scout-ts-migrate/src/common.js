export function lfToCrlf(text) {
  return text.replace(/(?!\r)\n/gm, '\r\n');
}

export function crlfToLf(text) {
  return text.replace(/\r\n/gm, '\n');
}

export function inConstructor(path) {
  return !!findParentPath(path, parentPath => parentPath.node.type === 'ClassMethod' && parentPath.node.kind === 'constructor');
}

export function findParentClassBody(path) {
  return findParentPath(path, parentPath => parentPath.node.type === 'ClassBody');
}

export function findParentPath(path, predicate) {
  let cur = path;
  while (cur.node.type !== 'Program') {
    if (predicate(cur)) {
      return cur;
    }
    cur = cur.parentPath;
  }
  return undefined;
}

export function getTypeFor(j, name, value, typeMaps) {
  switch (value.type) {
    case 'StringLiteral':
      return j.tsStringKeyword();
    case 'BooleanLiteral':
      return j.tsBooleanKeyword();
    case 'NumericLiteral':
      return j.tsNumberKeyword();
    case 'ArrayExpression':
      let elementType = j.tsAnyKeyword();

      if (value.elements.length > 0) {
        elementType = getTypeFor(j, null, value.elements[0]);
      }
      return j.tsArrayType(elementType);
    default:
      let type = findType(j, typeMaps, name);
      if (type) {
        return type;
      }
      console.log(`unknown type ${value.type} for name ${name}`);
      return j.tsAnyKeyword();
  }
}

export function mapType(j, name) {
  if (name.startsWith('ref:')) {
    name = name.substring(4, name.length);
    return j.tsTypeReference(j.identifier(name));
  }
  if (name.endsWith('[]')) {
    name = name.substring(0, name.length - 2);
    return j.tsArrayType(mapType(j, name));
  }
  switch (name) {
    case 'string':
      return j.tsStringKeyword();
    case 'boolean':
      return j.tsBooleanKeyword();
    case 'number':
      return j.tsNumberKeyword();
    case 'any':
      return j.tsAnyKeyword();
    default:
      return null;
  }
}

export function findType(j, typeMaps, name) {
  if (!name) {
    return;
  }
  for (let map of typeMaps) {
    if (map.predicate(name)) {
      return mapType(j, map.type);
    }
  }
  // Ignore leading _ and try again
  if (name.startsWith('_')) {
    name = name.substring(1, name.length);
  }
  for (let map of typeMaps) {
    if (map.predicate(name)) {
      return mapType(j, map.type);
    }
  }
}

export function createType(j, typeMaps, name) {
  let type = findType(j, typeMaps, name);
  if (type) {
    return j.tsTypeAnnotation(type);
  }
}

export function getJQueryTypeRef(j) {
  return j.tsTypeReference(j.identifier('JQuery'));
}

export function addDefaultImport(j, root, importName, moduleName) {
  const newImport = j.importDeclaration(
    [j.importDefaultSpecifier(j.identifier(importName))],
    j.stringLiteral(moduleName)
  );

  // Insert it at the top of the file
  root.get().node.program.body.unshift(newImport);
}

export function methodFilter(j, path) {
  return path.node.type === j.ClassMethod.name ||
    // All exported methods in a file that is not a class (e.g. utilities)
    (!findParentClassBody(path) && path.node?.type === 'FunctionDeclaration' && path.parentPath.node.type === 'ExportNamedDeclaration');
}

export const defaultParamTypeMap = {
  JQuery: {
    predicate: name => name.startsWith('$'),
    type: 'ref:JQuery'
  },
  HtmlComponent: {
    predicate: name => isOneOf(name, 'htmlComp', 'htmlContainer', 'htmlBody'),
    type: 'ref:HtmlComponent'
  },
  Session: {
    predicate: name => name === 'session',
    type: 'ref:Session'
  },
  number: {
    predicate: name => isOneOf(name, 'width', 'height', 'top', 'bottom', 'right', 'left', 'x', 'y', 'length', 'maximumUploadSize', 'viewRangeSize', 'count', 'selectionStart', 'selectionEnd',
        'sortCode', 'dense', 'delay', 'maxContentLines', 'useOnlyInVisibleColumns', 'index')
      || name.endsWith('Length') || name.endsWith('Width') || name.endsWith('Height') || name.endsWith('WidthInPixel') || name.endsWith('Count')
      || name.endsWith('Top') || name.endsWith('Left') || name.endsWith('Index') || name.endsWith('ingX') || name.endsWith('ingY') || name.endsWith('Delay'),
    type: 'number'
  },
  boolean: {
    predicate: name => isOneOf(name, 'loading', 'loaded', 'toggleAction', 'compact', 'exclusiveExpand', 'active', 'visible', 'enabled', 'checked', 'selected', 'selectable', 'hasText', 'invalidate', 'modal', 'closable', 'resizable',
        'movable', 'askIfNeedSave', 'showOnOpen', 'scrollable', 'updateDisplayTextOnModify', 'autoRemove', 'mandatory', 'suppressStatus', 'stackable', 'shrinkable', 'required', 'collapsed', 'collapsible', 'expanded', 'expandable',
        'editable', 'preventDoubleClick', 'autoCloseExternalWindow', 'hasDate', 'hasTime', 'focused', 'responsive', 'wrapText', 'tabbable', 'virtual', 'busy', 'trimText', 'browseAutoExpandAll', 'browseLoadIncremental', 'browseHierarchy',
        'minimized', 'maximized', 'failed', 'running', 'stopped', 'requestPending', 'pending', 'inputMasked', 'formatLower', 'formatUpper', 'marked', 'overflown', 'multiSelect', 'multiCheck', 'scrollToSelection', 'trackLocation',
        'autoFit', 'multiline', 'multilineText', 'hierarchical', 'loadIncremental', 'hidden', 'hiddenByUi', 'shown', 'withArrow', 'trimWidth', 'trimHeight', 'autoResizeColumns', 'filterAccepted', 'withPlaceholders',
        'clickable', 'empty', 'changing', 'inheritAccessibility', 'embedDetailContent', 'displayable', 'compacted', 'autoOptimizeWidth')
      || name.endsWith('Visible') || name.endsWith('Enabled') || name.endsWith('Focused') || name.endsWith('Required') || name.endsWith('Collapsed')
      || name.endsWith('Minimized') || name.endsWith('Focusable') || name.endsWith('Active') || name.endsWith('Expanded'),
    type: 'boolean'
  },
  string: {
    predicate: name => isOneOf(name, 'displayText', 'text', 'cssClass', 'displayViewId', 'title', 'subTitle', 'subtitle', 'titleSuffix', 'iconId', 'label', 'subLabel', 'imageUrl', 'logoUrl', 'titleSuffix')
      || name.endsWith('IconId') || name.endsWith('CssClass'),
    type: 'string'
  },
  Date: {
    predicate: name => name.endsWith('Date') || name.endsWith('Time'),
    type: 'ref:Date'
  },
  Actions: {
    predicate: name => isOneOf(name, 'actions'),
    type: 'ref:Action[]'
  },
  Popup: {
    predicate: name => isOneOf(name, 'popup'),
    type: 'ref:Popup'
  },
  Popups: {
    predicate: name => isOneOf(name, 'popups'),
    type: 'ref:Popup[]'
  },
  Insets: {
    predicate: name => isOneOf(name, 'insets'),
    type: 'ref:Insets'
  },
  IconDesc: {
    predicate: name => isOneOf(name, 'iconDesc'),
    type: 'ref:IconDesc'
  },
  Accordion: {
    predicate: name => isOneOf(name, 'accordion'),
    type: 'ref:Accordion'
  },
  BreadCrumbItem: {
    predicate: name => isOneOf(name, 'breadCrumbItem'),
    type: 'ref:BreadCrumbItem'
  },
  BreadCrumbItems: {
    predicate: name => isOneOf(name, 'breadCrumbItems'),
    type: 'ref:BreadCrumbItem[]'
  },
  Menu: {
    predicate: name => isOneOf(name, 'menu', 'menuItem'),
    type: 'ref:Menu'
  },
  Menus: {
    predicate: name => isOneOf(name, 'menus', 'menuItems', 'staticMenus', 'detailMenus', 'nodeMenus'),
    type: 'ref:Menu[]'
  },
  LookupCall: {
    predicate: name => isOneOf(name, 'lookupCall'),
    type: 'ref:LookupCall'
  },
  CodeType: {
    predicate: name => isOneOf(name, 'codeType'),
    type: 'ref:CodeType'
  },
  LookupRow: {
    predicate: name => isOneOf(name, 'lookupRow'),
    type: 'ref:LookupRow'
  },
  LookupRows: {
    predicate: name => isOneOf(name, 'lookupRows'),
    type: 'ref:LookupRow[]'
  },
  LookupResult: {
    predicate: name => isOneOf(name, 'lookupResult'),
    type: 'ref:LookupResult'
  },
  FormField: {
    predicate: name => isOneOf(name, 'formField', 'field') || name.endsWith('Field'),
    type: 'ref:FormField'
  },
  FormFields: {
    predicate: name => isOneOf(name, 'formFields', 'fields') || name.endsWith('Fields'),
    type: 'ref:FormField[]'
  },
  Column: {
    predicate: name => isOneOf(name, 'column'),
    type: 'ref:Column'
  },
  Columns: {
    predicate: name => isOneOf(name, 'columns'),
    type: 'ref:Column[]'
  },
  GridData: {
    predicate: name => isOneOf(name, 'gridData', 'gridDataHints'),
    type: 'ref:GridData'
  },
  Form: {
    predicate: name => isOneOf(name, 'form', 'displayParent') || name.endsWith('Form'),
    type: 'ref:Form'
  },
  Status: {
    predicate: name => isOneOf(name, 'errorStatus'),
    type: 'ref:Status'
  },
  Outline: {
    predicate: name => isOneOf(name, 'outline'),
    type: 'ref:Outline'
  },
  OutlineOverview: {
    predicate: name => isOneOf(name, 'outlineOverview'),
    type: 'ref:OutlineOverview'
  },
  Page: {
    predicate: name => isOneOf(name, 'page'),
    type: 'ref:Page'
  },
  TabItem: {
    predicate: name => isOneOf(name, 'tabItem'),
    type: 'ref:TabItem'
  },
  TabItems: {
    predicate: name => isOneOf(name, 'tabItems'),
    type: 'ref:TabItem[]'
  },
  KeyStroke: {
    predicate: name => isOneOf(name, 'keyStroke'),
    type: 'ref:KeyStroke'
  },
  Cell: {
    predicate: name => isOneOf(name, 'cell', 'headerCell'),
    type: 'ref:Cell'
  },
  Table: {
    predicate: name => isOneOf(name, 'table', 'detailTable'),
    type: 'ref:Table'
  },
  TableControl: {
    predicate: name => isOneOf(name, 'tableControl'),
    type: 'ref:TableControl'
  },
  TableControls: {
    predicate: name => isOneOf(name, 'tableControls'),
    type: 'ref:TableControl[]'
  },
  Tree: {
    predicate: name => isOneOf(name, 'tree'),
    type: 'ref:Tree'
  },
  TileGrid: {
    predicate: name => isOneOf(name, 'tileGrid'),
    type: 'ref:TileGrid'
  },
  Tile: {
    predicate: name => isOneOf(name, 'tile', 'focusedTile'),
    type: 'ref:Tile'
  },
  Tiles: {
    predicate: name => isOneOf(name, 'tiles'),
    type: 'ref:Tile[]'
  },
  Range: {
    predicate: name => isOneOf(name, 'viewRange'),
    type: 'ref:Range'
  },
  Widget: {
    predicate: name => isOneOf(name, 'widget', 'displayParent') || name.endsWith('Widget'),
    type: 'ref:Widget'
  },
  Widgets: {
    predicate: name => isOneOf(name, 'widgets'),
    type: 'ref:Widget[]'
  }
};

export const defaultReturnTypeMap = {
  JQuery: {
    predicate: name => name.startsWith('$') || name.startsWith('get$'),
    type: 'ref:JQuery'
  },
  Dimension: {
    predicate: name => isOneOf('prefSize', 'preferredLayoutSize'),
    type: 'ref:Dimension'
  },
  KeyStrokeContext: {
    predicate: name => name === '_createKeyStrokeContext',
    type: 'ref:KeyStrokeContext'
  }
};

export function isOneOf(value, ...args) {
  if (args.length === 0) {
    return false;
  }
  let argsToCheck = args;
  if (args.length === 1 && Array.isArray(args[0])) {
    argsToCheck = args[0];
  }
  return argsToCheck.indexOf(value) !== -1;
}
