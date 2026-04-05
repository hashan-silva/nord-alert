import { Linking, Pressable, StyleSheet, Text, View } from 'react-native';
import type { AlertItem } from '../models/alert';
import { formatPublishedAt, severityLabel, sourceLabel } from '../lib/format';

interface AlertCardProps {
  alert: AlertItem;
}

function severityTone(severity: string) {
  switch (severity) {
    case 'high':
      return {
        backgroundColor: '#9f2a2a',
        textColor: '#fff7f4'
      };
    case 'medium':
      return {
        backgroundColor: '#db8b2b',
        textColor: '#1d1308'
      };
    case 'low':
      return {
        backgroundColor: '#d9dd6b',
        textColor: '#24310d'
      };
    default:
      return {
        backgroundColor: '#b8d4e3',
        textColor: '#11313f'
      };
  }
}

export function AlertCard({ alert }: AlertCardProps) {
  const tone = severityTone(alert.severity);
  const areaText = alert.areas && alert.areas.length > 0 ? alert.areas.join(', ') : 'National';

  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={[styles.badge, { backgroundColor: tone.backgroundColor }]}>
          <Text style={[styles.badgeLabel, { color: tone.textColor }]}>
            {severityLabel(alert.severity)}
          </Text>
        </View>
        <Text style={styles.source}>{sourceLabel(alert.source)}</Text>
      </View>

      <Text style={styles.headline}>{alert.headline}</Text>

      <Text style={styles.meta}>{formatPublishedAt(alert.publishedAt)}</Text>
      <Text style={styles.areas}>{areaText}</Text>

      {alert.description ? (
        <Text numberOfLines={5} style={styles.description}>
          {alert.description}
        </Text>
      ) : null}

      {alert.url ? (
        <Pressable
          accessibilityRole="link"
          onPress={() => {
            void Linking.openURL(alert.url!);
          }}
          style={({ pressed }) => [styles.linkButton, pressed ? styles.linkPressed : null]}
        >
          <Text style={styles.linkLabel}>Open source</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fffaf2',
    borderColor: '#e4d7c3',
    borderRadius: 24,
    borderWidth: 1,
    marginBottom: 14,
    padding: 18
  },
  cardHeader: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 14
  },
  badge: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6
  },
  badgeLabel: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.3,
    textTransform: 'uppercase'
  },
  source: {
    color: '#5d5446',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.3,
    textTransform: 'uppercase'
  },
  headline: {
    color: '#1f170f',
    fontSize: 20,
    fontWeight: '800',
    lineHeight: 26,
    marginBottom: 8
  },
  meta: {
    color: '#756958',
    fontSize: 13,
    marginBottom: 4
  },
  areas: {
    color: '#4c4336',
    fontSize: 14,
    fontWeight: '700',
    marginBottom: 10
  },
  description: {
    color: '#3c342c',
    fontSize: 15,
    lineHeight: 22
  },
  linkButton: {
    alignSelf: 'flex-start',
    backgroundColor: '#12343b',
    borderRadius: 999,
    marginTop: 14,
    paddingHorizontal: 14,
    paddingVertical: 10
  },
  linkPressed: {
    opacity: 0.84
  },
  linkLabel: {
    color: '#f5fbfd',
    fontSize: 13,
    fontWeight: '700'
  }
});
