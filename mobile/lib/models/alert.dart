// Dart model matching the backend's /alerts response

enum AlertSource { polisen, smhi, krisinformation }

AlertSource alertSourceFromString(String s) {
  switch (s) {
    case 'polisen':
      return AlertSource.polisen;
    case 'smhi':
      return AlertSource.smhi;
    case 'krisinformation':
      return AlertSource.krisinformation;
    default:
      return AlertSource.polisen;
  }
}

class Alert {
  final String id;
  final AlertSource source;
  final String headline;
  final String? description;
  final List<String> areas;
  final String severity;
  final DateTime publishedAt;
  final String url;

  Alert({
    required this.id,
    required this.source,
    required this.headline,
    required this.description,
    required this.areas,
    required this.severity,
    required this.publishedAt,
    required this.url,
  });

  factory Alert.fromJson(Map<String, dynamic> json) {
    return Alert(
      id: (json['id'] ?? '') as String,
      source: alertSourceFromString((json['source'] as String?) ?? 'polisen'),
      headline: (json['headline'] ?? '') as String,
      description: json['description'] as String?,
      areas: (json['areas'] as List<dynamic>? ?? const []).map((e) => e.toString()).toList(),
      severity: (json['severity'] ?? 'info') as String,
      publishedAt: DateTime.tryParse(json['publishedAt'] as String? ?? '') ?? DateTime.now(),
      url: (json['url'] ?? '') as String,
    );
  }
}

