import '../../app/state/song_state.dart';

class SongsSelectionController {
  bool toggleMultiSelect(bool currentValue) {
    return !currentValue;
  }

  Set<String> clearSelection() {
    return <String>{};
  }

  Set<String> toggleSong(Set<String> current, String songId) {
    final next = Set<String>.from(current);
    if (next.contains(songId)) {
      next.remove(songId);
    } else {
      next.add(songId);
    }
    return next;
  }

  Set<String> toggleSelectAll(
    Set<String> current,
    List<SongEntity> visibleSongs,
  ) {
    if (visibleSongs.isEmpty) return current;
    if (current.length == visibleSongs.length) {
      return <String>{};
    }
    return visibleSongs.map((e) => e.id).toSet();
  }

  int selectedCount(Set<String> current) => current.length;
}
