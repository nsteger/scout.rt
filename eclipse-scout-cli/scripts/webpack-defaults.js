/*
 * Copyright (c) 2010-2022 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
const path = require('path');
const scoutBuildConstants = require('./constants');
const CopyPlugin = require('copy-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const AfterEmitWebpackPlugin = require('./AfterEmitWebpackPlugin');
const {SourceMapDevToolPlugin, WatchIgnorePlugin, ProgressPlugin} = require('webpack');

/**
 * @param {string} args.mode development or production
 * @param {boolean} args.clean true, to clean the dist folder before each build. Default is true.
 * @param {boolean} args.progress true, to show build progress in percentage. Default is true.
 * @param {boolean} args.profile true, to show timing information for each build step. Default is false.
 * @param {[]} args.resDirArray an array containing directories which should be copied to dist/res
 * @param {object} args.tsOptions a config object to be passed to the ts-loader
 */
module.exports = (env, args) => {
  const buildMode = args.mode;
  const {devMode, cssFilename, jsFilename} = scoutBuildConstants.getConstantsForMode(buildMode);
  const isMavenModule = scoutBuildConstants.isMavenModule();
  const outDir = scoutBuildConstants.getOutputDir(buildMode);
  const resDirArray = args.resDirArray || ['res'];
  console.log(`Webpack mode: ${buildMode}`);

  // # Copy static web-resources delivered by the modules
  const copyPluginConfig = [];
  const copyTarget = isMavenModule ? '../res' : '.';
  for (const resDir of resDirArray) {
    copyPluginConfig.push(
      {
        from: resDir,
        to: copyTarget
      });
  }

  const babelOptions = {
    compact: false,
    cacheDirectory: true,
    cacheCompression: false,
    presets: [
      [require.resolve('@babel/preset-env'), {
        debug: false,
        targets: {
          firefox: '69',
          chrome: '71',
          safari: '12.1'
        }
      }]
    ]
  };

  const tsOptions = {
    ...args.tsOptions,
    compilerOptions: {
      noEmit: false,
      ...args.tsOptions?.compilerOptions
    }
  };

  const config = {
    mode: buildMode,
    devtool: false, // disabled because SourceMapDevToolPlugin is used (see below)
    ignoreWarnings: [(webpackError, compilation) => isWarningIgnored(devMode, webpackError, compilation)],
    resolve: {
      // no automatic polyfills. clients must add the desired polyfills themselves.
      fallback: {
        assert: false,
        buffer: false,
        console: false,
        constants: false,
        crypto: false,
        domain: false,
        events: false,
        http: false,
        https: false,
        os: false,
        path: false,
        punycode: false,
        process: false,
        querystring: false,
        stream: false,
        string_decoder: false,
        sys: false,
        timers: false,
        tty: false,
        url: false,
        util: false,
        vm: false,
        zlib: false
      },
      extensions: ['.ts', '.js', '.json', '.wasm', '.tsx', '.jsx']
    },
    // expect these apis in the browser
    externals: {
      'crypto': 'crypto',
      'canvas': 'canvas',
      'fs': 'fs',
      'http': 'http',
      'https': 'https',
      'url': 'url',
      'zlib': 'zlib'
    },
    output: {
      filename: jsFilename,
      path: outDir,
      clean: nvl(args.clean, true)
    },
    performance: {
      hints: false
    },
    profile: args.profile,
    module: {
      rules: [{
        // LESS
        test: /\.less$/,
        use: [{
          // Extracts CSS into separate files. It creates a CSS file per JS file which contains CSS.
          // It supports On-Demand-Loading of CSS and SourceMaps.
          // see: https://webpack.js.org/plugins/mini-css-extract-plugin/
          //
          // Note: this creates some useless *.js files, like dark-theme.js
          // This seems to be an issue in webpack, workaround is to remove the files later
          // see: https://github.com/webpack-contrib/mini-css-extract-plugin/issues/151
          // seems to be fixed in webpack 5, workaround to manually delete js files can be removed as soon as webpack 5 is released
          loader: MiniCssExtractPlugin.loader
        }, {
          // Interprets @import and url() like import/require() and will resolve them.
          // see: https://webpack.js.org/loaders/css-loader/
          loader: require.resolve('css-loader'),
          options: {
            sourceMap: devMode,
            modules: false, // We don't want to work with CSS modules
            url: false // Don't resolve URLs in LESS, because relative path does not match /res/fonts
          }
        }, {
          // Compiles Less to CSS.
          // see: https://webpack.js.org/loaders/less-loader/
          loader: require.resolve('less-loader'),
          options: {
            sourceMap: devMode,
            lessOptions: {
              relativeUrls: false,
              rewriteUrls: 'off',
              math: 'always'
            }
          }
        }]
      }, {
        test: /\.tsx?$/,
        exclude: /node_modules/,
        use: [{
          loader: require.resolve('babel-loader'),
          options: babelOptions
        }, {
          loader: require.resolve('ts-loader'),
          options: tsOptions
        }]
      }, {
        test: /\.jsx?$/,
        use: [{
          loader: require.resolve('babel-loader'),
          options: babelOptions
        }]
      }, {
        test: /\.jsx?$/,
        enforce: 'pre',
        use: [{
          loader: require.resolve('source-map-loader')
        }]
      }, {
        // to support css imports (currently not used by Scout but might be used by included 3rd party libs)
        test: /\.css$/i,
        use: [{
          loader: require.resolve('style-loader')
        }, {
          loader: require.resolve('css-loader'),
          options: {
            sourceMap: devMode,
            modules: false, // We don't want to work with CSS modules
            url: false // Don't resolve URLs in LESS, because relative path does not match /res/fonts
          }
        }]
      }]
    },
    plugins: [
      new WatchIgnorePlugin({paths: [/\.d\.ts$/]}),
      // see: extracts css into separate files
      new MiniCssExtractPlugin({filename: cssFilename}),
      // run post-build script hook
      new AfterEmitWebpackPlugin({outDir: outDir}),
      new SourceMapDevToolPlugin({
        // Use external source maps in all modes because the browser is very slow in displaying a file containing large lines which is the case if source maps are inlined
        filename: '[file].map',

        // Don't create maps for static resources.
        // They may already have maps which could lead to "multiple assets emit different content to the same file" exception.
        exclude: /\/res\/.*/,

        // In production mode create external source maps without source code to map stack traces.
        // Otherwise, stack traces would point to the minified source code which makes it quite impossible to analyze productive issues.
        noSources: !devMode,
        moduleFilenameTemplate: devMode ? undefined : prodDevtoolModuleFilenameTemplate
      })
    ],
    optimization: {
      splitChunks: {
        chunks: 'all',
        name: (module, chunks, cacheGroupKey) => computeChunkName(module, chunks, cacheGroupKey)
      }
    }
  };

  // Copy resources only add the plugin if there are resources to copy. Otherwise, the plugin fails.
  if (copyPluginConfig.length > 0) {
    config.plugins.push(new CopyPlugin({patterns: copyPluginConfig}));
  }

  // Shows progress information in the console in dev mode
  if (nvl(args.progress, true)) {
    config.plugins.push(new ProgressPlugin({profile: args.profile}));
  }

  if (!devMode) {
    const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
    const TerserPlugin = require('terser-webpack-plugin');
    config.optimization.minimizer = [
      // minify css
      new CssMinimizerPlugin({
        test: /\.min\.css$/g,
        minimizerOptions: {
          preset: ['default', {
            discardComments: {removeAll: true}
          }]
        }
      }),
      // minify js
      new TerserPlugin({
        test: /\.js(\?.*)?$/i,
        parallel: 4
      })
    ];
  }

  return config;
};

