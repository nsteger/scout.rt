// import {createProject} from '@ts-morph/bootstrap';
import ts from 'typescript';
import path from 'path';
const __dirname = path.resolve();

export function mockPluginParams(params) {
  const {
    fileName = 'file.ts',
    text = '',
    semanticDiagnostics = [],
    syntacticDiagnostics = [],
    suggestionDiagnostics = [],
    options = {}
  } = params;

  const sourceFile = ts.createSourceFile(
    fileName,
    text,
    ts.ScriptTarget.Latest,
    /* setParentNodes */ true
  );

  const withFile = diagnostic => ({
    ...diagnostic,
    file: sourceFile
  });

  return {
    options: options,
    fileName,
    rootDir: __dirname,
    text,
    sourceFile,
    getLanguageService: () =>
      ({
        getSemanticDiagnostics: () => semanticDiagnostics.map(withFile),
        getSyntacticDiagnostics: () => syntacticDiagnostics.map(withFile),
        getSuggestionDiagnostics: () => suggestionDiagnostics.map(withFile)
      })
  };
}

export function mockDiagnostic(
  text,
  errorText,
  overrides = {}
) {
  const index = text.indexOf(errorText);
  if (index === -1) {
    throw new Error(`Did not find ${errorText} in ${text}`);
  }

  return {
    messageText: 'diagnostic message',
    start: index,
    length: errorText.length,
    category: ts.DiagnosticCategory.Error,
    code: 123,
    ...overrides
  };
}

// export async function realPluginParams(params) {
//   const {fileName = 'file.ts', text = '', options = {}} = params;
//
//   const project = await createProject({
//     compilerOptions: {
//       strict: true
//     },
//     useInMemoryFileSystem: true
//   });
//   const sourceFile = project.createSourceFile(fileName, text);
//
//   const getLanguageService = () => project.getLanguageService();
//
//   return {
//     options: options,
//     fileName,
//     rootDir: __dirname,
//     text,
//     sourceFile,
//     getLanguageService
//   };
// }
