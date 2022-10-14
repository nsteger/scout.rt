/*
 * Copyright (c) 2010-2020 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {arrays, CompactTree, ModelAdapterModel, ObjectFactory, Session, Tree, TreeAdapter, TreeModel, TreeNode, TreeNodeModel} from '../../index';
import $ from 'jquery';
import {Optional, RefModel} from '../../types';

export default class TreeSpecHelper {
  session: Session;

  constructor(session: Session) {
    this.session = session;
  }

  createModel(nodes: RefModel<TreeNodeModel>[]): TreeModel {
    return {
      objectType: Tree,
      parent: this.session.desktop,
      nodes: nodes
    };
  }

  createModelFixture(nodeCount: number, depth: number, expanded: boolean): TreeModel {
    return this.createModel(this.createModelNodes(nodeCount, depth, expanded));
  }

  // FIXME TS insertTree nodes without parent? Like TableRowData? Many widgets have the same problem (GroupBox.setFields etc).
  //  Parent is only necessary when calling scout.create directly. Improve scout.create or use RefModel for setters? ObjectType sometimes optional as well (tree, table)
  createModelNode(id: string, text: string): Optional<TreeNodeModel, 'parent'> {
    return {
      id: id + '' || ObjectFactory.get().createUniqueId(),
      text: text,
      enabled: true,
      checked: false
    };
  }

  createModelNodes(nodeCount: number, depth: number, expanded: boolean): RefModel<TreeNodeModel>[] {
    return this.createModelNodesInternal(nodeCount, depth, expanded);
  }

  createModelNodesInternal(nodeCount: number, depth: number, expanded: boolean, parentNode?: TreeNodeModel): RefModel<TreeNodeModel>[] {
    if (!nodeCount) {
      return;
    }

    let nodes = [],
      nodeId;
    if (!depth) {
      depth = 0;
    }
    for (let i = 0; i < nodeCount; i++) {
      nodeId = i;
      if (parentNode) {
        nodeId = parentNode.id + '_' + nodeId;
      }
      nodes[i] = this.createModelNode(nodeId, 'node ' + nodeId);
      nodes[i].expanded = expanded;
      if (depth > 0) {
        nodes[i].childNodes = this.createModelNodesInternal(nodeCount, depth - 1, expanded, nodes[i]);
      }
    }
    return nodes;
  }

  createTree(model: TreeModel): Tree {
    let defaults = {
      parent: this.session.desktop
    };
    model = $.extend({}, defaults, model);
    let tree = new Tree();
    tree.init(model);
    return tree;
  }

  createTreeAdapter(model: ModelAdapterModel): TreeAdapter {
    let adapter = new TreeAdapter();
    adapter.init(model);
    return adapter;
  }

  createCompactTree(model: TreeModel): CompactTree {
    let tree = new CompactTree();
    tree.init(model);
    return tree;
  }

  createCompactTreeAdapter(model: ModelAdapterModel): TreeAdapter {
    model.objectType = 'Tree:Compact';
    let tree = new TreeAdapter();
    tree.init(model);
    return tree;
  }

  findAllNodes(tree: Tree): JQuery {
    return tree.$container.find('.tree-node');
  }

  createNodeExpandedEvent(model: { id: string }, nodeId: string, expanded: boolean): any {
    return {
      target: model.id,
      nodeId: nodeId,
      expanded: expanded,
      type: 'nodeExpanded'
    };
  }

  selectNodesAndAssert(tree: Tree, nodes: TreeNode[]) {
    tree.selectNodes(nodes);
    this.assertSelection(tree, nodes);
  }

  assertSelection(tree: Tree, nodes: TreeNode[]) {
    let $selectedNodes = tree.$selectedNodes();
    expect($selectedNodes.length).toBe(nodes.length);

    let selectedNodes = [];
    $selectedNodes.each(function() {
      selectedNodes.push($(this).data('node'));
    });

    expect(arrays.equalsIgnoreOrder(nodes, selectedNodes)).toBeTruthy();
    expect(arrays.equalsIgnoreOrder(nodes, tree.selectedNodes)).toBeTruthy();
  }

  createNodesSelectedEvent(model: { id: string }, nodeIds: string[]): object {
    return {
      target: model.id,
      nodeIds: nodeIds,
      type: 'nodesSelected'
    };
  }

  createNodesInsertedEvent(model: { id: string }, nodes: string[], commonParentNodeId: string): object {
    return {
      target: model.id,
      commonParentNodeId: commonParentNodeId,
      nodes: nodes,
      type: 'nodesInserted'
    };
  }

  createNodesInsertedEventTopNode(model: { id: string }, nodes: TreeNodeModel[]): object {
    return {
      target: model.id,
      nodes: nodes,
      type: 'nodesInserted'
    };
  }

  createNodesDeletedEvent(model: { id: string }, nodeIds: string[], commonParentNodeId: string): object {
    return {
      target: model.id,
      commonParentNodeId: commonParentNodeId,
      nodeIds: nodeIds,
      type: 'nodesDeleted'
    };
  }

  createAllChildNodesDeletedEvent(model: { id: string }, commonParentNodeId: string): object {
    return {
      target: model.id,
      commonParentNodeId: commonParentNodeId,
      type: 'allChildNodesDeleted'
    };
  }

  createNodeChangedEvent(model: { id: string }, nodeId: string): object {
    return {
      target: model.id,
      nodeId: nodeId,
      type: 'nodeChanged'
    };
  }

  createNodesUpdatedEvent(model: { id: string }, nodes: TreeNodeModel[]): object {
    return {
      target: model.id,
      nodes: nodes,
      type: 'nodesUpdated'
    };
  }

  createChildNodeOrderChangedEvent(model: { id: string }, childNodeIds: string[], parentNodeId: string): object {
    return {
      target: model.id,
      parentNodeId: parentNodeId,
      childNodeIds: childNodeIds,
      type: 'childNodeOrderChanged'
    };
  }

  createTreeEnabledEvent(model: { id: string }, enabled: boolean): object {
    return {
      target: model.id,
      type: 'property',
      properties: {
        enabled: enabled
      }
    };
  }
}
