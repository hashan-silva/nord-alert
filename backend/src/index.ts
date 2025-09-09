import Fastify from 'fastify';
import { z } from 'zod';
import { fetchAllAlerts } from './services/aggregator';
import { Severity, Alert } from './models/alert';

const app = Fastify();

const querySchema = z.object({
  county: z.string().optional(),
  severity: z.enum(['info', 'low', 'medium', 'high']).optional(),
});

const severityOrder: Severity[] = ['info', 'low', 'medium', 'high'];

function pruneNulls(alert: Alert): Alert {
  return Object.fromEntries(
    Object.entries(alert).filter(([, v]) => v !== null && v !== undefined)
  ) as Alert;
}

app.get('/health', async () => {
  return { status: 'ok' };
});

app.get('/alerts', async (request, reply) => {
  const { county, severity } = querySchema.parse(request.query);
  const alerts = await fetchAllAlerts();
  let filtered = alerts;
  if (county) {
    filtered = filtered.filter((a) => a.areas.includes(county));
  }
  if (severity) {
    const threshold = severityOrder.indexOf(severity);
    filtered = filtered.filter(
      (a) => severityOrder.indexOf(a.severity) >= threshold
    );
  }
  return filtered.map(pruneNulls);
});

export async function start() {
  try {
    await app.listen({ port: Number(process.env.PORT) || 3000, host: '0.0.0.0' });
    console.log('Server listening');
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
}

if (require.main === module) {
  start();
}
