#!/usr/bin/env node

/* eslint-disable no-await-in-loop, no-restricted-syntax */
import path from 'path';
import {eslintFixPlugin, jsDocPlugin} from 'ts-migrate-plugins';
import {migrate, MigrateConfig} from 'ts-migrate-server';
import renameModule from 'ts-migrate/build/commands/rename.js';
import parser from 'yargs-parser';
import convertToCRLFPlugin from '../src/convertToCRLFPlugin.js';
import memberAccessModifierPlugin from '../src/memberAccessModifierPlugin.js';
import convertToLFPlugin from '../src/convertToLFPlugin.js';
import declareMissingClassPropertiesPlugin from '../src/declareMissingClassProperties.js';
import methodsPlugin from '../src/methodsPlugin.js';

const rename = renameModule.default; // Default imports don't work as expected when importing from cjs modules

const yargsOptions = {
  boolean: ['rename', 'renameOnly',],
  array: ['sources'],
  default: {'rename': true, renameOnly: false}
};
const args = parser(process.argv, yargsOptions);

const defaultAccessibility = undefined;
const privateRegex = undefined;
const protectedRegex = '_';
const publicRegex = undefined;
const anyAlias = undefined;
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

const typeMap = {
  function: {
    tsName: 'Function',
    acceptsTypeParameters: false
  }
};

const config = new MigrateConfig()
  // .addPlugin(countMethodsPlugin);
  // .addPlugin(stripTSIgnorePlugin, {})
  // .addPlugin(hoistClassStaticsPlugin, { anyAlias })
  .addPlugin(convertToCRLFPlugin, {})
  .addPlugin(jsDocPlugin, {anyAlias, typeMap, annotateReturns: true})
  .addPlugin(declareMissingClassPropertiesPlugin, {anyAlias})
  .addPlugin(memberAccessModifierPlugin, {})
  // .addPlugin(memberAccessibilityPlugin, {
  //   defaultAccessibility,
  //   privateRegex,
  //   protectedRegex,
  //   publicRegex
  // })
  .addPlugin(methodsPlugin, {})
  .addPlugin(convertToLFPlugin, {})
  // .addPlugin(explicitAnyPlugin, {anyAlias})
// .addPlugin(addConversionsPlugin, { anyAlias })
// .addPlugin(tsIgnorePlugin, {})
// We need to run eslint-fix again after ts-ignore to fix up formatting.
  // Fixes most of the formatting issues but not all -> run format code afterwards manually in IntelliJ
  .addPlugin(eslintFixPlugin, {});

migrate({rootDir, config, sources}).then(exitCode => process.exit(exitCode));
