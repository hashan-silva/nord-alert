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

  const AlertsState({
    required this.loading,
    required this.error,
    required this.all,
    required this.filtered,
    required this.selectedCounty,
    required this.selectedSources,
  });

  factory AlertsState.initial() => const AlertsState(
        loading: false,
        error: null,
        all: [],
        filtered: [],
        selectedCounty: null,
        selectedSources: {AlertSource.polisen, AlertSource.smhi, AlertSource.krisinformation},
      );

  AlertsState copyWith({
    bool? loading,
    String? error,
    List<Alert>? all,
    List<Alert>? filtered,
    String? selectedCounty,
    Set<AlertSource>? selectedSources,
  }) => AlertsState(
        loading: loading ?? this.loading,
        error: error,
        all: all ?? this.all,
        filtered: filtered ?? this.filtered,
        selectedCounty: selectedCounty ?? this.selectedCounty,
        selectedSources: selectedSources ?? this.selectedSources,
      );

  @override
  List<Object?> get props => [loading, error, all, filtered, selectedCounty, selectedSources];
}

class AlertsBloc extends Bloc<AlertsEvent, AlertsState> {
  AlertsBloc(this._api) : super(AlertsState.initial()) {
    on<LoadAlerts>(_onLoad);
    on<UpdateFilters>(_onUpdateFilters);
  }

  final AlertsApi _api;

  Future<void> _onLoad(LoadAlerts e, Emitter<AlertsState> emit) async {
    emit(state.copyWith(loading: true, error: null));
    try {
      final items = await _api.fetchAlerts(county: e.county);
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

