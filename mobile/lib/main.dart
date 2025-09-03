import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'api/alerts_api.dart';
import 'bloc/alerts_bloc.dart';
import 'settings/settings_cubit.dart';
import 'models/alert.dart';

void main() {
  runApp(const NordAlertApp());
}

class NordAlertApp extends StatelessWidget {
  const NordAlertApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiRepositoryProvider(
      providers: [RepositoryProvider(create: (_) => AlertsApi())],
      child: MultiBlocProvider(
        providers: [
          BlocProvider(create: (_) => SettingsCubit()..load()),
        ],
        child: MaterialApp(
          title: 'NordAlert',
          theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
          home: BlocBuilder<SettingsCubit, SettingsState>(
            builder: (context, settings) {
              if (!settings.loaded) {
                return const Scaffold(body: Center(child: CircularProgressIndicator()));
              }
              return BlocProvider(
                create: (ctx) => AlertsBloc(ctx.read<AlertsApi>(), initialBaseUrl: settings.baseUrl)
                  ..add(const LoadAlerts()),
                child: const AlertsPage(),
              );
            },
          ),
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
            tooltip: 'Settings',
            icon: const Icon(Icons.settings),
            onPressed: () => _openSettings(context),
          ),
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

Future<void> _openSettings(BuildContext context) async {
  final settings = context.read<SettingsCubit>().state;
  final api = context.read<AlertsApi>();
  final controller = TextEditingController(text: settings.baseUrl);

  String normalize(String raw) {
    var v = raw.trim();
    if (v.isEmpty) return v;
    if (!v.startsWith('http://') && !v.startsWith('https://')) {
      v = 'http://$v';
    }
    if (v.endsWith('/')) v = v.substring(0, v.length - 1);
    return v;
  }

  bool valid(String raw) {
    final v = normalize(raw);
    final u = Uri.tryParse(v);
    return u != null && (u.isScheme('http') || u.isScheme('https')) && (u.host.isNotEmpty);
  }

  final url = await showDialog<String>(
    context: context,
    builder: (ctx) {
      bool isTesting = false;
      bool? testOk;
      return StatefulBuilder(builder: (ctx, setState) {
        Future<void> doTest() async {
          setState(() {
            isTesting = true;
            testOk = null;
          });
          final ok = await api.healthCheck(normalize(controller.text));
          setState(() {
            isTesting = false;
            testOk = ok;
          });
        }

        final isValid = valid(controller.text);
        return AlertDialog(
          title: const Text('Backend Base URL'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: controller,
                decoration: const InputDecoration(
                  labelText: 'e.g., http://192.168.1.100:3000',
                  hintText: 'Protocol required (http/https)',
                ),
                keyboardType: TextInputType.url,
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 8),
              if (!isValid)
                const Text(
                  'Enter a valid URL with protocol',
                  style: TextStyle(color: Colors.red),
                )
              else if (testOk != null)
                Text(
                  testOk == true ? 'Connection OK' : 'Connection failed',
                  style: TextStyle(color: testOk == true ? Colors.green : Colors.red),
                ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancel')),
            TextButton(
              onPressed: isValid && !isTesting ? doTest : null,
              child: isTesting
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Test'),
            ),
            FilledButton(
              onPressed: isValid
                  ? () => Navigator.of(ctx).pop(normalize(controller.text))
                  : null,
              child: const Text('Save'),
            ),
          ],
        );
      });
    },
  );
  if (url != null && url.isNotEmpty) {
    await context.read<SettingsCubit>().saveBaseUrl(url);
    context.read<AlertsBloc>()
      ..add(UpdateBaseUrl(url))
      ..add(LoadAlerts(county: context.read<AlertsBloc>().state.selectedCounty));
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
                        a.publishedAt.toLocal().toString(),
                      ].where((e) => e.isNotEmpty).join(' • ')),
                      trailing: SeverityChip(severity: a.severity),
                      onTap: () async {
                        if (!context.mounted) return;
                        await Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => AlertDetailsPage(alert: a)),
                        );
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

class AlertDetailsPage extends StatelessWidget {
  const AlertDetailsPage({super.key, required this.alert});
  final Alert alert;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Alert Details')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _SourceIcon(source: alert.source),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    alert.headline,
                    style: theme.textTheme.titleLarge,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 4,
              children: [
                if (alert.areas.isNotEmpty)
                  Chip(label: Text(alert.areas.join(', '))),
                SeverityChip(severity: alert.severity),
                Chip(label: Text(alert.publishedAt.toLocal().toString())),
              ],
            ),
            const SizedBox(height: 16),
            if ((alert.description ?? '').isNotEmpty) ...[
              Text('Description', style: theme.textTheme.titleMedium),
              const SizedBox(height: 8),
              Text(alert.description!),
              const SizedBox(height: 16),
            ],
            Text('Source', style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            SelectableText(alert.url.isNotEmpty ? alert.url : 'No URL provided'),
          ],
        ),
      ),
    );
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
    final color = _colorForSource(source, Theme.of(context));
    final label = () {
      switch (source) {
        case AlertSource.polisen:
          return 'P';
        case AlertSource.smhi:
          return 'S';
        case AlertSource.krisinformation:
          return 'K';
      }
    }();
    return CircleAvatar(
      backgroundColor: color,
      foregroundColor: Colors.white,
      child: Text(label),
    );
  }
}

Color _colorForSource(AlertSource source, ThemeData theme) {
  switch (source) {
    case AlertSource.polisen:
      return Colors.indigo;
    case AlertSource.smhi:
      return Colors.orange;
    case AlertSource.krisinformation:
      return Colors.red;
  }
}

class SeverityChip extends StatelessWidget {
  const SeverityChip({super.key, required this.severity});
  final String severity;

  @override
  Widget build(BuildContext context) {
    final color = _colorForSeverity(severity);
    return Chip(
      label: Text(_labelForSeverity(severity)),
      labelStyle: TextStyle(color: color, fontWeight: FontWeight.w600),
      backgroundColor: color.withOpacity(0.12),
      visualDensity: VisualDensity.compact,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }
}

String _labelForSeverity(String s) {
  final t = s.trim();
  if (t.isEmpty) return 'info';
  return t;
}

Color _colorForSeverity(String s) {
  final v = s.toLowerCase();
  if (v.contains('extreme') || v.contains('critical')) return Colors.red;
  if (v.contains('severe') || v.contains('major') || v.contains('high') || v.contains('warning')) {
    return Colors.deepOrange;
  }
  if (v.contains('moderate') || v.contains('medium')) return Colors.amber[800]!;
  if (v.contains('low') || v.contains('minor') || v.contains('info')) return Colors.green[700]!;
  return Colors.grey[700]!;
}
