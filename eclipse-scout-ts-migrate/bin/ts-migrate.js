#!/usr/bin/env node

import path from 'path';
import {eslintFixPlugin, jsDocPlugin} from 'ts-migrate-plugins';
import {migrate, MigrateConfig} from 'ts-migrate-server';
import renameModule from 'ts-migrate/build/commands/rename.js';
import parser from 'yargs-parser';
import convertToCRLFPlugin from '../src/convertToCRLFPlugin.js';
import memberAccessModifierPlugin from '../src/memberAccessModifierPlugin.js';
import convertToLFPlugin from '../src/convertToLFPlugin.js';
import declareMissingClassPropertiesPlugin from '../src/declareMissingClassPropertiesPlugin.js';
import methodsPlugin from '../src/methodsPlugin.js';

const rename = renameModule.default; // Default imports don't work as expected when importing from cjs modules

const yargsOptions = {
  boolean: ['rename', 'renameOnly'],
  array: ['sources'],
  default: {'rename': true, 'renameOnly': false}
};
const args = parser(process.argv, yargsOptions);

const rootDir = path.resolve(process.cwd());
let sources = args.sources;

if (args.rename) {
  rename({rootDir, sources});
}
if (args.renameOnly) {
  process.exit(-1);
}

if (sources) {
  sources = sources.map(source => {
    source = source.replace(/(.js)$/, '.ts');
    if (!source.endsWith('.ts')) {
      source += '.ts';
    }
    return source;
  });
}

const jsDocTypeMap = {
  function: {
    tsName: 'Function',
    acceptsTypeParameters: false
  }
};

const moduleMap = {
  scout: name => name.indexOf('index') > -1
};

const config = new MigrateConfig()
  // .addPlugin(countMethodsPlugin);
  .addPlugin(convertToCRLFPlugin, {})
  .addPlugin(jsDocPlugin, {typeMap: jsDocTypeMap, annotateReturns: true})
  .addPlugin(declareMissingClassPropertiesPlugin, {moduleMap})
  .addPlugin(memberAccessModifierPlugin, {})
  .addPlugin(methodsPlugin, {moduleMap})
  .addPlugin(convertToLFPlugin, {})
  .addPlugin(eslintFixPlugin, {});

migrate({rootDir, config, sources}).then(exitCode => process.exit(exitCode));
