import ts from 'typescript';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = new URL('../', import.meta.url);
const output = new URL('target/tests/', root);
await mkdir(output, { recursive: true });
for (const [source, filename] of [
  ['src/app/core/services/auth.service.ts', 'auth.service.mjs'],
  ['src/app/shared/forms/validators.ts', 'validators.mjs'],
]) {
  const code = await readFile(new URL(source, root), 'utf8');
  const result = ts.transpileModule(code, {
    compilerOptions: {
      target: ts.ScriptTarget.ES2022,
      module: ts.ModuleKind.ES2022,
      experimentalDecorators: true,
    },
  });
  await writeFile(new URL(filename, output), result.outputText);
}
const result = spawnSync(process.execPath, ['--test', 'tests/auth.test.mjs'], {
  cwd: fileURLToPath(root),
  stdio: 'inherit',
});
process.exitCode = result.status ?? 1;
