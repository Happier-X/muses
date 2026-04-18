import 'package:flutter/material.dart';

import '../state/settings_background_state.dart';

class AppScrollBehavior extends MaterialScrollBehavior {
  const AppScrollBehavior();

  @override
  Widget buildOverscrollIndicator(
    BuildContext context,
    Widget child,
    ScrollableDetails details,
  ) {
    return child;
  }
}

class CoverPageTransitionsBuilder extends PageTransitionsBuilder {
  const CoverPageTransitionsBuilder();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    final slideAnimation = CurvedAnimation(
      parent: animation,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeInCubic,
    );
    final offsetTween = Tween(begin: const Offset(0.08, 0), end: Offset.zero);
    final content = SlideTransition(
      position: slideAnimation.drive(offsetTween),
      child: child,
    );
    if (secondaryAnimation.status != AnimationStatus.dismissed) {
      final fadeOut = CurvedAnimation(
        parent: secondaryAnimation,
        curve: const Interval(0, 0.2),
      );
      return FadeTransition(opacity: ReverseAnimation(fadeOut), child: content);
    }
    return content;
  }
}

extension AppThemeSurfaceX on ThemeData {
  bool get hasAmbientBackground {
    final backgroundPath = AppBackgroundSettings.backgroundImagePath.value;
    return AppBackgroundSettings.pageGlowEnabled.value ||
        (backgroundPath != null && backgroundPath.trim().isNotEmpty);
  }

  Color get appPanelColor {
    final base = brightness == Brightness.dark
        ? colorScheme.surfaceContainerHigh
        : colorScheme.surfaceContainerLow;
    if (!hasAmbientBackground) return base;
    final tint = brightness == Brightness.dark
        ? colorScheme.primary.withValues(alpha: 0.18)
        : colorScheme.primary.withValues(alpha: 0.08);
    return Color.alphaBlend(
      tint,
      base,
    ).withValues(alpha: brightness == Brightness.dark ? 0.84 : 0.76);
  }

  Color get appPanelShadowColor => brightness == Brightness.dark
      ? Colors.black.withValues(alpha: 0.22)
      : Colors.black.withValues(alpha: 0.08);
}
