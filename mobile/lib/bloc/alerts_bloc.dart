import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../api/alerts_api.dart';
import '../models/alert.dart';

// Events
abstract class AlertsEvent extends Equatable {
  const AlertsEvent();
  @override
  List<Object?> get props => [];
}

class LoadAlerts extends AlertsEvent {
  final String? county;
  const LoadAlerts({this.county});
  @override
  List<Object?> get props => [county];
}

class UpdateBaseUrl extends AlertsEvent {
  final String baseUrl;
  const UpdateBaseUrl(this.baseUrl);
  @override
  List<Object?> get props => [baseUrl];
}

class UpdateFilters extends AlertsEvent {
  final String? county;
  final Set<AlertSource>? sources;
  const UpdateFilters({this.county, this.sources});
  @override
  List<Object?> get props => [county, sources];
}

// State
class AlertsState extends Equatable {
  final bool loading;
  final String? error;
  final List<Alert> all;
  final List<Alert> filtered;
  final String? selectedCounty;
  final Set<AlertSource> selectedSources;
  final String baseUrl;

  const AlertsState({
    required this.loading,
    required this.error,
    required this.all,
    required this.filtered,
    required this.selectedCounty,
    required this.selectedSources,
    required this.baseUrl,
  });

  factory AlertsState.initial(String baseUrl) => AlertsState(
        loading: false,
        error: null,
        all: const [],
        filtered: const [],
        selectedCounty: null,
        selectedSources: const {AlertSource.polisen, AlertSource.smhi, AlertSource.krisinformation},
        baseUrl: baseUrl,
      );

  AlertsState copyWith({
    bool? loading,
    String? error,
    List<Alert>? all,
    List<Alert>? filtered,
    String? selectedCounty,
    Set<AlertSource>? selectedSources,
    String? baseUrl,
  }) => AlertsState(
        loading: loading ?? this.loading,
        error: error,
        all: all ?? this.all,
        filtered: filtered ?? this.filtered,
        selectedCounty: selectedCounty ?? this.selectedCounty,
        selectedSources: selectedSources ?? this.selectedSources,
        baseUrl: baseUrl ?? this.baseUrl,
      );

  @override
  List<Object?> get props => [loading, error, all, filtered, selectedCounty, selectedSources, baseUrl];
}

class AlertsBloc extends Bloc<AlertsEvent, AlertsState> {
  AlertsBloc(this._api, {required String initialBaseUrl}) : super(AlertsState.initial(initialBaseUrl)) {
    on<LoadAlerts>(_onLoad);
    on<UpdateFilters>(_onUpdateFilters);
    on<UpdateBaseUrl>(_onUpdateBaseUrl);
  }

  final AlertsApi _api;

  Future<void> _onLoad(LoadAlerts e, Emitter<AlertsState> emit) async {
    emit(state.copyWith(loading: true, error: null));
    try {
      final items = await _api.fetchAlerts(baseUrl: state.baseUrl, county: e.county);
      final filtered = _applyFilters(items, state.selectedCounty ?? e.county, state.selectedSources);
      emit(state.copyWith(
        loading: false,
        error: null,
        all: items,
        filtered: filtered,
        selectedCounty: e.county ?? state.selectedCounty,
      ));
    } catch (err) {
      emit(state.copyWith(loading: false, error: err.toString()));
    }
  }

  void _onUpdateBaseUrl(UpdateBaseUrl e, Emitter<AlertsState> emit) {
    emit(state.copyWith(baseUrl: e.baseUrl));
  }

  void _onUpdateFilters(UpdateFilters e, Emitter<AlertsState> emit) {
    final nextCounty = e.county ?? state.selectedCounty;
    final nextSources = e.sources ?? state.selectedSources;
    final filtered = _applyFilters(state.all, nextCounty, nextSources);
    emit(state.copyWith(
      filtered: filtered,
      selectedCounty: nextCounty,
      selectedSources: nextSources,
    ));
  }

  List<Alert> _applyFilters(List<Alert> all, String? county, Set<AlertSource> sources) {
    return all.where((a) {
      final countyOk = county == null || county.isEmpty || a.areas.contains(county);
      final providerOk = sources.contains(a.source);
      return countyOk && providerOk;
    }).toList()
      ..sort((a, b) => b.publishedAt.compareTo(a.publishedAt));
  }
}
