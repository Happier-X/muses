import 'dart:async';
import 'dart:collection';
import 'dart:io';
import 'dart:typed_data';

import '../../app/services/artwork_cache_helper.dart';
import '../../app/services/artwork_service.dart';
import '../../app/services/db/dao/song_dao.dart';
import '../../app/state/song_state.dart';

class SongsArtworkCoordinator {
  static const int _artworkMaxConcurrent = 6;
  static const int _artworkCacheMax = 300;

  static final LinkedHashMap<String, Uint8List> _artworkCache =
      LinkedHashMap<String, Uint8List>();
  static final Map<String, Future<Uint8List?>> _artworkLoading =
      <String, Future<Uint8List?>>{};
  static final List<_ArtworkTask> _artworkQueue = <_ArtworkTask>[];
  static int _artworkActive = 0;

  final ArtworkService _artworkService;
  final SongDao _songDao;

  String _lastPrefetchKey = '';
  int _lastPrefetchCount = -1;

  SongsArtworkCoordinator({ArtworkService? artworkService, SongDao? songDao})
    : _artworkService = artworkService ?? ArtworkService.instance,
      _songDao = songDao ?? SongDao();

  void clearSong(String songId, {String? uri}) {
    _artworkCache.remove(songId);
    _artworkLoading.remove(songId);
    _artworkQueue.removeWhere((t) => t.song.id == songId);
    _artworkService.clearByUri(uri);
  }

  Future<Uint8List?> loadArtwork(
    SongEntity song, {
    required bool cacheArtworkEnabled,
    required void Function(SongEntity updatedSong) onSongUpdated,
  }) {
    final id = song.id;
    final cached = _artworkCache[id];
    if (cached != null) {
      _rememberArtwork(id, cached);
      return Future.value(cached);
    }
    final inflight = _artworkLoading[id];
    if (inflight != null) return inflight;
    final completer = Completer<Uint8List?>();
    _artworkLoading[id] = completer.future;
    _artworkQueue.add(
      _ArtworkTask(
        song,
        completer,
        cacheArtworkEnabled: cacheArtworkEnabled,
        onSongUpdated: onSongUpdated,
      ),
    );
    _drainArtworkQueue();
    return completer.future.whenComplete(() => _artworkLoading.remove(id));
  }

  void scheduleRangePrefetch(
    int start,
    int end,
    List<SongEntity> songs, {
    required bool enabled,
    required String sourceFilter,
    required String sortKey,
    required bool ascending,
    required bool cacheArtworkEnabled,
    required void Function(SongEntity updatedSong) onSongUpdated,
  }) {
    if (songs.isEmpty || !enabled) return;
    if (end - start > 9) {
      end = start + 9;
    }
    final key = '$sourceFilter|$sortKey|$ascending|$start|$end|${songs.length}';
    if (key == _lastPrefetchKey && songs.length == _lastPrefetchCount) return;
    _lastPrefetchKey = key;
    _lastPrefetchCount = songs.length;
    Future.microtask(
      () => _prefetchArtworkRange(
        songs,
        start,
        end,
        cacheArtworkEnabled: cacheArtworkEnabled,
        onSongUpdated: onSongUpdated,
      ),
    );
  }

  void _prefetchArtworkRange(
    List<SongEntity> songs,
    int start,
    int end, {
    required bool cacheArtworkEnabled,
    required void Function(SongEntity updatedSong) onSongUpdated,
  }) {
    if (songs.isEmpty) return;
    final safeStart = start < 0 ? 0 : start;
    final safeEnd = end >= songs.length ? songs.length - 1 : end;
    if (safeEnd < safeStart) return;
    for (var i = safeStart; i <= safeEnd; i++) {
      final song = songs[i];
      final hasLocalCover = (song.localCoverPath ?? '').trim().isNotEmpty;
      if (hasLocalCover || !song.isLocal) continue;
      unawaited(
        loadArtwork(
          song,
          cacheArtworkEnabled: cacheArtworkEnabled,
          onSongUpdated: onSongUpdated,
        ),
      );
    }
  }

  void _drainArtworkQueue() {
    while (_artworkActive < _artworkMaxConcurrent && _artworkQueue.isNotEmpty) {
      final task = _artworkQueue.removeLast();
      _artworkActive += 1;
      _loadArtworkInternal(
            task.song,
            cacheArtworkEnabled: task.cacheArtworkEnabled,
            onSongUpdated: task.onSongUpdated,
          )
          .then((bytes) {
            if (!task.completer.isCompleted) {
              task.completer.complete(bytes);
            }
          })
          .catchError((_) {
            if (!task.completer.isCompleted) {
              task.completer.complete(null);
            }
          })
          .whenComplete(() {
            _artworkActive -= 1;
            _drainArtworkQueue();
          });
    }
  }

  Future<Uint8List?> _loadArtworkInternal(
    SongEntity song, {
    required bool cacheArtworkEnabled,
    required void Function(SongEntity updatedSong) onSongUpdated,
  }) async {
    final cachedPath = song.localCoverPath;
    if (cachedPath != null && cachedPath.isNotEmpty) {
      final file = File(cachedPath);
      if (await file.exists()) {
        final bytes = await file.readAsBytes();
        if (bytes.isNotEmpty) {
          _rememberArtwork(song.id, bytes);
          return bytes;
        }
      }
    }
    if (!song.isLocal) return null;
    try {
      final bytes = await _artworkService.loadArtworkBytes(
        uri: song.uri,
        localCoverPath: song.localCoverPath,
        isLocal: song.isLocal,
        preferOriginal: false,
      );
      if (bytes == null || bytes.isEmpty) return null;

      if (cacheArtworkEnabled && (song.localCoverPath ?? '').trim().isEmpty) {
        final cached = await ArtworkCacheHelper.cacheCompressedArtwork(
          bytes: bytes,
          key: '${song.id}_${song.fileModifiedMs ?? 0}',
        );
        if (cached != null && cached.isNotEmpty) {
          final updated = song.copyWith(localCoverPath: cached);
          unawaited(_songDao.upsertSongs([updated]));
          onSongUpdated(updated);
        }
      }
      _rememberArtwork(song.id, bytes);
      return bytes;
    } catch (_) {
      return null;
    }
  }

  void _rememberArtwork(String id, Uint8List bytes) {
    _artworkCache.remove(id);
    _artworkCache[id] = bytes;
    while (_artworkCache.length > _artworkCacheMax) {
      final oldestKey = _artworkCache.keys.first;
      _artworkCache.remove(oldestKey);
    }
  }
}

class _ArtworkTask {
  final SongEntity song;
  final Completer<Uint8List?> completer;
  final bool cacheArtworkEnabled;
  final void Function(SongEntity updatedSong) onSongUpdated;

  _ArtworkTask(
    this.song,
    this.completer, {
    required this.cacheArtworkEnabled,
    required this.onSongUpdated,
  });
}
