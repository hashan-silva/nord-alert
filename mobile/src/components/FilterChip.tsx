import { Pressable, StyleSheet, Text } from 'react-native';

interface FilterChipProps {
  active: boolean;
  label: string;
  onPress: () => void;
}

export function FilterChip({ active, label, onPress }: FilterChipProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected: active }}
      onPress={onPress}
      style={({ pressed }) => [
        styles.chip,
        active ? styles.chipActive : styles.chipInactive,
        pressed ? styles.chipPressed : null
      ]}
    >
      <Text style={[styles.label, active ? styles.labelActive : styles.labelInactive]}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  chip: {
    borderRadius: 999,
    borderWidth: 1,
    marginBottom: 10,
    marginRight: 10,
    paddingHorizontal: 14,
    paddingVertical: 10
  },
  chipActive: {
    backgroundColor: '#12343b',
    borderColor: '#12343b'
  },
  chipInactive: {
    backgroundColor: '#f5f1e8',
    borderColor: '#d9cfc0'
  },
  chipPressed: {
    opacity: 0.86
  },
  label: {
    fontSize: 13,
    fontWeight: '700'
  },
  labelActive: {
    color: '#f8faf9'
  },
  labelInactive: {
    color: '#3d3427'
  }
});
