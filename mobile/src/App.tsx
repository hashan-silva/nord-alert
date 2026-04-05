import { StatusBar } from 'expo-status-bar';
import { useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View
} from 'react-native';
import { AlertCard } from './components/AlertCard';
import { FilterChip } from './components/FilterChip';
import { fetchAlerts, fetchCounties } from './lib/api';
import { baseUrl } from './lib/config';
import { appVersion } from './lib/version';
import type { AlertItem } from './models/alert';
import type { CountyItem } from './models/county';

const severityOptions = [
  { label: 'All', value: '' },
  { label: 'Info+', value: 'info' },
  { label: 'Medium+', value: 'medium' },
  { label: 'High', value: 'high' }
];
export default function App() {
  const [alerts, setAlerts] = useState<AlertItem[]>([]);
  const [counties, setCounties] = useState<CountyItem[]>([]);
  const [selectedCounty, setSelectedCounty] = useState('');
  const [severity, setSeverity] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [alertError, setAlertError] = useState('');
  const [countyError, setCountyError] = useState('');
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);

  async function loadCounties() {
    setCountyError('');

    try {
      const nextCounties = await fetchCounties();
      setCounties(nextCounties);
    } catch (error) {
      setCountyError(error instanceof Error ? error.message : 'Unable to load counties');
    }
  }

  async function loadAlerts(options?: { silent?: boolean }) {
    const silent = options?.silent ?? false;

    if (!silent) {
      setLoading(true);
    }

    setAlertError('');

    try {
      const nextAlerts = await fetchAlerts({ county: selectedCounty, severity });
      setAlerts(nextAlerts);
      setLastUpdated(new Date().toISOString());
    } catch (error) {
      setAlertError(error instanceof Error ? error.message : 'Unable to load alerts');
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  }

  useEffect(() => {
    void loadCounties();
  }, []);

  useEffect(() => {
    void loadAlerts();
  }, [selectedCounty, severity]);

  async function handleRefresh() {
    setRefreshing(true);
    await Promise.all([loadCounties(), loadAlerts({ silent: true })]);
    setRefreshing(false);
  }

  const headerStatus = useMemo(() => {
    if (loading) {
      return 'Loading alert feed';
    }

    if (alertError || countyError) {
      return 'Connection needs attention';
    }

    return `${alerts.length} alerts in the current feed`;
  }, [alertError, alerts.length, countyError, loading]);

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="dark" />
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
      >
        <View style={styles.hero}>
          <Text style={styles.kicker}>NordAlert Mobile</Text>
          <Text style={styles.title}>Swedish public alerts, built for the phone.</Text>
          <Text style={styles.subtitle}>
            Android-ready hybrid client powered by the existing NordAlert backend.
          </Text>

          <View style={styles.heroMeta}>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>{alerts.length}</Text>
              <Text style={styles.statLabel}>Current alerts</Text>
            </View>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>{selectedCounty || 'All'}</Text>
              <Text style={styles.statLabel}>County scope</Text>
            </View>
          </View>

          <Text style={styles.endpointLabel}>Backend</Text>
          <Text style={styles.endpointValue}>{baseUrl}</Text>
          <Text style={styles.statusLine}>{headerStatus}</Text>
        </View>

        <View style={styles.panel}>
          <Text style={styles.panelTitle}>Severity</Text>
          <View style={styles.filterRow}>
            {severityOptions.map((option) => (
              <FilterChip
                key={option.label}
                active={severity === option.value}
                label={option.label}
                onPress={() => setSeverity(option.value)}
              />
            ))}
          </View>
        </View>

        <View style={styles.panel}>
          <Text style={styles.panelTitle}>County</Text>
          <View style={styles.filterRow}>
            <FilterChip
              active={selectedCounty === ''}
              label="All counties"
              onPress={() => setSelectedCounty('')}
            />
            {counties.map((county) => (
              <FilterChip
                key={county.code}
                active={selectedCounty === county.name}
                label={county.name}
                onPress={() => setSelectedCounty(county.name)}
              />
            ))}
          </View>
          {countyError ? <Text style={styles.errorText}>{countyError}</Text> : null}
        </View>

        <View style={styles.feedHeader}>
          <View>
            <Text style={styles.feedTitle}>Live feed</Text>
            <Text style={styles.feedSubtitle}>
              Pull down to refresh. Tap an alert to open the original source.
            </Text>
          </View>
          {lastUpdated ? (
            <Text style={styles.feedTimestamp}>
              Updated {new Date(lastUpdated).toLocaleTimeString()}
            </Text>
          ) : null}
        </View>

        {alertError ? <Text style={styles.errorText}>{alertError}</Text> : null}

        {loading ? (
          <View style={styles.loadingState}>
            <ActivityIndicator size="large" color="#12343b" />
            <Text style={styles.loadingText}>Loading alerts…</Text>
          </View>
        ) : alerts.length === 0 ? (
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No alerts match these filters.</Text>
            <Text style={styles.emptyCopy}>
              Try widening the county scope or lowering the severity threshold.
            </Text>
          </View>
        ) : (
          alerts.map((alert) => <AlertCard key={alert.id} alert={alert} />)
        )}

        <View style={styles.footer}>
          <Text style={styles.footerText}>NordAlert · Version {appVersion}</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: '#eadfce',
    flex: 1
  },
  content: {
    padding: 18,
    paddingBottom: 28
  },
  hero: {
    backgroundColor: '#f7efe2',
    borderRadius: 32,
    marginBottom: 18,
    padding: 22
  },
  kicker: {
    color: '#8a4b2a',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 1.5,
    marginBottom: 10,
    textTransform: 'uppercase'
  },
  title: {
    color: '#1e1710',
    fontSize: 32,
    fontWeight: '900',
    letterSpacing: -0.8,
    lineHeight: 36,
    marginBottom: 10
  },
  subtitle: {
    color: '#564c3d',
    fontSize: 16,
    lineHeight: 23,
    marginBottom: 18
  },
  heroMeta: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 18
  },
  statCard: {
    backgroundColor: '#fffaf2',
    borderRadius: 22,
    flex: 1,
    minHeight: 88,
    padding: 16
  },
  statValue: {
    color: '#12343b',
    fontSize: 24,
    fontWeight: '900',
    marginBottom: 6
  },
  statLabel: {
    color: '#665b4b',
    fontSize: 13,
    fontWeight: '700'
  },
  endpointLabel: {
    color: '#8a4b2a',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 4,
    textTransform: 'uppercase'
  },
  endpointValue: {
    color: '#213c44',
    fontSize: 14,
    fontWeight: '700',
    marginBottom: 8
  },
  statusLine: {
    color: '#665b4b',
    fontSize: 14
  },
  panel: {
    backgroundColor: '#f9f4ea',
    borderRadius: 26,
    marginBottom: 16,
    padding: 18
  },
  panelTitle: {
    color: '#251d15',
    fontSize: 18,
    fontWeight: '800',
    marginBottom: 14
  },
  filterRow: {
    flexDirection: 'row',
    flexWrap: 'wrap'
  },
  feedHeader: {
    alignItems: 'flex-start',
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 14
  },
  feedTitle: {
    color: '#21170f',
    fontSize: 22,
    fontWeight: '900',
    marginBottom: 4
  },
  feedSubtitle: {
    color: '#5b5142',
    fontSize: 14,
    lineHeight: 20,
    maxWidth: 220
  },
  feedTimestamp: {
    color: '#6d624f',
    fontSize: 12,
    fontWeight: '700',
    marginTop: 4
  },
  loadingState: {
    alignItems: 'center',
    backgroundColor: '#f8f1e7',
    borderRadius: 24,
    padding: 32
  },
  loadingText: {
    color: '#5d5243',
    fontSize: 15,
    marginTop: 12
  },
  emptyState: {
    backgroundColor: '#fff8ef',
    borderColor: '#e5d7c4',
    borderRadius: 24,
    borderWidth: 1,
    padding: 24
  },
  emptyTitle: {
    color: '#241a11',
    fontSize: 18,
    fontWeight: '800',
    marginBottom: 8
  },
  emptyCopy: {
    color: '#5f5444',
    fontSize: 15,
    lineHeight: 22
  },
  footer: {
    alignItems: 'center',
    paddingTop: 12
  },
  footerText: {
    color: '#6d624f',
    fontSize: 12,
    fontWeight: '700'
  },
  errorText: {
    color: '#8f2727',
    fontSize: 14,
    fontWeight: '700',
    marginTop: 8
  }
});
