import 'dart:collection';

class PageCacheStore {
  PageCacheStore._();

  static final PageCacheStore instance = PageCacheStore._();

  static const int _maxEntries = 32;

  final LinkedHashMap<String, Object?> _entries =
      LinkedHashMap<String, Object?>();

  String _fullKey(String scope, String key) => '$scope::$key';

  T? get<T>(String scope, String key) {
    final fullKey = _fullKey(scope, key);
    if (!_entries.containsKey(fullKey)) return null;
    final value = _entries.remove(fullKey);
    _entries[fullKey] = value;
    return value as T?;
  }

  void set<T>(String scope, String key, T value) {
    final fullKey = _fullKey(scope, key);
    _entries.remove(fullKey);
    _entries[fullKey] = value;
    while (_entries.length > _maxEntries) {
      _entries.remove(_entries.keys.first);
    }
  }

  void remove(String scope, String key) {
    _entries.remove(_fullKey(scope, key));
  }

  void clearScope(String scope) {
    final prefix = '$scope::';
    final keys = _entries.keys.where((key) => key.startsWith(prefix)).toList();
    for (final key in keys) {
      _entries.remove(key);
    }
  }
}
