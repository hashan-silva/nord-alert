import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'api/alerts_api.dart';
import 'bloc/alerts_bloc.dart';
import 'models/alert.dart';

void main() {
  runApp(const NordAlertApp());
}

class NordAlertApp extends StatelessWidget {
  const NordAlertApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NordAlert',
      theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
      home: RepositoryProvider(
        create: (_) => AlertsApi(),
        child: BlocProvider(
          create: (ctx) => AlertsBloc(ctx.read<AlertsApi>())..add(const LoadAlerts()),
          child: const AlertsPage(),
        ),
      ),
    );
  }
}

class AlertsPage extends StatelessWidget {
  const AlertsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('NordAlert'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<AlertsBloc>().add(LoadAlerts(county: context.read<AlertsBloc>().state.selectedCounty)),
          ),
        ],
      ),
      body: const _AlertsBody(),
    );
  }
}

class _AlertsBody extends StatelessWidget {
  const _AlertsBody();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<AlertsBloc, AlertsState>(
      builder: (context, state) {
        if (state.loading) {
          return const Center(child: CircularProgressIndicator());
        }
        if (state.error != null) {
          return Center(child: Text('Error: ${state.error}'));
        }
        final counties = _extractCounties(state.all);
        return Column(
          children: [
            _Filters(
              counties: counties,
              selectedCounty: state.selectedCounty,
              selectedSources: state.selectedSources,
            ),
            const Divider(height: 1),
            Expanded(
              child: RefreshIndicator(
                onRefresh: () async {
                  context.read<AlertsBloc>().add(LoadAlerts(county: state.selectedCounty));
                },
                child: ListView.separated(
                  itemCount: state.filtered.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, i) {
                    final a = state.filtered[i];
                    return ListTile(
                      leading: _SourceIcon(source: a.source),
                      title: Text(a.headline),
                      subtitle: Text([
                        if (a.areas.isNotEmpty) a.areas.join(', '),
                        a.severity,
                        a.publishedAt.toLocal().toString(),
                      ].where((e) => e.isNotEmpty).join(' • ')),
                      onTap: () async {
                        // noop; could launch URL
                      },
                    );
                  },
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  List<String> _extractCounties(List<Alert> alerts) {
    final set = <String>{};
    for (final a in alerts) {
      set.addAll(a.areas);
    }
    final list = set.toList()..sort();
    return list;
  }
}

class _Filters extends StatelessWidget {
  const _Filters({
    required this.counties,
    required this.selectedCounty,
    required this.selectedSources,
  });

  final List<String> counties;
  final String? selectedCounty;
  final Set<AlertSource> selectedSources;

  @override
  Widget build(BuildContext context) {
    final bloc = context.read<AlertsBloc>();
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('County:'),
              const SizedBox(width: 12),
              Expanded(
                child: DropdownButton<String>(
                  isExpanded: true,
                  value: selectedCounty?.isNotEmpty == true ? selectedCounty : null,
                  hint: const Text('All counties'),
                  items: [
                    const DropdownMenuItem<String>(value: '', child: Text('All counties')),
                    ...counties.map((c) => DropdownMenuItem<String>(value: c, child: Text(c))),
                  ],
                  onChanged: (v) {
                    final value = (v ?? '').isEmpty ? null : v;
                    // refetch from backend with county filter to reduce payload
                    bloc.add(LoadAlerts(county: value));
                    bloc.add(UpdateFilters(county: value));
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: AlertSource.values.map((s) {
              final selected = selectedSources.contains(s);
              return FilterChip(
                label: Text(_labelForSource(s)),
                selected: selected,
                onSelected: (on) {
                  final next = Set<AlertSource>.from(selectedSources);
                  if (on) {
                    next.add(s);
                  } else {
                    next.remove(s);
                  }
                  bloc.add(UpdateFilters(sources: next));
                },
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  String _labelForSource(AlertSource s) {
    switch (s) {
      case AlertSource.polisen:
        return 'Polisen';
      case AlertSource.smhi:
        return 'SMHI';
      case AlertSource.krisinformation:
        return 'Krisinformation';
    }
  }
}

class _SourceIcon extends StatelessWidget {
  const _SourceIcon({required this.source});
  final AlertSource source;

  @override
  Widget build(BuildContext context) {
    switch (source) {
      case AlertSource.polisen:
        return const CircleAvatar(child: Text('P'));
      case AlertSource.smhi:
        return const CircleAvatar(child: Text('S'));
      case AlertSource.krisinformation:
        return const CircleAvatar(child: Text('K'));
    }
  }
}