/**
 * @param {object} [options]
 * @param {boolean} [options.clean] Instruct the AfterEmitWebpackPlugin to clean the output. Default is true.
 * @param {boolean} [options.externalizeDevDeps] Add devDependencies as externals. Default is false.
 */
function libraryConfig(config, options = {}) {
  const packageJson = require(path.resolve('./package.json'));
  let dependencies = toExternals(packageJson.dependencies);
  if (options.externalizeDevDeps ?? false) {
    dependencies = Object.assign(dependencies, toExternals(packageJson.devDependencies));
  }
  // Make synthetic default import work (import $ from 'jquery') by importing jquery as commonjs module
  let globalDependencies = {};
  if (dependencies['jquery']) {
    globalDependencies['jquery'] = 'commonjs jquery';
  }

  let plugins = config.plugins;
  if (options.clean ?? true) {
    // FileList is not necessary in library mode
    plugins = plugins.map(plugin => {
      if (plugin instanceof AfterEmitWebpackPlugin) {
        return new AfterEmitWebpackPlugin({outDir: plugin.options.outDir, createFileList: false});
      }
      return plugin;
    });
  } else {
    // If clean is false, we don't need the plugin at all
    plugins = plugins.filter(plugin => !(plugin instanceof AfterEmitWebpackPlugin));
  }

  return {
    ...config,
    optimization: {
      ...config.optimization,
      splitChunks: undefined // disable splitting
    },
    output: {
      ...config.output,
      library: {
        type: 'module'
      }
    },
    externals: {
      ...config.externals,
      ...dependencies,
      ...globalDependencies
    },
    experiments: {
      // required for library.type = 'module'
      outputModule: true
    },
    plugins
  };
}

