import 'package:dio/dio.dart';
import '../models/alert.dart';

class AlertsApi {
  AlertsApi([Dio? client])
      : _dio = client ??
            Dio(BaseOptions(
              // Short, sensible defaults for mobile
              receiveTimeout: const Duration(seconds: 5),
              sendTimeout: const Duration(seconds: 5),
            ));

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

  Future<bool> healthCheck(String baseUrl) async {
    try {
      final uri = Uri.parse('$baseUrl/health');
      final resp = await _dio.getUri(
        uri,
        options: Options(
          followRedirects: false,
          validateStatus: (code) => code != null && code >= 200 && code < 500,
          sendTimeout: const Duration(seconds: 3),
          receiveTimeout: const Duration(seconds: 3),
        ),
      );
      return resp.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
