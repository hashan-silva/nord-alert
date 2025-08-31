import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsState {
  final bool loaded;
  final String baseUrl;
  const SettingsState({required this.loaded, required this.baseUrl});

  SettingsState copyWith({bool? loaded, String? baseUrl}) =>
      SettingsState(loaded: loaded ?? this.loaded, baseUrl: baseUrl ?? this.baseUrl);
}

class SettingsCubit extends Cubit<SettingsState> {
  SettingsCubit() : super(const SettingsState(loaded: false, baseUrl: 'http://localhost:3000'));

  static const _kBaseUrl = 'base_url';

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final url = prefs.getString(_kBaseUrl) ?? 'http://localhost:3000';
    emit(SettingsState(loaded: true, baseUrl: url));
  }

  Future<void> saveBaseUrl(String url) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_kBaseUrl, url);
    emit(state.copyWith(baseUrl: url, loaded: true));
  }
}

