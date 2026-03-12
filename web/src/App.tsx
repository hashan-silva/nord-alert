import { useEffect, useMemo, useState } from 'react';
import SyncRoundedIcon from '@mui/icons-material/SyncRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import {
  Alert,
  Box,
  Button,
  Container,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  Typography
} from '@mui/material';
import AlertList from './components/AlertList';
import SummaryCard from './components/SummaryCard';
import { type AlertItem, baseUrl, fetchAlerts } from './lib/api';

const severityOptions = [
  { label: 'All severities', value: '' },
  { label: 'Info and above', value: 'info' },
  { label: 'Medium and above', value: 'medium' },
  { label: 'High only', value: 'high' }
];

function App() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [county, setCounty] = useState('');
  const [severity, setSeverity] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function loadAlerts() {
      setLoading(true);
      setError('');

      try {
        const nextAlerts = await fetchAlerts({ county, severity });
        if (!cancelled) {
          setAlerts(nextAlerts);
        }
      } catch (nextError) {
        if (!cancelled) {
          setError(nextError instanceof Error ? nextError.message : 'Unable to load alerts');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadAlerts();

    return () => {
      cancelled = true;
    };
  }, [county, severity]);

  const counties = useMemo(() => {
    const options = new Set<string>();
    alerts.forEach((alert) => {
      alert.areas?.forEach((area) => options.add(area));
    });
    return ['', ...Array.from(options).sort((left, right) => left.localeCompare(right))];
  }, [alerts]);

  const sourceCounts = useMemo<Record<string, number>>(() => {
    return alerts.reduce<Record<string, number>>((counts, alert) => {
      counts[alert.source] = (counts[alert.source] || 0) + 1;
      return counts;
    }, {});
  }, [alerts]);

  return (
    <Box className="dashboard-shell">
      <Container maxWidth="xl">
        <Stack className="hero" spacing={3}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            justifyContent="space-between"
            spacing={3}
            alignItems={{ xs: 'flex-start', md: 'flex-end' }}
          >
            <Box>
              <Typography className="hero__eyebrow" variant="overline">
                NordAlert dashboard
              </Typography>
              <Typography variant="h1">Swedish public alerts in one live command view.</Typography>
              <Typography className="hero__copy" variant="body1">
                Track official updates from Polisen, SMHI, and Krisinformation with a
                browser-based dashboard tailored for operations and monitoring.
              </Typography>
            </Box>
            <Paper className="hero__endpoint" elevation={0}>
              <Typography variant="overline">Backend API</Typography>
              <Typography variant="body1">{baseUrl}</Typography>
            </Paper>
          </Stack>

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 4 }}>
              <SummaryCard
                eyebrow="Active feed"
                value={alerts.length}
                caption="alerts returned"
                tone="default"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <SummaryCard
                eyebrow="Police"
                value={sourceCounts.POLISEN ?? 0}
                caption="incidents"
                tone="accent"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <SummaryCard
                eyebrow="Weather + crisis"
                value={(sourceCounts.SMHI ?? 0) + (sourceCounts.KRISINFORMATION ?? 0)}
                caption="warnings"
                tone="warning"
              />
            </Grid>
          </Grid>
        </Stack>

        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 4 }}>
            <Paper className="panel filters-panel" elevation={0}>
              <Stack spacing={3}>
                <Box>
                  <Typography variant="h3">Filters</Typography>
                  <Typography color="text.secondary">
                    Narrow the feed by county and severity threshold.
                  </Typography>
                </Box>

                <FormControl fullWidth>
                  <InputLabel id="county-select-label">County</InputLabel>
                  <Select
                    labelId="county-select-label"
                    label="County"
                    value={county}
                    onChange={(event) => setCounty(event.target.value)}
                  >
                    {counties.map((option) => (
                      <MenuItem key={option || 'all'} value={option}>
                        {option || 'All counties'}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth>
                  <InputLabel id="severity-select-label">Severity</InputLabel>
                  <Select
                    labelId="severity-select-label"
                    label="Severity"
                    value={severity}
                    onChange={(event) => setSeverity(event.target.value)}
                  >
                    {severityOptions.map((option) => (
                      <MenuItem key={option.label} value={option.value}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <Button
                  variant="outlined"
                  startIcon={<SyncRoundedIcon />}
                  onClick={() => {
                    setCounty('');
                    setSeverity('');
                  }}
                >
                  Reset filters
                </Button>
              </Stack>
            </Paper>
          </Grid>

          <Grid size={{ xs: 12, lg: 8 }}>
            <Paper className="panel feed-panel" elevation={0}>
              <Stack spacing={2}>
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  justifyContent="space-between"
                  spacing={2}
                >
                  <Box>
                    <Typography variant="h3">Live feed</Typography>
                    <Typography color="text.secondary">
                      Aggregated official alerts from the backend `/alerts` endpoint.
                    </Typography>
                  </Box>
                  <Typography color="text.secondary" variant="body2">
                    {loading ? 'Refreshing feed...' : `${alerts.length} alerts loaded`}
                  </Typography>
                </Stack>

                {error && (
                  <Alert icon={<WarningAmberRoundedIcon />} severity="error">
                    {error}
                  </Alert>
                )}

                <AlertList alerts={alerts} />
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}

export default App;
