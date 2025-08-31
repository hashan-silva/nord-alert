import 'package:dio/dio.dart';
import '../models/alert.dart';

class AlertsApi {
  AlertsApi([Dio? client]) : _dio = client ?? Dio();

  final Dio _dio;

  Future<List<Alert>> fetchAlerts({required String baseUrl, String? county}) async {
    final uri = Uri.parse('$baseUrl/alerts').replace(
      queryParameters: {
        if (county != null && county.isNotEmpty) 'county': county,
      },
    );
    final resp = await _dio.getUri(uri);
    final data = resp.data as List<dynamic>;
    return data.map((e) => Alert.fromJson(e as Map<String, dynamic>)).toList();
  }
}
