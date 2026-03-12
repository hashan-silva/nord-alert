import { useEffect, useMemo, useState } from 'react';
import SyncRoundedIcon from '@mui/icons-material/SyncRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Container,
  FormLabel,
  FormControl,
  Grid,
  InputLabel,
  ListItemText,
  MenuItem,
  OutlinedInput,
  Pagination,
  Paper,
  Select,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import AlertList from './components/AlertList';
import SummaryCard from './components/SummaryCard';
import { type AlertItem, baseUrl, fetchAlerts } from './lib/api';

const webVersion = process.env.REACT_APP_WEB_VERSION || 'unknown';
const backendVersion = process.env.REACT_APP_BACKEND_VERSION || 'unknown';

const severityOptions = [
  { label: 'All severities', value: '' },
  { label: 'Info and above', value: 'info' },
  { label: 'Medium and above', value: 'medium' },
  { label: 'High only', value: 'high' }
];
const alertsPerPage = 6;
const sourceLabels: Record<string, string> = {
  krisinformation: 'Krisinformation',
  polisen: 'Polisen',
  smhi: 'SMHI'
};

function App() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [county, setCounty] = useState('');
  const [severity, setSeverity] = useState('');
  const [selectedResources, setSelectedResources] = useState<string[]>([]);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(1);
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

  const resourceOptions = useMemo(() => {
    return Array.from(new Set(alerts.map((alert) => alert.source))).sort((left, right) =>
      left.localeCompare(right)
    );
  }, [alerts]);

  const filteredAlerts = useMemo(() => {
    return alerts.filter((alert) => {
      const matchesResource =
        selectedResources.length === 0 || selectedResources.includes(alert.source);

      const publishedAt = alert.publishedAt ? new Date(alert.publishedAt) : null;
      const matchesFrom =
        !dateFrom || (publishedAt !== null && publishedAt >= new Date(`${dateFrom}T00:00:00`));
      const matchesTo =
        !dateTo || (publishedAt !== null && publishedAt < new Date(`${dateTo}T23:59:59.999`));

      return matchesResource && matchesFrom && matchesTo;
    });
  }, [alerts, dateFrom, dateTo, selectedResources]);

  const pageCount = useMemo(
    () => Math.max(1, Math.ceil(filteredAlerts.length / alertsPerPage)),
    [filteredAlerts.length]
  );

  const paginatedAlerts = useMemo(() => {
    const startIndex = (page - 1) * alertsPerPage;
    return filteredAlerts.slice(startIndex, startIndex + alertsPerPage);
  }, [filteredAlerts, page]);

  const sourceCounts = useMemo<Record<string, number>>(() => {
    return filteredAlerts.reduce<Record<string, number>>((counts, alert) => {
      const sourceKey = alert.source.toLowerCase();
      counts[sourceKey] = (counts[sourceKey] || 0) + 1;
      return counts;
    }, {});
  }, [filteredAlerts]);

  useEffect(() => {
    setPage(1);
  }, [alerts, county, severity, selectedResources, dateFrom, dateTo]);

  useEffect(() => {
    if (page > pageCount) {
      setPage(pageCount);
    }
  }, [page, pageCount]);

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
              <Box
                component="img"
                src="/logo.png"
                alt="NordAlert logo"
                className="brand-lockup__logo"
              />
              <Typography className="hero__eyebrow" variant="overline">
                Operational overview
              </Typography>
              <Typography className="hero__title" variant="h1">
                Swedish public alerts in one live command view.
              </Typography>
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
                value={filteredAlerts.length}
                caption="alerts returned"
                tone="default"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <SummaryCard
                eyebrow="Police"
                value={sourceCounts.polisen ?? 0}
                caption="incidents"
                tone="accent"
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <SummaryCard
                eyebrow="Weather + crisis"
                value={(sourceCounts.smhi ?? 0) + (sourceCounts.krisinformation ?? 0)}
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
                  <InputLabel id="resource-select-label">Resources</InputLabel>
                  <Select
                    multiple
                    labelId="resource-select-label"
                    input={<OutlinedInput label="Resources" />}
                    value={selectedResources}
                    onChange={(event) => {
                      const value = event.target.value;
                      setSelectedResources(
                        typeof value === 'string' ? value.split(',') : value
                      );
                    }}
                    renderValue={(selected) =>
                      selected.length === 0
                        ? 'All resources'
                        : selected.map((value) => sourceLabels[value] || value).join(', ')
                    }
                  >
                    {resourceOptions.map((option) => (
                      <MenuItem key={option} value={option}>
                        <Checkbox checked={selectedResources.includes(option)} />
                        <ListItemText primary={sourceLabels[option] || option} />
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

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

                <Stack spacing={2}>
                  <FormLabel>Date range</FormLabel>
                  <TextField
                    label="From"
                    type="date"
                    value={dateFrom}
                    onChange={(event) => setDateFrom(event.target.value)}
                    InputLabelProps={{ shrink: true }}
                    fullWidth
                  />
                  <TextField
                    label="To"
                    type="date"
                    value={dateTo}
                    onChange={(event) => setDateTo(event.target.value)}
                    InputLabelProps={{ shrink: true }}
                    fullWidth
                  />
                </Stack>

                <Button
                  variant="outlined"
                  startIcon={<SyncRoundedIcon />}
                  onClick={() => {
                    setCounty('');
                    setSeverity('');
                    setSelectedResources([]);
                    setDateFrom('');
                    setDateTo('');
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
                    {loading
                      ? 'Refreshing feed...'
                      : `${filteredAlerts.length} alerts loaded, page ${page} of ${pageCount}`}
                  </Typography>
                </Stack>

                {error && (
                  <Alert icon={<WarningAmberRoundedIcon />} severity="error">
                    {error}
                  </Alert>
                )}

                <AlertList alerts={paginatedAlerts} />

                {filteredAlerts.length > alertsPerPage && (
                  <Stack alignItems="center" pt={1}>
                    <Pagination
                      color="primary"
                      count={pageCount}
                      page={page}
                      onChange={(_, value) => setPage(value)}
                    />
                  </Stack>
                )}
              </Stack>
            </Paper>
          </Grid>
        </Grid>

        <Paper className="footer-bar" elevation={0}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            justifyContent="space-between"
            spacing={2}
          >
            <Typography color="text.secondary" variant="body2">
              NordAlert monitoring surface for backend alert aggregation and web operations.
            </Typography>
            <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
              <Typography className="footer-bar__tag" variant="body2">
                Web v{webVersion}
              </Typography>
              <Typography className="footer-bar__tag" variant="body2">
                Backend v{backendVersion}
              </Typography>
            </Stack>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}

export default App;
