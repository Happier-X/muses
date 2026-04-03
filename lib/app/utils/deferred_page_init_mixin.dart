import 'dart:async';

import 'package:flutter/widgets.dart';

mixin DeferredPageInitMixin<T extends StatefulWidget> on State<T> {
  Timer? _deferredInitTimer;

  Duration get deferredInitDelay => const Duration(milliseconds: 120);

  Future<void> runDeferredInit();

  @protected
  void scheduleDeferredInit() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _deferredInitTimer = Timer(deferredInitDelay, () {
        if (!mounted) return;
        unawaited(runDeferredInit());
      });
    });
  }

  @override
  void dispose() {
    _deferredInitTimer?.cancel();
    super.dispose();
  }
}
