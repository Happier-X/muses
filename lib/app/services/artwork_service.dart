import 'dart:async';
import 'dart:collection';
import 'dart:io';
import 'dart:typed_data';

import 'package:audio_metadata_reader/audio_metadata_reader.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:photo_manager/photo_manager.dart';

class ArtworkService {
  ArtworkService._();

  static final ArtworkService instance = ArtworkService._();

  static const int _maxCache = 100;
  static const int _maxConcurrent = 6;

  final LinkedHashMap<String, Uint8List?> _bytesCache =
      LinkedHashMap<String, Uint8List?>();
  final Map<String, Future<Uint8List?>> _loadingFutures =
      <String, Future<Uint8List?>>{};
  final List<_ArtworkRequest> _queue = <_ArtworkRequest>[];
  int _active = 0;

  Future<Uint8List?> loadArtworkBytes({
    required String? uri,
    required String? localCoverPath,
    required String? localAssetId,
    required bool isLocal,
    required bool preferOriginal,
  }) async {
    final trimmedCover = (localCoverPath ?? '').trim();
    if (!preferOriginal && trimmedCover.isNotEmpty) {
      final file = File(trimmedCover);
      if (await file.exists()) {
        return null;
      }
    }

    if (!isLocal) return null;
    final trimmedUri = (uri ?? '').trim();
    final trimmedAssetId = (localAssetId ?? '').trim();
    if (trimmedUri.isEmpty) return null;

    final cacheBase = trimmedAssetId.isNotEmpty
        ? 'asset:$trimmedAssetId'
        : trimmedUri;
    final cacheKey = preferOriginal ? '$cacheBase|original' : cacheBase;
    if (_bytesCache.containsKey(cacheKey)) {
      final cached = _bytesCache.remove(cacheKey);
      _bytesCache[cacheKey] = cached;
      return cached;
    }

    final inflight = _loadingFutures[cacheKey];
    if (inflight != null) return inflight;

    final completer = Completer<Uint8List?>();
    _loadingFutures[cacheKey] = completer.future;
    _queue.add(
      _ArtworkRequest(
        trimmedUri,
        trimmedAssetId,
        cacheKey,
        preferOriginal,
        completer,
      ),
    );
    _drainQueue();

    return completer.future.whenComplete(() {
      _loadingFutures.remove(cacheKey);
    });
  }

  void clearByUri(String? uri) {
    final trimmed = (uri ?? '').trim();
    if (trimmed.isEmpty) return;
    _bytesCache.remove(trimmed);
    _bytesCache.remove('$trimmed|original');
  }

  void _remember(String cacheKey, Uint8List? bytes) {
    _bytesCache.remove(cacheKey);
    _bytesCache[cacheKey] = bytes;
    while (_bytesCache.length > _maxCache) {
      _bytesCache.remove(_bytesCache.keys.first);
    }
  }

  void _drainQueue() {
    while (_active < _maxConcurrent && _queue.isNotEmpty) {
      final task = _queue.removeLast();
      _active += 1;
      _readArtworkBytes(
            task.uri,
            assetId: task.assetId,
            preferOriginal: task.preferOriginal,
          )
          .then((bytes) {
            _remember(task.cacheKey, bytes);
            if (!task.completer.isCompleted) {
              task.completer.complete(bytes);
            }
          })
          .catchError((_) {
            _remember(task.cacheKey, null);
            if (!task.completer.isCompleted) {
              task.completer.complete(null);
            }
          })
          .whenComplete(() {
            _active -= 1;
            _drainQueue();
          });
    }
  }

  static Future<Uint8List?> _readArtworkBytes(
    String uri, {
    required String assetId,
    required bool preferOriginal,
  }) async {
    try {
      if (assetId.isNotEmpty && !preferOriginal) {
        final entity = await AssetEntity.fromId(assetId);
        if (entity != null) {
          final thumb = await entity.thumbnailDataWithSize(
            const ThumbnailSize(320, 320),
          );
          if (thumb != null && thumb.isNotEmpty) {
            return thumb;
          }
        }
      }

      final file = File(uri);
      if (!await file.exists()) return null;
      final metadata = readMetadata(file, getImage: true);
      if (metadata.pictures.isEmpty) return null;
      final original = metadata.pictures.first.bytes;
      if (original.isEmpty) return null;
      if (preferOriginal) {
        return original;
      }
      try {
        final compressed = await FlutterImageCompress.compressWithList(
          original,
          minWidth: 300,
          minHeight: 300,
          quality: 85,
        );
        if (compressed.isNotEmpty) {
          return compressed;
        }
      } catch (_) {}
      return original;
    } catch (_) {
      return null;
    }
  }
}

class _ArtworkRequest {
  final String uri;
  final String assetId;
  final String cacheKey;
  final bool preferOriginal;
  final Completer<Uint8List?> completer;

  _ArtworkRequest(
    this.uri,
    this.assetId,
    this.cacheKey,
    this.preferOriginal,
    this.completer,
  );
}
