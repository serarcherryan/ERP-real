import { execFileSync } from 'node:child_process';
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function runScript(scriptName, env = {}) {
  return execFileSync('bash', [path.join(rootDir, 'scripts', scriptName)], {
    cwd: rootDir,
    env: {
      ...process.env,
      ERP_SCRIPT_DRY_RUN: '1',
      ...env,
    },
    encoding: 'utf8',
  });
}

describe('dev startup scripts', () => {
  it('prints full stack startup commands without requiring external services', () => {
    const output = runScript('start-dev.sh', {
      WEB_PORT: '5310',
      BACKEND_PORT: '18180',
    });

    assert.match(output, /DRY RUN: docker compose -f docker\/dev\/docker-compose\.yml up -d postgres/);
    assert.match(output, /SERVER_PORT=18180 SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run/);
    assert.match(output, /Backend: http:\/\/localhost:18180/);
    assert.match(output, /Web:\s+http:\/\/localhost:5310/);
    assert.match(output, /DRY RUN: PORT=5310 npm run dev:web -- --port 5310/);
  });

  it('starts backend through docker compose profile in dry-run mode', () => {
    const output = runScript('start-backend-docker.sh');

    assert.match(output, /DRY RUN: docker compose -f docker\/dev\/docker-compose\.yml --profile backend up --build/);
  });

  it('starts only backend with the requested port in dry-run mode', () => {
    const output = runScript('start-backend-dev.sh', {
      BACKEND_PORT: '19090',
    });

    assert.match(output, /DRY RUN: docker compose -f docker\/dev\/docker-compose\.yml up -d postgres/);
    assert.match(output, /SERVER_PORT=19090 SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run/);
  });

  it('starts only web admin with the requested port in dry-run mode', () => {
    const output = runScript('start-web.sh', {
      PORT: '5320',
    });

    assert.match(output, /Starting ERP web admin on port 5320/);
    assert.match(output, /DRY RUN: npm run dev:web -- --port 5320/);
  });
});
