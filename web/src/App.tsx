import { useEffect, useMemo, useState } from 'react';
import SyncRoundedIcon from '@mui/icons-material/SyncRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import {
  Alert,
  CircularProgress,
  Box,
  Button,
  Checkbox,
  Container,
  FormControl,
  FormLabel,
  Grid,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  InputLabel,
  ListItemText,
  MenuItem,
  OutlinedInput,
  Pagination,
  Paper,
  Select,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography
} from '@mui/material';
import { type SelectChangeEvent } from '@mui/material/Select';
import AlertMap from './components/AlertMap';
import AlertList from './components/AlertList';
import SummaryCard from './components/SummaryCard';
import { baseUrl, createSubscription, fetchAlerts, fetchCounties } from './lib/api';
import {
  normalizeResourceKey,
  resourceLabels,
  resourceOptions as allResourceOptions
} from './lib/alertMeta';
import type { AlertItem } from './models/alert';
import type { CountyItem } from './models/county';
import type { SubscriptionItem } from './models/subscription';

const webVersion = process.env.REACT_APP_WEB_VERSION || 'unknown';
const backendVersion = process.env.REACT_APP_BACKEND_VERSION || 'unknown';

const severityOptions = [
  { label: 'All severities', value: '' },
  { label: 'Info and above', value: 'info' },
  { label: 'Medium and above', value: 'medium' },
  { label: 'High only', value: 'high' }
];
const alertsPerPage = 6;
function App() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [countyOptions, setCountyOptions] = useState<CountyItem[]>([]);
  const [selectedCounties, setSelectedCounties] = useState<string[]>([]);
  const [viewMode, setViewMode] = useState<'map' | 'list'>('map');
  const [severity, setSeverity] = useState('');
  const [selectedResources, setSelectedResources] = useState<string[]>([]);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [alertError, setAlertError] = useState('');
  const [countyError, setCountyError] = useState('');
  const [subscriptionEmail, setSubscriptionEmail] = useState('');
  const [subscriptionCounties, setSubscriptionCounties] = useState<string[]>([]);
  const [subscriptionSeverity, setSubscriptionSeverity] = useState('');
  const [subscriptionSources, setSubscriptionSources] = useState<string[]>([]);
  const [subscriptionSaving, setSubscriptionSaving] = useState(false);
  const [subscriptionError, setSubscriptionError] = useState('');
  const [subscriptionSuccess, setSubscriptionSuccess] = useState('');
  const [subscriptionDialogOpen, setSubscriptionDialogOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadAlerts() {
      setLoading(true);
      setAlertError('');

      try {
        const nextAlerts = await fetchAlerts({ counties: selectedCounties, severity });
        if (!cancelled) {
          setAlerts(nextAlerts);
        }
      } catch (nextError) {
        if (!cancelled) {
          setAlertError(nextError instanceof Error ? nextError.message : 'Unable to load alerts');
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
  }, [selectedCounties, severity]);

  useEffect(() => {
    let cancelled = false;

    async function loadCounties() {
      try {
        const nextCounties = await fetchCounties();
        if (!cancelled) {
          setCountyOptions(nextCounties);
        }
      } catch (nextError) {
        if (!cancelled) {
          setCountyError(nextError instanceof Error ? nextError.message : 'Unable to load counties');
        }
      }
    }

    loadCounties();

    return () => {
      cancelled = true;
    };
  }, []);

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
      const sourceKey = normalizeResourceKey(alert.source);
      counts[sourceKey] = (counts[sourceKey] || 0) + 1;
      return counts;
    }, {});
  }, [filteredAlerts]);

  useEffect(() => {
    setPage(1);
  }, [alerts, selectedCounties, severity, selectedResources, dateFrom, dateTo]);

  useEffect(() => {
    if (page > pageCount) {
      setPage(pageCount);
    }
  }, [page, pageCount]);

  function handleCountyChange(event: SelectChangeEvent<string[]>) {
    const value = event.target.value;
    setSelectedCounties(typeof value === 'string' ? value.split(',') : value);
  }

  function handleSubscriptionCountyChange(event: SelectChangeEvent<string[]>) {
    const value = event.target.value;
    setSubscriptionCounties(typeof value === 'string' ? value.split(',') : value);
  }

  async function handleCreateSubscription() {
    setSubscriptionSaving(true);
    setSubscriptionError('');
    setSubscriptionSuccess('');

    try {
      const subscription: SubscriptionItem = await createSubscription({
        email: subscriptionEmail,
        counties: subscriptionCounties,
        severity: subscriptionSeverity,
        sources: subscriptionSources
      });
      setSubscriptionSuccess(
        `Subscription created for ${subscription.email}. Check the AWS SES verification email to activate alert delivery.`
      );
      setSubscriptionEmail('');
      setSubscriptionDialogOpen(false);
    } catch (nextError) {
      setSubscriptionError(
        nextError instanceof Error ? nextError.message : 'Unable to create subscription'
      );
    } finally {
      setSubscriptionSaving(false);
    }
  }

  const error = countyError || alertError;

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
                    Narrow the feed by counties and severity threshold.
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
                        : selected
                            .map(
                              (value) => resourceLabels[normalizeResourceKey(value)] || value
                            )
                            .join(', ')
                    }
                  >
                    {resourceOptions.map((option) => (
                      <MenuItem key={option} value={option}>
                        <Checkbox checked={selectedResources.includes(option)} />
                        <ListItemText
                          primary={resourceLabels[normalizeResourceKey(option)] || option}
                        />
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth>
                  <InputLabel id="county-select-label">Counties</InputLabel>
                  <Select
                    labelId="county-select-label"
                    multiple
                    input={<OutlinedInput label="Counties" />}
                    value={selectedCounties}
                    onChange={handleCountyChange}
                    renderValue={(selected) =>
                      selected.length === 0 ? 'All counties' : selected.join(', ')
                    }
                  >
                    {countyOptions.map((option) => (
                      <MenuItem key={option.code} value={option.name}>
                        <Checkbox checked={selectedCounties.includes(option.name)} />
                        <ListItemText primary={option.name} />
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
                    setSelectedCounties([]);
                    setSeverity('');
                    setSelectedResources([]);
                    setDateFrom('');
                    setDateTo('');
                  }}
                >
                  Reset filters
                </Button>

                {subscriptionSuccess && <Alert severity="success">{subscriptionSuccess}</Alert>}

                <Button
                  variant="contained"
                  onClick={() => {
                    setSubscriptionError('');
                    setSubscriptionDialogOpen(true);
                  }}
                >
                  Email subscription
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
                  alignItems={{ xs: 'flex-start', sm: 'center' }}
                >
                  <Box>
                    <Typography variant="h3">Live feed</Typography>
                    <Typography color="text.secondary">
                      Aggregated official alerts from the backend `/alerts` endpoint.
                    </Typography>
                  </Box>
                  <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1.5}
                    alignItems={{ xs: 'flex-start', sm: 'center' }}
                  >
                    <ToggleButtonGroup
                      size="small"
                      exclusive
                      color="primary"
                      value={viewMode}
                      onChange={(_, nextView) => {
                        if (nextView) {
                          setViewMode(nextView);
                        }
                      }}
                    >
                      <ToggleButton value="list">List</ToggleButton>
                      <ToggleButton value="map">Map</ToggleButton>
                    </ToggleButtonGroup>
                    <Typography color="text.secondary" variant="body2">
                      {loading
                        ? 'Refreshing feed...'
                        : `${filteredAlerts.length} alerts loaded${viewMode === 'list' ? `, page ${page} of ${pageCount}` : ''}`}
                    </Typography>
                  </Stack>
                </Stack>

                {error && (
                  <Alert icon={<WarningAmberRoundedIcon />} severity="error">
                    {error}
                  </Alert>
                )}

                {loading ? (
                  <Box className="loading-state">
                    <CircularProgress color="primary" size={48} thickness={4.5} />
                    <Typography color="text.secondary" variant="body1">
                      Loading alerts...
                    </Typography>
                  </Box>
                ) : (
                  <>
                    {viewMode === 'map' ? (
                      <AlertMap alerts={filteredAlerts} />
                    ) : (
                      <>
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
                      </>
                    )}
                  </>
                )}
              </Stack>
            </Paper>
          </Grid>
        </Grid>

        <Box className="app-footer">
          <Typography color="text.secondary" variant="body2">
            NordAlert © Shermal Hashan Silva · Web {webVersion} · Backend {backendVersion}
          </Typography>
        </Box>

        <Dialog
          open={subscriptionDialogOpen}
          onClose={() => {
            if (!subscriptionSaving) {
              setSubscriptionDialogOpen(false);
            }
          }}
          fullWidth
          maxWidth="sm"
        >
          <DialogTitle>Email subscription</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} pt={1}>
              <Typography color="text.secondary" variant="body2">
                Store a recurring alert subscription. AWS SES will send a verification email
                before alert delivery is activated.
              </Typography>

              {subscriptionError && <Alert severity="error">{subscriptionError}</Alert>}

              <TextField
                label="Email"
                type="email"
                value={subscriptionEmail}
                onChange={(event) => setSubscriptionEmail(event.target.value)}
                fullWidth
              />

              <FormControl fullWidth>
                <InputLabel id="subscription-resource-select-label">Resources</InputLabel>
                <Select
                  multiple
                  labelId="subscription-resource-select-label"
                  input={<OutlinedInput label="Resources" />}
                  value={subscriptionSources}
                  onChange={(event) => {
                    const value = event.target.value;
                    setSubscriptionSources(typeof value === 'string' ? value.split(',') : value);
                  }}
                  renderValue={(selected) =>
                    selected.length === 0
                      ? 'All resources'
                      : selected
                          .map((value) => resourceLabels[normalizeResourceKey(value)] || value)
                          .join(', ')
                  }
                >
                  {allResourceOptions.map((option) => (
                    <MenuItem key={option} value={option}>
                      <Checkbox checked={subscriptionSources.includes(option)} />
                      <ListItemText
                        primary={resourceLabels[normalizeResourceKey(option)] || option}
                      />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl fullWidth>
                <InputLabel id="subscription-county-select-label">Counties</InputLabel>
                <Select
                  labelId="subscription-county-select-label"
                  multiple
                  input={<OutlinedInput label="Counties" />}
                  value={subscriptionCounties}
                  onChange={handleSubscriptionCountyChange}
                  renderValue={(selected) =>
                    selected.length === 0 ? 'All counties' : selected.join(', ')
                  }
                >
                  {countyOptions.map((option) => (
                    <MenuItem key={`subscription-${option.code}`} value={option.name}>
                      <Checkbox checked={subscriptionCounties.includes(option.name)} />
                      <ListItemText primary={option.name} />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <FormControl fullWidth>
                <InputLabel id="subscription-severity-select-label">Severity</InputLabel>
                <Select
                  labelId="subscription-severity-select-label"
                  label="Severity"
                  value={subscriptionSeverity}
                  onChange={(event) => setSubscriptionSeverity(event.target.value)}
                >
                  {severityOptions.map((option) => (
                    <MenuItem key={`subscription-${option.label}`} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button
              variant="outlined"
              onClick={() => {
                setSubscriptionCounties(selectedCounties);
                setSubscriptionSeverity(severity);
                setSubscriptionSources(selectedResources);
              }}
            >
              Use current filters
            </Button>
            <Box sx={{ flex: 1 }} />
            <Button
              onClick={() => {
                if (!subscriptionSaving) {
                  setSubscriptionDialogOpen(false);
                }
              }}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handleCreateSubscription}
              disabled={subscriptionSaving || subscriptionEmail.trim().length === 0}
            >
              {subscriptionSaving ? 'Saving...' : 'Subscribe'}
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </Box>
  );
}

export default App;