/**
 * Creates a new object that contains the same keys as the given object. The values are replaced with the keys.
 * So the resulting object looks like: {key1: key1, key2: key2}.
 */
function toExternals(object) {
  return Object.keys(object).reduce((obj, current) => {
    obj[current] = current;
    return obj;
  }, {});
}

/**
 * @param {object} entry the webpack entry object
 * @param {object} options the options object to configure which themes should be built and how
 * @param {[string]} options.themes one or more themes of the availableThemes that should be built. Use 'all' to build all available themes, or 'none' to build no themes. Default is 'all'.
 * @param {[string]} options.availableThemes the themes that are available.
 * @param {function} options.generator a function that returns an array containing the key and value for the generated entry. The function will be called for each theme with the theme name as argument.
 */
function addThemes(entry, options = {}) {
  let themes = ensureArray(nvl(options.themes, 'all'));
  let availableThemes = options.availableThemes;
  if (!availableThemes) {
    throw 'Please specify the availableThemes';
  }
  let generator = options.generator;
  if (!generator) {
    throw 'Please specify a theme entry generator (themeEntryGen) that returns and array containing the key and value of the entry to generate for each theme.';
  }
  if (themes.includes('all')) {
    themes = availableThemes;
  }
  themes = themes.filter(theme => availableThemes.includes(theme));
  if (themes.length === 0) {
    return;
  }
  console.log(`Themes: ${themes}`);
  themes.forEach(theme => {
    let name = theme === 'default' ? '' : `-${theme}`;
    let [key, value] = generator(name);
    entry[key] = value;
  });
}

function computeChunkName(module, chunks, cacheGroupKey) {
  const entryPointDelim = '~';
  const allChunksNames = chunks
    .map(chunk => chunk.name)
    .filter(chunkName => !!chunkName)
    .join(entryPointDelim);
  let fileName = cacheGroupKey === 'defaultVendors' ? 'vendors' : cacheGroupKey;

  if (allChunksNames.length < 1) {
    // there is no chunk name (e.g. lazy loaded module): derive chunk-name from filename
    const segmentDelim = '-';
    if (fileName.length > 0) {
      fileName += segmentDelim;
    }
    return fileName + computeModuleId(module);
  }

  if (fileName.length > 0) {
    fileName += entryPointDelim;
  }
  return fileName + allChunksNames;
}

function computeModuleId(module) {
  const nodeModules = 'node_modules';
  // noinspection JSUnresolvedVariable
  let id = module.userRequest;
  const nodeModulesPos = id.lastIndexOf(nodeModules);
  if (nodeModulesPos < 0) {
    // use file name
    id = path.basename(id, '.js');
  } else {
    // use js-module name
    id = id.substring(nodeModulesPos + nodeModules.length + path.sep.length);
    let end = id.indexOf(path.sep);
    if (end >= 0) {
      if (id.startsWith('@')) {
        const next = id.indexOf(path.sep, end + 1);
        if (next >= 0) {
          end = next;
        }
      }
      id = id.substring(0, end);
    }
  }

  return id.replace(/[/\\\-@:_.|]+/g, '').toLowerCase();
}

function ensureArray(array) {
  if (array === undefined || array === null) {
    return [];
  }
  if (Array.isArray(array)) {
    return array;
  }
  const isIterable = typeof array[Symbol.iterator] === 'function' && typeof array !== 'string';
  if (isIterable) {
    return Array.from(array);
  }
  return [array];
}

function nvl(arg, defaultValue) {
  if (arg === undefined || arg === null) {
    return defaultValue;
  }
  return arg;
}

function isWarningIgnored(devMode, webpackError) {
  if (devMode || !webpackError || !webpackError.warning || !webpackError.warning.message) {
    return false;
  }
  return webpackError.warning.message.startsWith('Failed to parse source map');
}

/**
 * Don't reveal absolute file paths in production mode -> only return the file name relative to its module.
 * @param info.resourcePath
 */
function prodDevtoolModuleFilenameTemplate(info) {
  let path = info.resourcePath || '';
  // Search for the last /src/ in the path and return the fragment starting from its parent
  let result = path.match(/.*\/(.*\/src\/.*)/);
  if (result) {
    return result[1];
  }
  // Match everything after the /last node_modules/ in the path
  result = path.match(/.*\/node_modules\/(.*)/);
  if (result) {
    return result[1];
  }
  // Return only the file name (the part after the last /)
  result = path.match(/([^/\\]*)$/);
  if (result) {
    return result[1];
  }
}

module.exports.addThemes = addThemes;
module.exports.libraryConfig = libraryConfig;
