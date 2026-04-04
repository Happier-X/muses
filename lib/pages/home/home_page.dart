import 'dart:async';

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:signals/signals.dart';
import 'package:signals_flutter/signals_flutter.dart' hide computed;

import '../../app/state/settings_state.dart';
import '../../app/services/db/dao/song_dao.dart';
import '../../app/router/app_page_route.dart';
import '../../app/services/library_refresh_service.dart';
import '../../app/services/player_service.dart';
import '../../app/services/webdav/webdav_source_repository.dart';
import '../../app/utils/cache_version_store.dart';
import '../../app/utils/page_cache_store.dart';
import '../../components/index.dart';
import '../library/albums_page.dart';
import '../library/artists_page.dart';
import '../library/playlists_page.dart';
import '../songs/songs_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with SignalsMixin {
  static const String _prefsHomeFilter = 'home_filter';
  static const String _cacheScope = 'home_counts';

  final GlobalKey<AppPageScaffoldState> _scaffoldKey =
      GlobalKey<AppPageScaffoldState>();
  final SongDao _songDao = SongDao();
  final PlayerService _player = PlayerService.instance;
  final LibraryRefreshService _libraryRefreshService =
      LibraryRefreshService.instance;
  final WebDavSourceRepository _webDavRepo = WebDavSourceRepository.instance;
  final PageCacheStore _cacheStore = PageCacheStore.instance;
  bool _autoPlayTried = false;
  bool _libraryRefreshTried = false;

  late final _filter = createSignal('all');
  late final _loading = createSignal(true);
  late final _countAll = createSignal(0);
  late final _countLocal = createSignal(0);
  late final _countRemote = createSignal(0);
  late final _webDavSources = createSignal<List<WebDavSource>>([]);
  late final _webDavCounts = createSignal<Map<String, int>>({});

  late final _webDavNameMap = computed<Map<String, String>>(() {
    final map = <String, String>{};
    for (final s in _webDavSources.value) {
      final name = s.name.trim().isEmpty ? 'WebDAV' : s.name.trim();
      map[s.id] = name;
    }
    return map;
  });

  late final _filterTitle = computed<String>(() {
    final filter = _filter.value;
    if (filter == 'local') return '本地音乐';
    if (filter == 'webdav') return '云端（全部）';
    if (filter.startsWith('webdav:')) {
      final id = filter.substring('webdav:'.length);
      final name = _webDavNameMap.value[id];
      return '云端：${(name ?? id).trim()}';
    }
    return '全部';
  });

  late final _filterCount = computed<int>(() {
    final filter = _filter.value;
    if (filter == 'local') return _countLocal.value;
    if (filter == 'webdav') return _countRemote.value;
    if (filter.startsWith('webdav:')) {
      final id = filter.substring('webdav:'.length);
      return _webDavCounts.value[id] ?? 0;
    }
    return _countAll.value;
  });

  @override
  void initState() {
    super.initState();
    unawaited(_tryAutoPlayOnAppLaunch());
    unawaited(_tryRefreshLibraryOnLaunch());
    _load();
  }

  Future<void> _tryAutoPlayOnAppLaunch() async {
    if (_autoPlayTried) return;
    _autoPlayTried = true;
    await AppLaunchPlaybackSettings.ensureLoaded();
    if (!mounted || !AppLaunchPlaybackSettings.autoPlayOnAppLaunch.value) {
      return;
    }
    var attempts = 0;
    while (_player.currentSong.value == null && attempts < 8) {
      await Future.delayed(const Duration(milliseconds: 200));
      if (!mounted) return;
      attempts += 1;
    }
    if (_player.currentSong.value == null || _player.isPlaying.value) return;
    try {
      await _player.play();
    } catch (e) {
      debugPrint('App auto play on launch failed: $e');
    }
  }

  Future<void> _tryRefreshLibraryOnLaunch() async {
    if (_libraryRefreshTried) return;
    _libraryRefreshTried = true;

    await Future.delayed(const Duration(milliseconds: 500));
    if (!mounted) return;

    final result = await _libraryRefreshService.refreshOnLaunch();
    if (!mounted || result == null) return;
    if (!result.hasChanges) return;

    await _load(includeWebDavCounts: true);
    if (!mounted) return;

    final parts = <String>[];
    if (result.localAdded > 0) {
      parts.add('本地 ${result.localAdded} 首');
    }
    if (result.cloudAdded > 0) {
      parts.add('云端 ${result.cloudAdded} 首');
    }
    final detail = parts.join('，');
    AppToast.show(context, '已自动刷新音源，新增 $detail', type: ToastType.success);
  }

  Future<void> _load({bool includeWebDavCounts = false}) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_prefsHomeFilter) ?? 'all';
    final cacheKey =
        'songv:${CacheVersionStore.instance.getVersion(SongDao.cacheVersionScope)}';

    final cached = _cacheStore.get<_HomeCountsCache>(_cacheScope, cacheKey);
    if (cached != null) {
      _countAll.value = cached.countAll;
      _countLocal.value = cached.countLocal;
      _countRemote.value = cached.countRemote;
      _webDavSources.value = cached.webDavSources;
      _webDavCounts.value = cached.webDavCounts;
      _loading.value = false;
    }

    await _refreshData(
      cacheKey: cacheKey,
      rawFilter: raw,
      includeWebDavCounts: includeWebDavCounts,
    );
  }

  Future<void> _refreshData({
    required String cacheKey,
    required String rawFilter,
    required bool includeWebDavCounts,
  }) async {
    final countsFuture = Future.wait<int>([
      _songDao.countAll(),
      _songDao.countLocal(),
      _songDao.countRemote(),
    ]);
    final sourcesFuture = _webDavRepo.loadSources();

    final counts = await countsFuture;
    final sources = await sourcesFuture;

    Map<String, int> webdavCounts;
    if (includeWebDavCounts) {
      final entries = await Future.wait(
        sources.map(
          (s) async =>
              MapEntry<String, int>(s.id, await _songDao.countBySource(s.id)),
        ),
      );
      webdavCounts = {for (final e in entries) e.key: e.value};
    } else {
      webdavCounts =
          _cacheStore
              .get<_HomeCountsCache>(_cacheScope, cacheKey)
              ?.webDavCounts ??
          const {};
    }

    var filter = rawFilter;
    if (filter.startsWith('webdav:')) {
      final id = filter.substring('webdav:'.length);
      if (!sources.any((s) => s.id == id)) {
        filter = 'webdav';
      }
    } else if (filter != 'local' && filter != 'webdav' && filter != 'all') {
      filter = 'all';
    }
    if (!mounted) return;

    _cacheStore.set(
      _cacheScope,
      cacheKey,
      _HomeCountsCache(
        countAll: counts[0],
        countLocal: counts[1],
        countRemote: counts[2],
        webDavSources: sources,
        webDavCounts: webdavCounts,
      ),
    );

    _filter.value = filter;
    _countAll.value = counts[0];
    _countLocal.value = counts[1];
    _countRemote.value = counts[2];
    _webDavSources.value = sources;
    _webDavCounts.value = webdavCounts;
    _loading.value = false;
  }

  Future<void> _refreshWebDavCounts() async {
    final sources = await _webDavRepo.loadSources();
    final entries = await Future.wait(
      sources.map(
        (s) async =>
            MapEntry<String, int>(s.id, await _songDao.countBySource(s.id)),
      ),
    );
    if (!mounted) return;
    final webdavCounts = {for (final e in entries) e.key: e.value};
    final cacheKey =
        'songv:${CacheVersionStore.instance.getVersion(SongDao.cacheVersionScope)}';
    final previous = _cacheStore.get<_HomeCountsCache>(_cacheScope, cacheKey);
    if (previous != null) {
      _cacheStore.set(
        _cacheScope,
        cacheKey,
        previous.copyWith(webDavSources: sources, webDavCounts: webdavCounts),
      );
    }
    _webDavSources.value = sources;
    _webDavCounts.value = webdavCounts;
  }

  Future<void> _setFilter(String next) async {
    _filter.value = next;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsHomeFilter, next);
  }

  Future<void> _showSourceSheet() async {
    await _refreshWebDavCounts();
    if (!mounted) return;
    final sources = _webDavSources.value;

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (context) {
        final items = [
          const _HomeSourceItem(label: '全部', value: 'all'),
          const _HomeSourceItem(label: '本地', value: 'local'),
          const _HomeSourceItem(label: '云端（全部）', value: 'webdav'),
        ];
        final webdavIds = sources.map((s) => s.id).toList()..sort();
        return AppSheetPanel(
          title: '切换音源',
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ...items.map((item) {
                final isSelected = _filter.value == item.value;
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 24),
                  title: Text(item.label),
                  trailing: isSelected ? const Icon(Icons.check_rounded) : null,
                  onTap: () {
                    _setFilter(item.value);
                    Navigator.pop(context);
                  },
                );
              }),
              if (webdavIds.isEmpty)
                const ListTile(
                  contentPadding: EdgeInsets.symmetric(horizontal: 24),
                  title: Text('暂无云端音源'),
                  enabled: false,
                )
              else
                ...webdavIds.map((id) {
                  final value = 'webdav:$id';
                  final isSelected = _filter.value == value;
                  final name = _webDavNameMap.value[id];
                  final label = (name ?? id).trim();
                  return ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 24),
                    title: Text('云端：$label'),
                    trailing: isSelected
                        ? const Icon(Icons.check_rounded)
                        : null,
                    onTap: () {
                      _setFilter(value);
                      Navigator.pop(context);
                    },
                  );
                }),
            ],
          ),
        );
      },
    );
  }

  Future<void> _pushLibraryPage(Widget page) async {
    await Navigator.of(context).push(buildAppPageRoute<void>((_) => page));
  }

  @override
  Widget build(BuildContext context) {
    return AppPageScaffold(
      key: _scaffoldKey,
      extendBodyBehindAppBar: true,
      appBar: AppTopBar(
        title: '首页',
        leading: IconButton(
          icon: const Icon(Icons.menu_rounded),
          onPressed: () => _scaffoldKey.currentState?.openDrawer(),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.swap_horiz_rounded),
            onPressed: _showSourceSheet,
          ),
          const SizedBox(width: 8),
        ],
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      drawer: SideMenu(
        onCloseDrawer: () => _scaffoldKey.currentState?.closeDrawer(),
      ),
      body: Watch.builder(
        builder: (context) => RefreshIndicator(
          onRefresh: () => _load(includeWebDavCounts: true),
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 160),
            children: [
              Text(
                '音乐库',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 12),
              _HomeStatsRow(
                loading: _loading.value,
                filterLabel: _filterTitle.value,
                songCount: _filterCount.value,
              ),
              const SizedBox(height: 14),
              LayoutBuilder(
                builder: (context, constraints) {
                  final width = constraints.maxWidth;
                  final itemWidth = (width - 16) / 2;
                  return Wrap(
                    spacing: 16,
                    runSpacing: 12,
                    children: [
                      SizedBox(
                        width: itemWidth,
                        child: _HomeEntryCard(
                          icon: Icons.music_note_rounded,
                          label: '歌曲',
                          onTap: () {
                            _pushLibraryPage(const SongsPage());
                          },
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _HomeEntryCard(
                          icon: Icons.people_rounded,
                          label: '艺术家',
                          onTap: () {
                            _pushLibraryPage(const ArtistsPage());
                          },
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _HomeEntryCard(
                          icon: Icons.album_rounded,
                          label: '专辑',
                          onTap: () {
                            _pushLibraryPage(const AlbumsPage());
                          },
                        ),
                      ),
                      SizedBox(
                        width: itemWidth,
                        child: _HomeEntryCard(
                          icon: Icons.queue_music_rounded,
                          label: '歌单',
                          onTap: () {
                            _pushLibraryPage(const PlaylistsPage());
                          },
                        ),
                      ),
                    ],
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeCountsCache {
  final int countAll;
  final int countLocal;
  final int countRemote;
  final List<WebDavSource> webDavSources;
  final Map<String, int> webDavCounts;

  const _HomeCountsCache({
    required this.countAll,
    required this.countLocal,
    required this.countRemote,
    required this.webDavSources,
    required this.webDavCounts,
  });

  _HomeCountsCache copyWith({
    int? countAll,
    int? countLocal,
    int? countRemote,
    List<WebDavSource>? webDavSources,
    Map<String, int>? webDavCounts,
  }) {
    return _HomeCountsCache(
      countAll: countAll ?? this.countAll,
      countLocal: countLocal ?? this.countLocal,
      countRemote: countRemote ?? this.countRemote,
      webDavSources: webDavSources ?? this.webDavSources,
      webDavCounts: webDavCounts ?? this.webDavCounts,
    );
  }
}

class _HomeStatsRow extends StatelessWidget {
  final bool loading;
  final String filterLabel;
  final int songCount;

  const _HomeStatsRow({
    required this.loading,
    required this.filterLabel,
    required this.songCount,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final bg = isDark
        ? const Color(0xFF1F2329)
        : const Color.fromARGB(242, 255, 255, 255);
    final shadowColor = isDark
        ? const Color.fromARGB(28, 0, 0, 0)
        : const Color.fromARGB(15, 0, 0, 0);

    return Container(
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(
            color: shadowColor,
            blurRadius: 14,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          Icon(Icons.library_music_rounded, color: theme.colorScheme.primary),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              filterLabel,
              style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
            ),
          ),
          if (loading)
            Text(
              '--',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: theme.colorScheme.onSurface.withAlpha(204),
              ),
            )
          else
            Text(
              '$songCount 首',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: theme.colorScheme.onSurface.withAlpha(204),
              ),
            ),
        ],
      ),
    );
  }
}

class _HomeSourceItem {
  final String label;
  final String value;

  const _HomeSourceItem({required this.label, required this.value});
}

class _HomeEntryCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _HomeEntryCard({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final cardColor = isDark
        ? const Color(0xFF1F2329)
        : const Color.fromARGB(242, 255, 255, 255);
    final shadowColor = isDark
        ? const Color.fromARGB(28, 0, 0, 0)
        : const Color.fromARGB(15, 0, 0, 0);
    final iconColor = isDark
        ? Colors.white70
        : const Color.fromARGB(255, 40, 40, 40);
    final textColor = isDark
        ? Colors.white
        : const Color.fromARGB(255, 45, 45, 45);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onTap,
        child: Container(
          height: 88,
          decoration: BoxDecoration(
            color: cardColor,
            borderRadius: BorderRadius.circular(18),
            boxShadow: [
              BoxShadow(
                color: shadowColor,
                blurRadius: 14,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              Icon(icon, size: 28, color: iconColor),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  label,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: textColor,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
