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
  ['src/app/core/services/forum.service.ts', 'forum.service.mjs'],
  ['src/app/core/guards/admin.guard.ts', 'admin.guard.mjs'],
  ['src/app/features/forum/moderation-page.ts', 'moderation-page.mjs'],
  ['src/app/features/forum/forum.validators.ts', 'forum.validators.mjs'],
]) {
  const code = await readFile(new URL(source, root), 'utf8');
  const result = ts.transpileModule(code, {
    compilerOptions: {
      target: ts.ScriptTarget.ES2022,
      module: ts.ModuleKind.ES2022,
      experimentalDecorators: true,
    },
  });
  await writeFile(
    new URL(filename, output),
    result.outputText
      .replaceAll("'./auth.service'", "'./auth.service.mjs'")
      .replaceAll("'../services/auth.service'", "'./auth.service.mjs'")
      .replaceAll("'../../core/services/auth.service'", "'./auth.service.mjs'")
      .replaceAll("'../../core/services/forum.service'", "'./forum.service.mjs'")
      .replaceAll("'./forum.validators'", "'./forum.validators.mjs'"),
  );
}
const result = spawnSync(
  process.execPath,
  ['--test', 'tests/auth.test.mjs', 'tests/forum.test.mjs'],
  {
    cwd: fileURLToPath(root),
    stdio: 'inherit',
  },
);
process.exitCode = result.status ?? 1;
