import Anthropic from '@anthropic-ai/sdk';
import * as readline from 'readline';
import * as fs from 'fs';
import * as path from 'path';

// ─── System prompt: lee CLAUDE.md desde la raíz del proyecto ─────────────────
// __dirname apunta a assistant/src/ (tsx) o assistant/dist/ (node),
// por lo que ../../CLAUDE.md siempre resuelve a la raíz del proyecto.

function cargarSystemPrompt(): string {
  const claudeMdPath = path.resolve(__dirname, '../../CLAUDE.md');
  if (!fs.existsSync(claudeMdPath)) {
    throw new Error(`No se encontró CLAUDE.md en: ${claudeMdPath}`);
  }
  const contenido = fs.readFileSync(claudeMdPath, 'utf-8');
  return `Eres un asistente experto en el sistema Alera de trazabilidad de cerveza artesanal. \
Tienes acceso al documento de arquitectura completo del proyecto. \
Responde preguntas, ayuda a implementar funcionalidades y corrige errores siguiendo estrictamente \
las convenciones y reglas de negocio documentadas.

${contenido}`;
}

// ─── Tipos ───────────────────────────────────────────────────────────────────

type Message = {
  role: 'user' | 'assistant';
  content: string;
};

// ─── Estado de la sesión ─────────────────────────────────────────────────────

const history: Message[] = [];
let client: Anthropic;
let systemPrompt: string;

// ─── Chat con streaming y prompt caching ─────────────────────────────────────

async function chat(userMessage: string): Promise<void> {
  history.push({ role: 'user', content: userMessage });

  process.stdout.write(`\n\x1b[33m🐸 Alera:\x1b[0m `);

  let fullResponse = '';

  const stream = client.messages.stream({
    model: 'claude-opus-4-7',
    max_tokens: 8192,
    system: [
      {
        type: 'text',
        text: systemPrompt,
        cache_control: { type: 'ephemeral' },
      },
    ],
    messages: history.map((m) => ({ role: m.role, content: m.content })),
  });

  stream.on('text', (text) => {
    process.stdout.write(text);
    fullResponse += text;
  });

  const finalMsg = await stream.finalMessage();
  console.log('\n');

  history.push({ role: 'assistant', content: fullResponse });

  const usage = finalMsg.usage as Anthropic.Usage & {
    cache_read_input_tokens?: number;
    cache_creation_input_tokens?: number;
  };

  const cacheRead  = usage.cache_read_input_tokens  ?? 0;
  const cacheWrite = usage.cache_creation_input_tokens ?? 0;

  if (cacheWrite > 0) {
    console.log(`\x1b[90m[caché escrita: ${cacheWrite} tokens]\x1b[0m`);
  } else if (cacheRead > 0) {
    console.log(`\x1b[90m[caché leída: ${cacheRead} tokens — ahorro activo]\x1b[0m`);
  }
}

// ─── CLI interactivo ─────────────────────────────────────────────────────────

async function pedirApiKey(rl: readline.Interface): Promise<string> {
  return new Promise((resolve) => {
    rl.question('\x1b[33mAPI Key de Anthropic:\x1b[0m ', (key) => {
      resolve(key.trim());
    });
  });
}

async function main(): Promise<void> {
  // Cargar CLAUDE.md antes de arrancar
  try {
    systemPrompt = cargarSystemPrompt();
  } catch (err) {
    console.error(`\x1b[31mError:\x1b[0m ${(err as Error).message}`);
    process.exit(1);
  }

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: true,
  });

  console.log('\x1b[32m╔══════════════════════════════════════════╗\x1b[0m');
  console.log('\x1b[32m║   🐸  Golden Frog  —  Asistente Alera   ║\x1b[0m');
  console.log('\x1b[32m╚══════════════════════════════════════════╝\x1b[0m\n');

  const apiKey = await pedirApiKey(rl);
  if (!apiKey.startsWith('sk-ant-')) {
    console.error('\x1b[31mAPI key inválida.\x1b[0m Debe empezar con sk-ant-');
    rl.close();
    process.exit(1);
  }

  client = new Anthropic({ apiKey });

  console.log('\n\x1b[32m✓ Conectado.\x1b[0m Experto en el sistema de trazabilidad de cerveza artesanal Alera.');
  console.log(`\x1b[90mCLAUDE.md cargado (${systemPrompt.length.toLocaleString()} caracteres)\x1b[0m`);
  console.log('\x1b[90m"salir" para terminar  ·  "limpiar" para nueva conversación  ·  "recargar" para actualizar CLAUDE.md\x1b[0m\n');

  const prompt = (): void => {
    rl.question('\x1b[36mTú:\x1b[0m ', async (input) => {
      const text = input.trim();

      if (!text) { prompt(); return; }

      if (text.toLowerCase() === 'salir') {
        console.log('\n\x1b[32m¡Hasta luego! 🍺\x1b[0m');
        rl.close();
        process.exit(0);
      }

      if (text.toLowerCase() === 'limpiar') {
        history.length = 0;
        console.log('\x1b[90m[Conversación reiniciada]\x1b[0m\n');
        prompt();
        return;
      }

      // Recarga CLAUDE.md en caliente sin reiniciar el proceso
      if (text.toLowerCase() === 'recargar') {
        try {
          systemPrompt = cargarSystemPrompt();
          history.length = 0;
          console.log(`\x1b[32m✓ CLAUDE.md recargado\x1b[0m \x1b[90m(${systemPrompt.length.toLocaleString()} caracteres — conversación reiniciada)\x1b[0m\n`);
        } catch (err) {
          console.error(`\x1b[31mError al recargar:\x1b[0m ${(err as Error).message}\n`);
        }
        prompt();
        return;
      }

      try {
        await chat(text);
      } catch (err) {
        if (err instanceof Anthropic.APIError) {
          console.error(`\n\x1b[31mError API (${err.status}):\x1b[0m ${err.message}\n`);
        } else {
          console.error('\n\x1b[31mError:\x1b[0m', err, '\n');
        }
      }

      prompt();
    });
  };

  prompt();
}

main();
