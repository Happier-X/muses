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
    final isDark = brightness == Brightness.dark;
    final base = isDark
        ? Color.alphaBlend(
            colorScheme.primary.withValues(alpha: 0.08),
            colorScheme.surfaceContainerHigh,
          )
        : Color.alphaBlend(
            colorScheme.primary.withValues(alpha: 0.12),
            Colors.white,
          );
    if (!hasAmbientBackground) return base;
    
    final overlayColor = isDark
        ? colorScheme.surfaceContainerHighest.withValues(alpha: 0.65)
        : Colors.white.withValues(alpha: 0.70);
        
    return Color.alphaBlend(
      colorScheme.primary.withValues(alpha: 0.06), 
      overlayColor,
    );
  }

  Color get appPanelShadowColor {
    final isDark = brightness == Brightness.dark;
    return isDark
        ? Colors.black.withValues(alpha: 0.35)
        : colorScheme.primary.withValues(alpha: 0.16);
  }

  Color get appPanelBorderColor {
    final isDark = brightness == Brightness.dark;
    return isDark
        ? colorScheme.outline.withValues(alpha: 0.36)
        : colorScheme.primary.withValues(alpha: 0.12);
  }

  Color get appPanelElevatedColor {
    final base = appPanelColor;
    final overlay = brightness == Brightness.dark
        ? Colors.white.withValues(alpha: 0.05)
        : Colors.white.withValues(alpha: 0.25);
    return Color.alphaBlend(overlay, base);
  }
}
