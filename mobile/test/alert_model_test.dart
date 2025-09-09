import 'package:flutter_test/flutter_test.dart';
import 'package:nord_alert_mobile/models/alert.dart';

void main() {
  test('Alert model parses from JSON', () { 
    final json = {
      'id': 'abc123',
      'source': 'polisen',
      'headline': 'Test headline',
      'description': 'Details',
      'areas': ['Stockholm'],
      'severity': 'info',
      'publishedAt': '2024-01-01T00:00:00.000Z',
      'url': 'https://example.com',
    };

    final a = Alert.fromJson(json);
    expect(a.id, 'abc123');
    expect(a.source, AlertSource.polisen);
    expect(a.headline, 'Test headline');
    expect(a.description, 'Details');
    expect(a.areas, ['Stockholm']);
    expect(a.severity, 'info');
    expect(a.url, 'https://example.com');
  });

  test('Alert model handles missing attributes', () {
    final json = {
      'id': 'abc123',
      'source': 'polisen',
    };

    final a = Alert.fromJson(json);
    expect(a.id, 'abc123');
    expect(a.source, AlertSource.polisen);
    expect(a.headline, '');
    expect(a.description, null);
    expect(a.areas, <String>[]);
    expect(a.severity, 'info');
    expect(a.url, '');
    expect(a.publishedAt, isA<DateTime>());
  });
}

