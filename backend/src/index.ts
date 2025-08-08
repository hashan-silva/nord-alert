import Fastify from 'fastify';
import { z } from 'zod';
import { fetchAllAlerts } from './services/aggregator';
import { Severity } from './models/alert';

const app = Fastify();

const querySchema = z.object({
  county: z.string().optional(),
  severity: z.enum(['info', 'low', 'medium', 'high']).optional(),
});

const severityOrder: Severity[] = ['info', 'low', 'medium', 'high'];

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
  return filtered;
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
